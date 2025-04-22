package com.travelshop.utils;

import com.travelshop.enums.BusinessType;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.context.ApplicationContext;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

@Slf4j
@Component
public class BloomFilter {

    private final StringRedisTemplate stringRedisTemplate;
    private final RedissonClient redissonClient;
    private final ApplicationContext applicationContext;

    private static final ScheduledExecutorService CACHE_REBUILD_EXECUTOR = Executors.newScheduledThreadPool(10);

    // 布隆过滤器集合
    private final Map<String, RBloomFilter<String>> bloomFilters = new ConcurrentHashMap<>();
    // 记录已删除的key
    private final Set<String> deletedKeys = new HashSet<>();
    // 布隆过滤器统计信息的key前缀
    private static final String BLOOM_STATS_PREFIX = "bloom:stats:";
    // 布隆过滤器名称前缀
    private static final String BLOOM_FILTER_PREFIX = "bloom:filter:";
    // 重建状态
    private final Map<String, Boolean> rebuildingStatus = new ConcurrentHashMap<>();



    public BloomFilter(StringRedisTemplate stringRedisTemplate, RedissonClient redissonClient,
                       ApplicationContext applicationContext) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.redissonClient = redissonClient;
        this.applicationContext = applicationContext;
    }

    @PostConstruct
    public void init() {
        rebuildAllBloomFilters();
        log.info("初始化布隆过滤器完成");
    }

    public void addBloomFilter(String businessCode, String key) {
        RBloomFilter<String> bloomFilter = getOrCreateBloomFilter(businessCode);
        bloomFilter.add(key);
        // 从删除列表中移除
        deletedKeys.remove(key);
    }

    public <ID> void batchAddToBloomFilter(BusinessType businessType, Iterable<ID> ids) {
        String businessCode = businessType.getCode();
        RBloomFilter<String> bloomFilter = getOrCreateBloomFilter(businessCode);
        int count = 0;

        for (ID id : ids) {
            String key = businessType.buildCacheKey(id);
            bloomFilter.add(key);
            count++;
        }

        if (count > 0) {
            log.info("批量添加到布隆过滤器[{}]完成，共添加{}个元素", businessCode, count);
        }
    }

    public void deleteFromBloomFilter(String key) {
        if (deletedKeys.add(key)) {
            BusinessType businessType = BusinessType.getByKey(key);
            if (businessType != null) {
                String businessCode = businessType.getCode();

                // 增加业务特定的删除计数
                incrementBloomDeleteStats(businessCode);
                long businessDeletedCount = getBloomDeleteStats(businessCode);

                RBloomFilter<String> filter = bloomFilters.get(businessCode);
                if (filter != null && filter.count() > 0) {
                    // 计算特定业务的删除比例
                    if ((double) businessDeletedCount / filter.count() > getBusinessRebuildThreshold(businessCode)) {
                        log.info("业务[{}]删除元素数量超过阈值，触发重建", businessCode);
                        CACHE_REBUILD_EXECUTOR.execute(() -> rebuildSingleBloomFilter(businessType));
                        // 重置该业务的删除计数
                        stringRedisTemplate.opsForValue().set(BLOOM_STATS_PREFIX + businessCode + ":deleted", "0");
                    }
                }
            }
        }
    }

    // 获取业务特定的重建阈值
    private double getBusinessRebuildThreshold(String businessCode) {
        // 可从配置中心或Redis获取，这里使用默认值
        if (businessCode == null) {
            return 0.1;  // 默认阈值
        } else if (businessCode.equals("shop")) {
            return 0.05;  // 店铺数据变动频繁，设置较低阈值
        } else if (businessCode.equals("blog")) {
            return 0.15;  // 博客数据相对稳定，可用较高阈值
        } else {
            return 0.1;   // 默认阈值
        }
    }

    public boolean mightContain(String businessCode, String key) {
        if (deletedKeys.contains(key)) {
            return false;
        }
        RBloomFilter<String> bloomFilter = bloomFilters.get(businessCode);
        if (bloomFilter == null) {
            return false;
        }
        boolean result = bloomFilter.contains(key);
        incrementBloomStats("queries");
        if (result) {
            incrementBloomStats("positives");
        }
        return result;
    }

    public boolean mightContain(BusinessType businessType, Object id) {
        String key = businessType.buildCacheKey(id);
        return mightContain(businessType.getCode(), key);
    }

    public boolean rebuildAllBloomFilters() {
        log.info("开始重建所有布隆过滤器");
        boolean allSuccess = true;

        for (BusinessType businessType : BusinessType.values()) {
            try {
                boolean success = rebuildSingleBloomFilter(businessType);
                if (!success) {
                    allSuccess = false;
                }
            } catch (Exception e) {
                log.error("重建布隆过滤器[{}]失败", businessType.getCode(), e);
                allSuccess = false;
            }
        }

        if (!deletedKeys.isEmpty()) {
            log.info("清理已删除的元素");
            deletedKeys.clear();
            setBloomStats("deleted", 0L);
        }
        log.info("所有布隆过滤器重建完成，结果：{}", allSuccess ? "成功" : "部分失败");
        return allSuccess;
    }

    /**
     * 重建单个业务类型的布隆过滤器
     * 修改为公共方法，供XXL-Job调用
     * @param businessType 业务类型
     * @return 重建是否成功
     */
    public boolean rebuildSingleBloomFilter(BusinessType businessType) {
        String businessCode = businessType.getCode();

        // 检查是否已经在重建中
        if (Boolean.TRUE.equals(rebuildingStatus.get(businessCode))) {
            log.info("布隆过滤器[{}]正在重建中，跳过本次重建", businessCode);
            return false;
        }

        try {
            // 标记为重建中状态
            rebuildingStatus.put(businessCode, true);
            log.info("开始重建布隆过滤器[{}]", businessCode);

            // 获取并重置布隆过滤器
            RBloomFilter<String> bloomFilter = getOrCreateBloomFilter(businessCode);
            bloomFilter.delete();

            // 重新初始化过滤器（可根据数据量动态设置容量和错误率）
            int expectedInsertions = estimateDataSize(businessType);
            double falseProbability = getBusinessFalseRate(businessCode);
            bloomFilter.tryInit(Math.max(expectedInsertions, 1000), falseProbability);

            // 记录开始时间，用于性能统计
            long startTime = System.currentTimeMillis();

            // 加载业务数据
            Set<String> keys = loadBusinessData(businessType);

            // 将数据添加到布隆过滤器
            int count = 0;
            for (String key : keys) {
                bloomFilter.add(key);
                count++;
            }

            // 清理该业务类型的已删除键记录
            clearDeletedKeysForBusiness(businessType);

            // 重置该业务的删除计数
            stringRedisTemplate.opsForValue().set(BLOOM_STATS_PREFIX + businessCode + ":deleted", "0");

            // 记录重建耗时
            long costTime = System.currentTimeMillis() - startTime;
            log.info("布隆过滤器[{}]重建完成，共添加{}个元素，耗时{}ms", businessCode, count, costTime);

            return true;
        } catch (Exception e) {
            log.error("重建布隆过滤器[{}]时发生错误", businessCode, e);
            return false;
        } finally {
            // 无论成功失败，都将重建状态标记为完成
            rebuildingStatus.put(businessCode, false);
        }
    }

    // 估计业务数据大小，用于动态设置布隆过滤器容量
    private int estimateDataSize(BusinessType businessType) {
        try {
            Set<String> sampleData = loadBusinessData(businessType);
            // 为可能的数据增长预留50%空间
            return (int)(sampleData.size() * 1.5);
        } catch (Exception e) {
            log.warn("无法估计{}业务数据量，使用默认值", businessType.getCode());
            return 10000;  // 默认值
        }
    }

    // 获取业务特定的错误率配置
    private double getBusinessFalseRate(String businessCode) {
        if (businessCode == null) {
            return 0.01;  // 默认错误率
        } else if (businessCode.equals("shop")) {
            return 0.001;  // 商铺查询频繁，使用较低错误率
        } else if (businessCode.equals("blog")) {
            return 0.01;   // 博客查询较少，可用较高错误率
        } else {
            return 0.005;  // 默认错误率
        }
    }

    // 清理指定业务类型的已删除键
    private void clearDeletedKeysForBusiness(BusinessType businessType) {
        String prefix = businessType.getKeyPrefix();
        deletedKeys.removeIf(key -> key.startsWith(prefix));
    }

    private Set<String> loadBusinessData(BusinessType businessType) {
        Set<String> keys = new HashSet<>();
        try {
            if (applicationContext != null) {
                Object mapper = businessType.getMapper(applicationContext);

                String keyPrefix = businessType.getKeyPrefix();

                // 使用反射调用 selectAllIds 方法
                Method selectMethod = mapper.getClass().getMethod("selectAllIds");
                @SuppressWarnings("unchecked")
                List<Object> ids = (List<Object>) selectMethod.invoke(mapper);

                // 将ID转换为缓存键
                for (Object id : ids) {
                    keys.add(keyPrefix + id);
                }

                log.info("加载{}业务数据完成，共{}条记录", businessType.getCode(), ids.size());
            }
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            log.error("加载业务数据失败: {}", businessType.getCode(), e);
        } catch (Exception e) {
            log.error("加载业务数据发生未知错误: {}", businessType.getCode(), e);
        }
        return keys;
    }

    private RBloomFilter<String> getOrCreateBloomFilter(String businessCode) {
        return bloomFilters.computeIfAbsent(businessCode, name -> {
            RBloomFilter<String> filter = redissonClient.getBloomFilter(BLOOM_FILTER_PREFIX + name);
            // 使用默认配置初始化
            filter.tryInit(10000, 0.01);
            return filter;
        });
    }
    private void incrementBloomStats(String statName) {
        Long value = getBloomStats(statName);
        setBloomStats(statName, value + 1);
    }

    // 替代全局删除计数器
    private void incrementBloomDeleteStats(String businessCode) {
        String key = BLOOM_STATS_PREFIX + businessCode + ":deleted";
        stringRedisTemplate.opsForValue().increment(key);
    }

    // 获取特定业务类型的删除计数
    private Long getBloomDeleteStats(String businessCode) {
        String key = BLOOM_STATS_PREFIX + businessCode + ":deleted";
        String value = stringRedisTemplate.opsForValue().get(key);
        return value != null ? Long.parseLong(value) : 0L;
    }

    /**
     * 获取布隆过滤器统计数据
     */
    private Long getBloomStats(String statName) {
        String value = stringRedisTemplate.opsForValue().get(BLOOM_STATS_PREFIX + statName);
        return value != null ? Long.parseLong(value) : 0L;
    }

    /**
     * 设置布隆过滤器统计数据
     */
    private void setBloomStats(String statName, Long value) {
        stringRedisTemplate.opsForValue().set(BLOOM_STATS_PREFIX + statName, value.toString());
    }

    /**
     * 获取布隆过滤器性能统计
     */
    public Map<String, Object> getBloomFilterStats() {
        Map<String, Object> stats = new HashMap<>();

        Long queries = getBloomStats("queries");
        Long positives = getBloomStats("positives");
        Long falsePositives = getBloomStats("falsePositives");
        Long deleted = getBloomStats("deleted");

        double falsePositiveRate = queries > 0 ? (double) falsePositives / queries : 0;

        stats.put("totalQueries", queries);
        stats.put("positiveResults", positives);
        stats.put("falsePositives", falsePositives);
        stats.put("falsePositiveRate", falsePositiveRate);
        stats.put("deletedKeys", deleted);
        stats.put("deletedKeysInMemory", deletedKeys.size());

        return stats;
    }

    /**
     * 布隆过滤器健康检查
     */
    public Map<String, Object> bloomFilterHealthCheck() {
        Map<String, Object> health = new HashMap<>();

        for (Map.Entry<String, RBloomFilter<String>> entry : bloomFilters.entrySet()) {
            String businessCode = entry.getKey();
            RBloomFilter<String> filter = entry.getValue();

            Map<String, Object> filterInfo = new HashMap<>();
            filterInfo.put("size", filter.count());
            filterInfo.put("expectedInsertions", filter.getExpectedInsertions());
            filterInfo.put("falseProbability", filter.getFalseProbability());

            health.put(businessCode, filterInfo);
        }
        health.put("stats", getBloomFilterStats());
        return health;
    }

}
