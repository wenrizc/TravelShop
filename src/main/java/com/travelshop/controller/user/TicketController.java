package com.travelshop.controller.user;

import com.travelshop.dto.Result;
import com.travelshop.dto.UserDTO;
import com.travelshop.entity.Ticket;
import com.travelshop.entity.TicketSku;
import com.travelshop.entity.TicketUsage;
import com.travelshop.mapper.TicketSkuMapper;
import com.travelshop.service.ITicketService;
import com.travelshop.service.ITicketUsageService;
import com.travelshop.utils.UserHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 门票控制器
 */
@RestController
@RequestMapping("/tickets")
@RequiredArgsConstructor
public class TicketController {

    private final ITicketService ticketService;
    private final TicketSkuMapper ticketSkuMapper;
    private final ITicketUsageService ticketUsageService;

    /**
     * 获取门票详情
     */
    @GetMapping("/{id}")
    public Result getTicket(@PathVariable("id") Long id) {
        Ticket ticket = ticketService.getById(id);
        if (ticket == null) {
            return Result.fail("门票不存在");
        }

        // 加载门票规格
        List<TicketSku> skus = ticketSkuMapper.queryByTicketId(id);
        ticket.setSkus(skus);

        return Result.ok(ticket);
    }
    /**
     * 获取门票详情
     */
    @PostMapping("/verify")
    public Result verifyTicket(@RequestParam("code") String code, @RequestParam(value = "shopId", required = false) Long shopId) {
        // 如果提供了商铺ID，使用verifyTicket方法（包含商铺验证）
        if (shopId != null) {
            return ticketService.verifyTicket(code, shopId);
        }

        // 否则使用简单核销方法
        TicketUsage usage = ticketUsageService.getByCode(code);
        if (usage == null) {
            return Result.fail("门票不存在");
        }

        // 核销门票
        boolean success = ticketUsageService.useTicket(code);
        if (success) {
            // 检查是否所有该订单项下的门票都已核销
            boolean allUsed = ticketUsageService.checkAllTicketsUsed(usage.getOrderItemId());
            if (allUsed) {
                // 可以标记该订单项为已完成状态
                // TODO: 更新订单项状态为已完成
                // orderItemService.markAsCompleted(usage.getOrderItemId());
            }
        }

        return success ? Result.ok("核销成功") : Result.fail("核销失败");
    }
    /**
     * 门票核销
     */
    @PostMapping("/ticket/use")
    public Result useTicket(@RequestParam("code") String code) {
        boolean success = ticketUsageService.useTicket(code);
        return success ? Result.ok() : Result.fail("门票核销失败");
    }

    /**
     * 查询我的门票列表
     */
    @GetMapping("/tickets/list")
    public Result myTickets() {
        UserDTO user = UserHolder.getUser();
        List<TicketUsage> tickets = ticketUsageService.getUserTickets(user.getId());
        return Result.ok(tickets);
    }

}