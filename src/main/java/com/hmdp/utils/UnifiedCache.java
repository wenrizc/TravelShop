package com.hmdp.utils;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hmdp.enums.BusinessType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cglib.core.internal.Function;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class UnifiedCache {

    private static final ExecutorService CACHE_REBUILD_EXECUTOR = Executors.newFixedThreadPool(16);

    private final StringRedisTemplate stringRedisTemplate;
    private final BloomFilter bloomFilter;
    private final DistributedLock distributedLock;
    private final CacheMessage cacheMessageService;

    private static final String LOCK_PREFIX = "cache:lock:";

    // 本地缓存相关配置
    private static final long LOCAL_CACHE_MAX_SIZE = 10000; // 最大缓存条目数
    private static final long LOCAL_CACHE_EXPIRE_SECONDS = 60; // 本地缓存过期时间(秒)
    private Cache<String, Object> localCache;

    private static final long CACHE_NULL_TTL = 60;

    public UnifiedCache(StringRedisTemplate stringRedisTemplate, BloomFilter bloomFilter, DistributedLock distributedLock, CacheMessage cacheMessageService) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.bloomFilter = bloomFilter;
        this.distributedLock = distributedLock;
        this.cacheMessageService = cacheMessageService;
    }

    @PostConstruct
    public void init() {
        // 初始化本地缓存
        localCache = Caffeine.newBuilder()
                .maximumSize(LOCAL_CACHE_MAX_SIZE)
                .expireAfterWrite(LOCAL_CACHE_EXPIRE_SECONDS, TimeUnit.SECONDS)
                .recordStats() // 开启统计
                .build();
        log.info("本地缓存初始化完成，最大容量：{}，过期时间：{}秒",
                LOCAL_CACHE_MAX_SIZE, LOCAL_CACHE_EXPIRE_SECONDS);
        cacheMessageService.subscribeToChanges(this::handleCacheMessage);
    }

    private void handleCacheMessage(Map<String, String> message) {
        String operation = message.get("operation");
        String key = message.get("key");
        if (key != null) {
            if ("delete".equals(operation)) {
                deleteFromLocalCache(key);
                bloomFilter.deleteFromBloomFilter(key);
                log.debug("收到删除缓存消息，key: {}", key);
            } else if ("update".equals(operation)) {
                deleteFromLocalCache(key);
                log.debug("收到更新缓存消息，key: {}", key);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private <R> R getFromLocalCache(String key, Class<R> type) {
        Object value = localCache.getIfPresent(key);
        if (value != null) {
            if (value instanceof String && ((String) value).isEmpty()) {
                return null;
            }
            return (R) value;
        }
        return null;
    }

    private <R> void putToLocalCache(String key, R value) {
        if (value == null) {
            localCache.put(key, "");
        } else {
            localCache.put(key, value);
        }
    }

    private void deleteFromLocalCache(String key) {
        localCache.invalidate(key);
        log.debug("删除本地缓存，key: {}", key);
    }

    public <T> void setWithRandomExpire(String key, T value, long time, TimeUnit unit) {
        long seconds = unit.toSeconds(time);
        // 添加随机偏移量，防止缓存雪崩
        long randomOffset = (long) (Math.random() * 0.2 * seconds);
        stringRedisTemplate.opsForValue().set(
                key,
                JSONUtil.toJsonStr(value),
                seconds + randomOffset,
                TimeUnit.SECONDS
        );
        log.debug("设置缓存，key: {}，过期时间: {}秒", key, seconds + randomOffset);
        BusinessType businessType = BusinessType.getByKey(key);
        if (businessType != null) {
            bloomFilter.addBloomFilter(businessType.getCode(), key);
        }
        cacheMessageService.publishCacheChange("update", key);
    }

    public <T> void setWithLogicalExpire(String key, T value, long time, TimeUnit unit) {
        RedisData redisData = new RedisData();
        redisData.setData(value);
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(unit.toSeconds(time)));
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(redisData));
        log.debug("设置逻辑过期缓存，key: {}", key);
        BusinessType businessType = BusinessType.getByKey(key);
        if (businessType != null) {
            bloomFilter.addBloomFilter(businessType.getCode(), key);
        }
        cacheMessageService.publishCacheChange("update", key);
    }

    public void setCacheNull(String key) {
        stringRedisTemplate.opsForValue().set(key, "", CACHE_NULL_TTL, TimeUnit.SECONDS);
        log.debug("设置空值缓存，key: {}", key);
        cacheMessageService.publishCacheChange("update", key);
    }

    public void deleteCache(String key) {
        stringRedisTemplate.delete(key);
        log.debug("删除缓存，key: {}", key);
        cacheMessageService.publishCacheChange("delete", key);
    }

    public <R> R getFromCache(String key, Class<R> type) {
        String json = stringRedisTemplate.opsForValue().get(key);
        if (json == null) {
            return null;
        }
        // 处理空值缓存情况
        if (StrUtil.isEmpty(json)) {
            return null;
        }
        return JSONUtil.toBean(json, type);
    }

    public <R> R getFromLogicalCache(String key, Class<R> type) {
        String json = stringRedisTemplate.opsForValue().get(key);
        if (json == null) {
            return null;
        }

        // 反序列化RedisData
        RedisData redisData = JSONUtil.toBean(json, RedisData.class);
        // 使用JSONUtil的转换而非强制类型转换
        R data = JSONUtil.toBean((JSONObject) redisData.getData(), type);

        // 判断是否逻辑过期
        if (redisData.getExpireTime().isAfter(LocalDateTime.now())) {
            // 未过期
            return data;
        }

        // 已过期，尝试获取锁异步更新
        String lockKey = LOCK_PREFIX + key;
        boolean isLockSuccess = distributedLock.tryLock(lockKey, 10);
        // 返回旧数据
        if (isLockSuccess) {
            // 成功获取锁，启动异步更新
            CACHE_REBUILD_EXECUTOR.submit(() -> {
                try {
                    // 重建缓存的逻辑
                    this.rebuildCache(key, 20L, TimeUnit.MINUTES);

                } catch (Exception e) {
                    log.error("异步重建缓存失败, key={}", key, e);
                } finally {
                    // 释放锁
                    distributedLock.unlock(lockKey);
                }
            });
            log.info("缓存已逻辑过期，已启动异步更新，key={}", key);
        }

        return data;
    }

    // 重建缓存的方法
    private <R> void rebuildCache(String key, Long timeout, TimeUnit timeUnit) {
        // 从业务类型和ID解析出需要的信息
        BusinessType businessType = BusinessType.getByKey(key);
        if (businessType == null) {
            log.warn("无法识别的业务类型, key={}", key);
            return;
        }

        // 获取ID
        String idStr = key.substring(businessType.getKeyPrefix().length());
        Long id;
        try {
            id = Long.valueOf(idStr);
        } catch (NumberFormatException e) {
            log.error("无法解析ID, key={}", key, e);
            return;
        }

        try {
            // 查询数据库获取新数据
            Object mapper = businessType.getMapper(SpringContextHolder.getApplicationContext());
            if (mapper != null) {
                Object entity = ((BaseMapper<?>)mapper).selectById(id);
                if (entity != null) {
                    // 更新缓存
                    this.setWithLogicalExpire(key, entity, timeout, timeUnit);
                    log.info("异步重建缓存成功, key={}", key);
                } else {
                    // 数据库中数据不存在，删除缓存
                    this.deleteCache(key);
                    log.info("数据库中不存在数据，已删除缓存, key={}", key);
                }
            }
        } catch (Exception e) {
            log.error("重建缓存异常, key={}", key, e);
        }
    }


    public <R, ID> R queryWithBloomFilter(
            String business,           // 业务类型编码
            String keyPrefix,          // 缓存键前缀
            ID id,                     // 数据ID
            Class<R> type,             // 返回类型
            Function<ID, R> dbFallback, // 数据库查询函数
            boolean useLogicalExpire,   // 是否使用逻辑过期
            long timeout,               // 超时时间
            TimeUnit timeUnit           // 时间单位
    ) {
        String key = keyPrefix + id;

        // 1. 先查本地缓存
        R localResult = getFromLocalCache(key, type);
        if (localResult != null) {
            return localResult;
        }

        // 2. 布隆过滤器检查
        if (!bloomFilter.mightContain(business, key)) {
            log.debug("布隆过滤器判断键[{}]不存在，直接返回null", key);
            putToLocalCache(key, null); // 在本地缓存中记录不存在
            return null;
        }

        // 3. 查询Redis缓存
        String json = stringRedisTemplate.opsForValue().get(key);
        if (json != null) {
            if (StrUtil.isNotBlank(json)) {
                try {
                    // 检查JSON格式
                    if (!json.startsWith("{") && !json.startsWith("[")) {
                        log.error("Redis中键[{}]的数据格式不是有效JSON: {}", key, json);
                        // 删除无效数据
                        stringRedisTemplate.delete(key);
                        // 查询数据库并重建缓存
                        R result = dbFallback.apply(id);
                        if (result != null) {
                            if (useLogicalExpire) {
                                setWithLogicalExpire(key, result, timeout, timeUnit);
                            } else {
                                setWithRandomExpire(key, result, timeout, timeUnit);
                            }
                            bloomFilter.addBloomFilter(business, key);
                            putToLocalCache(key, result);
                        } else {
                            setCacheNull(key);
                            putToLocalCache(key, null);
                        }
                        return result;
                    }
                    
                    // JSON格式正确，解析对象
                    R result;
                    if (json.startsWith("[")) {
                        // 数组类型处理
                        if (List.class.isAssignableFrom(type)) {
                            // 如果期望的类型是List，直接解析为List
                            result = (R) JSONUtil.toList(new JSONArray(json), getListGenericType(type));
                        } else {
                            // 如果期望单个对象但数据是数组，可以尝试以下策略：
                            // 1. 取第一个元素
                            JSONArray array = new JSONArray(json);
                            if (array.size() > 0) {
                                result = JSONUtil.toBean(array.getJSONObject(0), type);
                                log.warn("键[{}]存储了数组但期望单个对象，已提取第一个元素", key);
                            } else {
                                log.warn("键[{}]存储了空数组但期望单个对象，返回null", key);
                                return null;
                            }
                        }
                    } else {
                        // 对象类型直接解析
                        result = JSONUtil.toBean(json, type);
                    }

                    bloomFilter.addBloomFilter(business, key);
                    putToLocalCache(key, result);
                    return result;
                } catch (Exception e) {
                    log.error("解析Redis缓存数据异常，key: {}, json: {}", key, json, e);
                    // 删除无效数据并走数据库查询
                    stringRedisTemplate.delete(key);
                }
            }
            putToLocalCache(key, null);
            return null;
        }
        // 4. 查询数据库
        R result = dbFallback.apply(id);

        // 5. 缓存结果
        if (result != null) {
            // 根据策略选择缓存方式
            if (useLogicalExpire) {
                setWithLogicalExpire(key, result, timeout, timeUnit);
            } else {
                setWithRandomExpire(key, result, timeout, timeUnit);
            }
            // 确保数据在布隆过滤器中
            bloomFilter.addBloomFilter(business, key);
            // 存入本地缓存
            putToLocalCache(key, result);
        } else {
            // 缓存空值，防止缓存穿透
            setCacheNull(key);
            // 本地也缓存空值
            putToLocalCache(key, null);
        }

        return result;
    }

    /**
     * 获取List类型的泛型参数类型
     */
    private Class<?> getListGenericType(Class<?> type) {
        try {
            // 默认使用Voucher类型（根据实际业务调整）
            return Class.forName("com.hmdp.entity.Voucher");
        } catch (ClassNotFoundException e) {
            log.error("无法确定List泛型类型", e);
            return Object.class;
        }
    }

}


