package com.travelshop.utils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Component
@Slf4j
@RequiredArgsConstructor
public class TicketHeatManager {
    private final StringRedisTemplate redisTemplate;
    public static final String HOT_TICKET_SET_KEY = "cache:hot_ticket:set";
    public static final String TICKET_ACCESS_KEY = "stats:ticket:access:";
    public static final int HOT_THRESHOLD = 100; // 热门门票访问阈值
    public static final long HOT_CACHE_TTL = 30L; // 热门门票缓存时间(分钟)
    public static final long NORMAL_CACHE_TTL = 10L; // 普通门票缓存时间(分钟)

    // 判断门票是否热门
    public boolean isHotTicket(Long ticketId) {
        return Boolean.TRUE.equals(redisTemplate.opsForSet().isMember(HOT_TICKET_SET_KEY, ticketId.toString()));
    }

    // 记录门票访问并更新热度
    public void recordAccess(Long ticketId) {
        String key = TICKET_ACCESS_KEY + ticketId;
        Long count = redisTemplate.opsForValue().increment(key);

        // 确保计数器有过期时间(7天)，避免长期占用内存
        redisTemplate.expire(key, 7, TimeUnit.DAYS);

        // 达到热门阈值时，加入热门集合
        if (count != null && count == HOT_THRESHOLD) {
            redisTemplate.opsForSet().add(HOT_TICKET_SET_KEY, ticketId.toString());
            log.info("门票[{}]已达到热门阈值，标记为热门门票", ticketId);

            // 触发热门门票的预热
            preloadHotTicket(ticketId);
        }
    }

    // 获取所有热门门票ID
    public List<Long> getAllHotTicketIds() {
        Set<String> members = redisTemplate.opsForSet().members(HOT_TICKET_SET_KEY);
        if (members == null || members.isEmpty()) {
            return List.of();
        }
        return members.stream()
                .map(Long::valueOf)
                .collect(Collectors.toList());
    }

    // 预热热门门票数据
    private void preloadHotTicket(Long ticketId) {
        // 这里可以添加预热逻辑，如预加载门票详情到缓存
        log.info("开始预热热门门票[{}]的数据", ticketId);
        // 触发库存预加载等操作
        preloadTicketStock(ticketId);
    }

    // 预热门票库存数据到Redis
    private void preloadTicketStock(Long ticketId) {
        // 实际实现中可以调用服务加载门票的SKU库存到Redis
        log.info("预热门票[{}]的库存数据到Redis", ticketId);
    }

    // 判断并获取门票的缓存时间
    public long getTicketCacheTtl(Long ticketId) {
        return isHotTicket(ticketId) ? HOT_CACHE_TTL : NORMAL_CACHE_TTL;
    }

    // 判断是否使用逻辑过期
    public boolean shouldUseLogicalExpire(Long ticketId) {
        return isHotTicket(ticketId);
    }
}