package com.travelshop.utils;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.travelshop.enums.BusinessType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class CacheMessageListener {

    private final UnifiedCache cacheService;
    private final BloomFilter bloomFilter;
    private final StringRedisTemplate stringRedisTemplate;

    // 缓存过期时间(秒)
    private static final long CACHE_TTL = 60 * 60;

    // 消息去重缓存键前缀和过期时间
    private static final String MSG_PROCESSED_KEY = "cache:processed_msg:";
    private static final long MSG_ID_TTL = 24 * 60 * 60; // 24小时

    @Autowired
    public CacheMessageListener(UnifiedCache cacheService, BloomFilter bloomFilter,
                                StringRedisTemplate stringRedisTemplate) {
        this.cacheService = cacheService;
        this.bloomFilter = bloomFilter;
        this.stringRedisTemplate = stringRedisTemplate;

    }

    @RabbitListener(queues = "db.change.queue")
    public void handleDatabaseChange(Map<String, Object> message) {
        String messageId = message.containsKey("messageId") ?
                (String) message.get("messageId") :
                generateMessageId(message);

        if (isMessageProcessed(messageId)) {
            log.info("消息[{}]已处理，跳过", messageId);
            return;
        }

        String operation = (String) message.get("operation");
        String table = (String) message.get("table");
        Map<String, Object> data = (Map<String, Object>) message.get("data");

        log.info("处理数据库变更消息: 操作={}, 表={}, 消息ID={}", operation, table, messageId);

        BusinessType businessType = getBusinessTypeByTable(table);
        if (businessType == null) {
            log.warn("未知的业务类型：table={}", table);
            return;
        }

        handleEntityChange(operation, data, businessType);
    }

    private BusinessType getBusinessTypeByTable(String table) {
        for (BusinessType type : BusinessType.values()) {
            if (type.getCode().equals(table)) {
                return type;
            }
        }
        return null;
    }

    private void handleEntityChange(String operation, Map<String, Object> data, BusinessType businessType) {
        if (data == null || !data.containsKey("id")) {
            log.warn("数据格式不正确，缺少ID字段");
            return;
        }

        long id = Long.valueOf(data.get("id").toString());
        String key = businessType.buildCacheKey(id);

        switch (operation) {
            case "INSERT":
            case "UPDATE":
                updateCacheWithRetry(id, key, businessType);
                break;
            case "DELETE":
                deleteCacheWithRetry(key, businessType.getCode());
                break;
            default:
                log.warn("未知的操作类型: {}", operation);
        }
    }

    /**
     * 更新缓存（带重试机制）
     */
    @Retryable(value = {DataAccessException.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000))
    private void updateCacheWithRetry(long id, String key, BusinessType businessType) {
        try {
            ApplicationContext context = SpringContextHolder.getApplicationContext();
            if (context == null) {
                log.error("获取Spring上下文失败");
                return;
            }

            BaseMapper<?> mapper = (BaseMapper<?>) businessType.getMapper(context);
            Object entity = mapper.selectById(id);

            if (entity == null) {
                log.warn("{}[{}]实体不存在", businessType.getDescription(), id);
                return;
            }

            // 根据业务类型选择不同的缓存策略
            if (businessType == BusinessType.SHOP) {
                // 商铺使用逻辑过期策略
                cacheService.setWithLogicalExpire(key, entity, CACHE_TTL, TimeUnit.SECONDS);
            } else {
                // 其他业务使用物理过期策略
                cacheService.setWithRandomExpire(key, entity, CACHE_TTL, TimeUnit.SECONDS);
            }

            // 更新布隆过滤器
            bloomFilter.addBloomFilter(businessType.getCode(), key);
            log.info("{}[{}]缓存已更新", businessType.getDescription(), id);

        } catch (Exception e) {
            log.error("更新{}缓存失败: {}", businessType.getDescription(), e.getMessage(), e);
            throw e; // 重新抛出异常以触发重试
        }
    }

    /**
     * 删除缓存（带重试机制）
     */
    @Retryable(value = {Exception.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000))
    private void deleteCacheWithRetry(String key, String businessCode) {
        try {
            cacheService.deleteCache(key);
            bloomFilter.deleteFromBloomFilter(key);
            log.info("缓存已删除: {}", key);
        } catch (Exception e) {
            log.error("删除缓存失败: {}", e.getMessage(), e);
            throw e; // 重新抛出异常以触发重试
        }
    }

    private String generateMessageId(Map<String, Object> message) {
        String operation = (String) message.get("operation");
        String table = (String) message.get("table");
        Map<String, Object> data = (Map<String, Object>) message.get("data");
        String id = data.containsKey("id") ? String.valueOf(data.get("id")) : "";
        // 使用UUID确保唯一性
        return table + ":" + operation + ":" + id + ":" + UUID.randomUUID().toString();
    }

    private boolean isMessageProcessed(String messageId) {
        String key = MSG_PROCESSED_KEY + messageId;
        Boolean exists = stringRedisTemplate.hasKey(key);
        if (Boolean.TRUE.equals(exists)) {
            return true;
        }
        // 将消息ID存入Redis并设置过期时间
        cacheService.setWithRandomExpire(key, "1", MSG_ID_TTL, TimeUnit.SECONDS);
        return false;
    }
}