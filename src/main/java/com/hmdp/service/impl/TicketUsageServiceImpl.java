package com.hmdp.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.entity.TicketUsage;
import com.hmdp.mapper.TicketUsageMapper;
import com.hmdp.service.ITicketUsageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class TicketUsageServiceImpl extends ServiceImpl<TicketUsageMapper, TicketUsage> implements ITicketUsageService {

    @Override
    public TicketUsage getByCode(String code) {
        return baseMapper.getByCode(code);
    }

    @Override
    public TicketUsage getByOrderItemId(Long orderItemId) {
        return baseMapper.getByOrderItemId(orderItemId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean useTicket(String code) {
        TicketUsage usage = baseMapper.getByCode(code);
        if (usage == null) {
            log.error("门票不存在: code={}", code);
            return false;
        }

        // 检查状态
        if (usage.getStatus() != 1) {
            log.error("门票状态错误: code={}, status={}", code, usage.getStatus());
            return false;
        }

        // 检查是否过期
        if (usage.getExpireTime() != null && LocalDateTime.now().isAfter(usage.getExpireTime())) {
            // 标记为已过期
            markAsExpired(usage.getId());
            log.error("门票已过期: code={}", code);
            return false;
        }

        // 更新状态为已使用
        return baseMapper.updateStatusToUsed(code, LocalDateTime.now()) > 0;
    }

    @Override
    public boolean markAsExpired(Long id) {
        return baseMapper.updateStatusToExpired(id, LocalDateTime.now()) > 0;
    }

    @Override
    public boolean markAsRefunded(Long orderId) {
        return baseMapper.updateStatusToRefunded(orderId, LocalDateTime.now()) > 0;
    }

    @Override
    public List<TicketUsage> getUserTickets(Long userId) {
        return baseMapper.getUserTickets(userId);
    }

    @Override
    public boolean checkAllTicketsUsed(Long orderItemId) {
        if (orderItemId == null) {
            log.warn("检查门票核销状态失败：订单项ID为空");
            return false;
        }

        // 首先检查是否存在门票记录
        int totalCount = baseMapper.countTotalTickets(orderItemId);
        if (totalCount == 0) {
            log.warn("订单项 {} 没有关联的门票记录", orderItemId);
            return false;
        }

        // 检查未使用的门票数量
        int unusedCount = baseMapper.countUnusedTickets(orderItemId);

        // 记录日志
        log.info("订单项 {} 的门票核销情况：总数={}, 未核销={}", orderItemId, totalCount, unusedCount);

        // 如果没有未使用的门票，说明都已核销
        return unusedCount == 0;
    }
}