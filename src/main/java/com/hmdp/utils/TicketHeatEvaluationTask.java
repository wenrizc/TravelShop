package com.hmdp.utils;

import com.hmdp.service.ITicketService;
import com.hmdp.service.ITicketSkuService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class TicketHeatEvaluationTask {
    private final StringRedisTemplate redisTemplate;
    private final TicketHeatManager heatManager;
    private final ITicketService ticketService;
    private final ITicketSkuService ticketSkuService;
    private final UnifiedCache unifiedCache;

    @Scheduled(cron = "0 0 3 * * ?") // 每天凌晨3点执行
    public void evaluateTicketHeat() {
        log.info("开始执行门票热度评估任务");

        // 1. 获取所有门票访问记录
        Set<String> keys = redisTemplate.keys(TicketHeatManager.TICKET_ACCESS_KEY + "*");
        if (keys == null || keys.isEmpty()) {
            log.info("没有门票访问记录，评估任务结束");
            return;
        }

        // 2. 获取当前热门门票集合
        Set<String> hotTickets = redisTemplate.opsForSet().members(TicketHeatManager.HOT_TICKET_SET_KEY);
        Set<String> currentHotIds = hotTickets != null ? hotTickets : new HashSet<>();
        log.info("当前热门门票数量: {}", currentHotIds.size());

        // 3. 重新评估热度
        int newHotCount = 0;
        int removedCount = 0;

        for (String key : keys) {
            String ticketId = key.substring(key.lastIndexOf(":") + 1);
            String countStr = redisTemplate.opsForValue().get(key);
            long count = countStr != null ? Long.parseLong(countStr) : 0;

            if (count >= TicketHeatManager.HOT_THRESHOLD && !currentHotIds.contains(ticketId)) {
                // 新晋热门门票
                redisTemplate.opsForSet().add(TicketHeatManager.HOT_TICKET_SET_KEY, ticketId);
                log.info("门票[{}]升级为热门门票, 访问量: {}", ticketId, count);
                newHotCount++;

                // 预热新的热门门票数据
                preloadNewHotTicket(Long.valueOf(ticketId));

            } else if (count < TicketHeatManager.HOT_THRESHOLD && currentHotIds.contains(ticketId)) {
                // 降级为冷门门票
                redisTemplate.opsForSet().remove(TicketHeatManager.HOT_TICKET_SET_KEY, ticketId);
                log.info("门票[{}]降级为普通门票, 访问量: {}", ticketId, count);
                removedCount++;

                // 处理降级逻辑
                handleTicketDowngrade(Long.valueOf(ticketId));
            }

            // 计数器减半，保留历史热度影响但避免永远是热门
            redisTemplate.opsForValue().set(key, String.valueOf(count / 2));
        }

        log.info("门票热度评估任务完成, 新增热门门票: {}, 降级门票: {}", newHotCount, removedCount);
    }

    // 预热新晋热门门票
    private void preloadNewHotTicket(Long ticketId) {
        try {
            // 1. 预加载门票基本信息到缓存
            ticketService.queryTicketById(ticketId); // 触发缓存加载

            // 2. 预加载库存信息到Redis
            List<Long> skuIds = ticketSkuService.queryByTicketId(ticketId)
                    .stream()
                    .map(sku -> sku.getId())
                    .toList();

            for (Long skuId : skuIds) {
                String stockKey = "ticket:stock:" + skuId;
                // 将库存预加载到Redis (实际项目中需要加载真实库存)
                //redisTemplate.opsForValue().set(stockKey, String.valueOf(sku.getStock()));
            }

            log.info("门票[{}]预热完成，包含{}个SKU规格", ticketId, skuIds.size());
        } catch (Exception e) {
            log.error("预热热门门票[{}]数据失败", ticketId, e);
        }
    }

    // 处理门票降级
    private void handleTicketDowngrade(Long ticketId) {
        // 这里可以进行一些清理工作，如调整缓存策略等
        String cacheKey = "cache:ticket:" + ticketId;
        // 可以选择直接删除缓存，让其按普通门票重新加载
        unifiedCache.delete(cacheKey);
        log.info("门票[{}]降级，已清除缓存", ticketId);
    }
}