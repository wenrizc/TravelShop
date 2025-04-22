package com.travelshop.job;

import com.travelshop.utils.UnifiedCache;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * 缓存维护任务
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CacheMaintenanceJob {

    private final StringRedisTemplate stringRedisTemplate;
    private final UnifiedCache unifiedCache;

    /**
     * 缓存清理任务 - 清理过期缓存逻辑键
     */
    @XxlJob("cacheCleanupJob")
    public void cacheCleanupJob() {
        log.info("开始执行缓存清理任务");

        // 获取参数
        String pattern = XxlJobHelper.getJobParam();
        if (pattern == null || pattern.trim().isEmpty()) {
            pattern = "cache:logic:*"; // 默认清理所有逻辑键
        }

        try {
            long startTime = System.currentTimeMillis();

            // 执行清理逻辑
            Set<String> keys = unifiedCache.cleanupExpiredLogicKeys(pattern);

            long costTime = System.currentTimeMillis() - startTime;
            log.info("缓存清理任务完成，共清理{}个过期逻辑键，耗时{}ms", keys.size(), costTime);
            XxlJobHelper.log("缓存清理任务完成，共清理{}个过期逻辑键，耗时{}ms", keys.size(), costTime);
        } catch (Exception e) {
            log.error("缓存清理任务执行异常", e);
            XxlJobHelper.log("缓存清理任务执行异常: " + e.getMessage());
            XxlJobHelper.handleFail();
        }
    }

    /**
     * 热点数据预热任务
     */
    @XxlJob("cachePrewarmJob")
    public void cachePrewarmJob() {
        log.info("开始执行热点数据预热任务");

        // 获取分片参数
        int shardIndex = XxlJobHelper.getShardIndex();
        int shardTotal = XxlJobHelper.getShardTotal();

        // 获取任务参数，例如: shop,voucher,blog
        String businessType = XxlJobHelper.getJobParam();

        try {
            long startTime = System.currentTimeMillis();

            // 根据业务类型执行不同的预热逻辑
            int count = unifiedCache.prewarmHotData(businessType, shardIndex, shardTotal);

            long costTime = System.currentTimeMillis() - startTime;
            log.info("热点数据预热任务完成，共预热{}个热点数据，耗时{}ms", count, costTime);
            XxlJobHelper.log("热点数据预热任务完成，共预热{}个热点数据，耗时{}ms", count, costTime);
        } catch (Exception e) {
            log.error("热点数据预热任务执行异常", e);
            XxlJobHelper.log("热点数据预热任务执行异常: " + e.getMessage());
            XxlJobHelper.handleFail();
        }
    }
}