package com.hmdp.controller;


import com.hmdp.dto.OrderCreateDTO;
import com.hmdp.dto.OrderQueryDTO;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.OrderStatusHistory;
import com.hmdp.service.IOrderService;
import com.hmdp.service.IOrderStateService;
import com.hmdp.utils.UserHolder;
import com.hmdp.dto.Result;
import lombok.RequiredArgsConstructor;
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
}