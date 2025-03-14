package com.hmdp.controller;


import com.hmdp.dto.OrderCreateDTO;
import com.hmdp.dto.OrderQueryDTO;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.OrderStatusHistory;
import com.hmdp.entity.TicketUsage;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.service.IOrderService;
import com.hmdp.service.IOrderStateService;
import com.hmdp.service.ITicketUsageService;
import com.hmdp.service.IVoucherOrderService;
import com.hmdp.utils.UserHolder;
import com.hmdp.dto.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Lazy;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 订单控制器
 */
@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final IOrderService orderService;
    private final IOrderStateService orderStateService;
    private final ITicketUsageService ticketUsageService;

    @Lazy
    private final IVoucherOrderService voucherOrderService;
    /**
     * 创建订单
     */
    @PostMapping
    public Result createOrder(OrderCreateDTO orderCreateDTO) {
        return Result.ok(orderService.createOrder(orderCreateDTO));
    }

    /**
     * 获取订单详情
     */
    @GetMapping("/{id}")
    public Result getOrderDetail(@PathVariable("id") Long orderId) {
        return Result.ok(orderService.getOrderDetail(orderId));
    }

    /**
     * 获取订单状态变更历史
     */
    @GetMapping("/{id}/history")
    public Result getOrderHistory(@PathVariable("id") Long orderId) {
        List<OrderStatusHistory> historyList = orderService.getHistoryByOrderId(orderId);
        return Result.ok(historyList);
    }

    /**
     * 分页查询用户订单
     */
    @GetMapping
    public Result queryUserOrders(OrderQueryDTO queryDTO) {
        UserDTO user = UserHolder.getUser();
        return Result.ok(orderService.queryUserOrders(user.getId(), queryDTO));
    }

    /**
     * 支付订单
     */
    @PostMapping("/{id}/pay")
    public Result payOrder(@PathVariable("id") Long orderId, @RequestParam Integer payType) {
        return Result.ok(orderService.payOrder(orderId, payType));
    }

    /**
     * 取消订单
     */
    @PostMapping("/{id}/cancel")
    public Result cancelOrder(@PathVariable("id") Long orderId, @RequestParam String reason) {
        UserDTO user = UserHolder.getUser();
        boolean success = orderStateService.cancelOrder(orderId, reason, user.getId().toString());
        return Result.ok(success);
    }

    /**
     * 确认收货
     */
    @PostMapping("/{id}/receive")
    public Result confirmReceive(@PathVariable("id") Long orderId) {
        UserDTO user = UserHolder.getUser();
        boolean success = orderStateService.confirmReceive(orderId, user.getId());
        return Result.ok(success);
    }

    /**
     * 申请退款
     */
    @PostMapping("/{id}/refund/apply")
    public Result applyRefund(@PathVariable("id") Long orderId, @RequestParam String reason) {
        UserDTO user = UserHolder.getUser();
        boolean success = orderStateService.applyRefund(orderId, user.getId(), reason);
        return Result.ok(success);
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
     * 优惠券核销
     */
    @PostMapping("/voucher/use/{id}")
    public Result useVoucher(@PathVariable("id") Long id) {
        UserDTO user = UserHolder.getUser();

        // 验证优惠券所有权
        VoucherOrder voucherOrder = voucherOrderService.getById(id);
        if (voucherOrder == null) {
            return Result.fail("优惠券不存在");
        }

        if (!voucherOrder.getUserId().equals(user.getId())) {
            return Result.fail("无权使用此优惠券");
        }

        // 检查优惠券状态
        if (voucherOrder.getStatus() != 2) { // 假设2是已支付待使用状态
            String statusMsg = "";
            switch (voucherOrder.getStatus()) {
                case 1: statusMsg = "优惠券未支付"; break;
                case 3: statusMsg = "优惠券已使用"; break;
                case 4: statusMsg = "优惠券已过期"; break;
                case 5: statusMsg = "优惠券已退款"; break;
                default: statusMsg = "优惠券状态异常";
            }
            return Result.fail(statusMsg);
        }

        // 核销优惠券
        boolean success = voucherOrderService.useVoucher(id);
        return success ? Result.ok("核销成功") : Result.fail("核销失败");
    }


    /**
     * 查询我的门票列表
     */
    @GetMapping("/tickets")
    public Result myTickets() {
        UserDTO user = UserHolder.getUser();
        List<TicketUsage> tickets = ticketUsageService.getUserTickets(user.getId());
        return Result.ok(tickets);
    }

    /**
     * 查询我的优惠券列表
     */
    @GetMapping("/vouchers")
    public Result myVouchers() {
        UserDTO user = UserHolder.getUser();
        List<VoucherOrder> vouchers = voucherOrderService.getUserVouchers(user.getId());
        return Result.ok(vouchers);
    }


}