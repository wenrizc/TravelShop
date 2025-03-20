package com.hmdp.job;

import com.hmdp.service.ITicketService;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * 门票热度评估任务
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TicketHeatEvaluationJob {

    private final ITicketService ticketService;

    /**
     * 门票热度评估任务 - 分片执行
     */
    @XxlJob("ticketHeatEvaluationJob")
    public void evaluateTicketHeat() {
        log.info("开始执行门票热度评估任务");

        // 获取分片参数
        int shardIndex = XxlJobHelper.getShardIndex();
        int shardTotal = XxlJobHelper.getShardTotal();
        log.info("门票热度评估任务分片参数: 当前分片={}, 总分片={}", shardIndex, shardTotal);

        // 获取执行参数
        String param = XxlJobHelper.getJobParam();
        Map<String, String> paramMap = parseParams(param);

        // 提取参数或使用默认值
        int batchSize = Integer.parseInt(paramMap.getOrDefault("batchSize", "100"));
        boolean immediateApply = Boolean.parseBoolean(paramMap.getOrDefault("immediateApply", "false"));
        String periodType = paramMap.getOrDefault("periodType", "daily"); // daily, weekly, monthly

        try {
            long startTime = System.currentTimeMillis();

            // 随机延迟1-3秒，避免所有分片同时开始
            if (shardIndex > 0) {
                TimeUnit.MILLISECONDS.sleep(ThreadLocalRandom.current().nextInt(1000, 3000));
            }

            // 执行热度评估
            int count = ticketService.evaluateTicketHeat(
                    shardIndex,
                    shardTotal,
                    batchSize,
                    periodType,
                    immediateApply
            );

            long costTime = System.currentTimeMillis() - startTime;
            log.info("门票热度评估完成，处理了{}个门票，耗时{}ms", count, costTime);
            XxlJobHelper.log("门票热度评估完成，处理了{}个门票，耗时{}ms", count, costTime);

        } catch (Exception e) {
            log.error("门票热度评估任务异常", e);
            XxlJobHelper.log("门票热度评估任务异常: " + e.getMessage());
            XxlJobHelper.handleFail();
        }
    }

    /**
     * 解析参数字符串，格式: key1=value1,key2=value2
     */
    private Map<String, String> parseParams(String paramStr) {
        Map<String, String> result = new HashMap<>();
        if (paramStr != null && !paramStr.trim().isEmpty()) {
            String[] pairs = paramStr.split(",");
            for (String pair : pairs) {
                String[] keyValue = pair.split("=", 2);
                if (keyValue.length == 2) {
                    result.put(keyValue[0].trim(), keyValue[1].trim());
                }
            }
        }
        return result;
    }
}