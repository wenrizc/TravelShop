package com.travelshop.job;

import com.travelshop.enums.BusinessType;
import com.travelshop.utils.BloomFilter;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 布隆过滤器重建任务
 * 使用XXL-Job管理重建时间和监控
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BloomFilterRebuildJob {

    private final BloomFilter bloomFilter;

    /**
     * 全量重建所有布隆过滤器 - 凌晨3点执行
     */
    @XxlJob("rebuildAllBloomFiltersJob")
    public void rebuildAllBloomFiltersJob() {
        log.info("开始执行全量布隆过滤器重建任务");

        // 获取任务参数(可选)
        String param = XxlJobHelper.getJobParam();
        long startTime = System.currentTimeMillis();

        try {
            // 调用原有的重建方法
            boolean result = bloomFilter.rebuildAllBloomFilters();
            long costTime = System.currentTimeMillis() - startTime;

            if (result) {
                log.info("全量布隆过滤器重建成功，耗时:{}ms", costTime);
                XxlJobHelper.log("全量布隆过滤器重建成功，耗时:{}ms", costTime);
            } else {
                log.warn("全量布隆过滤器重建失败或部分失败，耗时:{}ms", costTime);
                XxlJobHelper.log("全量布隆过滤器重建失败或部分失败，耗时:{}ms", costTime);
            }
        } catch (Exception e) {
            log.error("全量布隆过滤器重建异常", e);
            XxlJobHelper.log("全量布隆过滤器重建异常: " + e.getMessage());
            XxlJobHelper.handleFail("重建异常");
        }
    }

    /**
     * 分批重建布隆过滤器 - 可配置不同时间执行
     * 支持分片处理不同业务类型
     */
    @XxlJob("rebuildBusinessBloomFiltersJob")
    public void rebuildBusinessBloomFiltersJob() {
        log.info("开始执行业务布隆过滤器分批重建任务");

        // 获取分片参数，用于处理不同业务类型
        int shardIndex = XxlJobHelper.getShardIndex();
        int shardTotal = XxlJobHelper.getShardTotal();

        // 获取任务参数，例如: shop,voucher,blog
        String param = XxlJobHelper.getJobParam();
        List<BusinessType> businessTypes;

        // 如果提供了参数，则只重建指定业务类型的布隆过滤器
        if (param != null && !param.trim().isEmpty()) {
            businessTypes = Arrays.stream(param.split(","))
                    .map(code -> BusinessType.getByCode(code.trim()))
                    .filter(type -> type != null)
                    .toList();
            log.info("根据参数重建指定业务类型: {}", businessTypes);
        } else {
            // 否则重建所有业务类型
            businessTypes = Arrays.asList(BusinessType.values());
            log.info("重建所有业务类型: {}", businessTypes.size());
        }

        // 按分片选择要处理的业务类型
        List<BusinessType> shardBusinessTypes;
        if (shardTotal > 0) {
            shardBusinessTypes = businessTypes.stream()
                    .filter(type -> Math.abs(type.hashCode() % shardTotal) == shardIndex)
                    .toList();
            log.info("当前分片[{}/{}]处理的业务类型: {}", shardIndex, shardTotal, shardBusinessTypes);
        } else {
            shardBusinessTypes = businessTypes;
        }

        if (shardBusinessTypes.isEmpty()) {
            XxlJobHelper.log("当前分片没有需要处理的业务类型");
            return;
        }

        // 分批次处理每个业务类型
        int success = 0;
        int fail = 0;

        for (BusinessType businessType : shardBusinessTypes) {
            long startTime = System.currentTimeMillis();
            try {
                log.info("开始重建[{}]业务的布隆过滤器", businessType.getCode());

                // 在重建前小睡一段时间，避免同时进行大量IO
                if (success > 0) {
                    TimeUnit.SECONDS.sleep(5);
                }

                boolean result = bloomFilter.rebuildSingleBloomFilter(businessType);
                long costTime = System.currentTimeMillis() - startTime;

                if (result) {
                    log.info("重建[{}]业务布隆过滤器成功，耗时:{}ms", businessType.getCode(), costTime);
                    XxlJobHelper.log("重建[{}]业务布隆过滤器成功，耗时:{}ms", businessType.getCode(), costTime);
                    success++;
                } else {
                    log.warn("重建[{}]业务布隆过滤器失败，耗时:{}ms", businessType.getCode(), costTime);
                    XxlJobHelper.log("重建[{}]业务布隆过滤器失败，耗时:{}ms", businessType.getCode(), costTime);
                    fail++;
                }
            } catch (Exception e) {
                long costTime = System.currentTimeMillis() - startTime;
                log.error("重建[{}]业务布隆过滤器异常，耗时:{}ms", businessType.getCode(), costTime, e);
                XxlJobHelper.log("重建[{}]业务布隆过滤器异常: {}", businessType.getCode(), e.getMessage());
                fail++;
            }
        }

        XxlJobHelper.log("分批重建布隆过滤器完成，成功:{}个，失败:{}个", success, fail);

        // 如果全部失败，则设置任务执行结果为失败
        if (success == 0 && fail > 0) {
            XxlJobHelper.handleFail("全部重建失败");
        }
    }

    /**
     * 布隆过滤器性能监控任务
     */
    @XxlJob("bloomFilterMonitorJob")
    public void bloomFilterMonitorJob() {
        log.info("开始执行布隆过滤器监控任务");

        try {
            // 获取布隆过滤器健康状态
            var healthCheck = bloomFilter.bloomFilterHealthCheck();
            var stats = bloomFilter.getBloomFilterStats();

            XxlJobHelper.log("布隆过滤器性能统计: {}", stats);

            // 分析各业务类型的使用情况
            for (var entry : healthCheck.entrySet()) {
                if (!"stats".equals(entry.getKey())) {
                    XxlJobHelper.log("业务[{}]布隆过滤器状态: {}", entry.getKey(), entry.getValue());
                }
            }

            // 检查是否需要提前重建(根据误判率)
            Long falsePositives = (Long) stats.get("falsePositives");
            Long queries = (Long) stats.get("totalQueries");
            double falsePositiveRate = queries > 0 ? (double) falsePositives / queries : 0;

            if (falsePositiveRate > 0.1) { // 如果误判率超过10%
                XxlJobHelper.log("布隆过滤器误判率过高({}%)，建议重建", falsePositiveRate * 100);
            }
        } catch (Exception e) {
            log.error("布隆过滤器监控任务异常", e);
            XxlJobHelper.log("布隆过滤器监控任务异常: " + e.getMessage());
            XxlJobHelper.handleFail();
        }
    }
}