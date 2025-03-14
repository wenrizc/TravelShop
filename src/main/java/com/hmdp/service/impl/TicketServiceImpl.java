package com.hmdp.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.entity.Ticket;
import com.hmdp.entity.TicketUsage;
import com.hmdp.mapper.TicketMapper;
import com.hmdp.service.ITicketService;
import com.hmdp.service.ITicketUsageService;
import com.hmdp.dto.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TicketServiceImpl extends ServiceImpl<TicketMapper, Ticket> implements ITicketService {

    private final ITicketUsageService ticketUsageService;

    /**
     * 核销门票
     * @param code 核销码
     * @param shopId 商铺ID（验证门票是否属于该商铺）
     * @return 核销结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result verifyTicket(String code, Long shopId) {
        // 1. 查询核销码
        TicketUsage usage = ticketUsageService.getByCode(code);
        if (usage == null) {
            return Result.fail("无效的核销码");
        }

        // 2. 检查门票状态
        if (usage.getStatus() != 1) {
            String statusDesc = "";
            switch (usage.getStatus()) {
                case 2: statusDesc = "门票已使用"; break;
                case 3: statusDesc = "门票已过期"; break;
                case 4: statusDesc = "门票已退款"; break;
                default: statusDesc = "门票状态异常";
            }
            return Result.fail(statusDesc);
        }

        // 3. 检查门票有效期
        if (usage.getExpireTime() != null && LocalDateTime.now().isAfter(usage.getExpireTime())) {
            usage.setStatus(3); // 设为已过期
            usage.setUpdateTime(LocalDateTime.now());
            ticketUsageService.updateById(usage);
            return Result.fail("门票已过期");
        }

        // 4. 检查门票所属商铺
        Ticket ticket = this.getById(usage.getTicketId());
        if (ticket == null) {
            return Result.fail("门票信息不存在");
        }

        if (!ticket.getShopId().equals(shopId)) {
            return Result.fail("该门票不属于当前商铺");
        }

        // 5. 更新门票状态为已使用
        usage.setStatus(2); // 已使用
        usage.setUseTime(LocalDateTime.now());
        usage.setUpdateTime(LocalDateTime.now());
        ticketUsageService.updateById(usage);

        return Result.ok("核销成功");
    }

    @Override
    public Ticket getTicketWithShop(Long id) {
        return baseMapper.getTicketWithShop(id);
    }

    @Override
    public List<Ticket> queryTicketsByShopId(Long shopId) {
        return baseMapper.queryTicketsByShopId(shopId);
    }

    @Override
    public Page<Ticket> queryTicketsPage(int page, int size, Long shopId, Integer typeId) {
        Page<Ticket> pageResult = new Page<>(page, size);
        LambdaQueryWrapper<Ticket> queryWrapper = new LambdaQueryWrapper<>();

        // 条件过滤
        if (shopId != null) {
            queryWrapper.eq(Ticket::getShopId, shopId);
        }
        if (typeId != null) {
            queryWrapper.eq(Ticket::getTypeId, typeId);
        }

        // 只查询有效的门票
        queryWrapper.eq(Ticket::getStatus, 1);

        // 按创建时间降序排序
        queryWrapper.orderByDesc(Ticket::getCreateTime);

        return page(pageResult, queryWrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean createTicket(Ticket ticket) {
        // 设置初始状态
        ticket.setStatus(1); // 设置为正常状态
        ticket.setCreateTime(LocalDateTime.now());
        ticket.setUpdateTime(LocalDateTime.now());

        return save(ticket);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateTicket(Ticket ticket) {
        ticket.setUpdateTime(LocalDateTime.now());
        return updateById(ticket);
    }

    @Override
    public boolean isTicketAvailable(Long ticketId) {
        Ticket ticket = getById(ticketId);
        return ticket != null && ticket.getStatus() == 1;
    }
}