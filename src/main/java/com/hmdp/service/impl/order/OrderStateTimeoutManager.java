package com.hmdp.service.impl.order;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hmdp.dto.OrderStateChangeDTO;
import com.hmdp.entity.Order;
import com.hmdp.enums.OperatorType;
import com.hmdp.enums.OrderStatus;
import com.hmdp.service.IOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;


@Slf4j
@Component
@RequiredArgsConstructor
public class OrderStateTimeoutManager {
    private final IOrderService orderService;
    private final OrderStateMachine orderStateMachine;

    @Value("${order.timeout.payment-minutes:30}")
    private Integer paymentTimeoutMinutes;

    @Value("${order.timeout.confirm-days:7}")
    private Integer confirmTimeoutDays;

    @Value("${order.timeout.auto-complete-days:15}")
    private Integer autoCompleteDays;

    /**
     * 定时任务：处理超时未支付订单
     * 每分钟执行一次
     */
    @Scheduled(cron = "0 */1 * * * ?")
    public void handlePaymentTimeout() {
        log.info("开始处理超时未支付订单...");

        // 计算超时时间点
        LocalDateTime timeoutPoint = LocalDateTime.now().minusMinutes(paymentTimeoutMinutes);

        // 查询超时未支付的订单
        LambdaQueryWrapper<Order> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Order::getStatus, OrderStatus.WAIT_PAY.getCode())
                .lt(Order::getCreateTime, timeoutPoint)
                .eq(Order::getIsDeleted, 0);

        List<Order> timeoutOrders = orderService.list(queryWrapper);
        log.info("找到 {} 个超时未支付订单", timeoutOrders.size());

        // 处理每个超时订单
        for (Order order : timeoutOrders) {
            try {
                // 构建状态变更DTO
                OrderStateChangeDTO changeDTO = new OrderStateChangeDTO();
                changeDTO.setOrderId(order.getId());
                changeDTO.setTargetStatus(OrderStatus.CANCELLED.getCode());
                changeDTO.setOperator("system");
                changeDTO.setOperatorType(OperatorType.SYSTEM);
                changeDTO.setReason("超时未支付，系统自动取消");

                // 执行状态转换
                orderStateMachine.changeState(changeDTO);
                log.info("成功取消超时未支付订单: {}", order.getOrderNo());
            } catch (Exception e) {
                log.error("取消超时未支付订单失败: {}, 异常: {}", order.getOrderNo(), e.getMessage(), e);
            }
        }
    }

    /**
     * 定时任务：处理超时未确认收货的订单
     * 每天凌晨1点执行
     */
    @Scheduled(cron = "0 0 1 * * ?")
    public void handleConfirmReceiptTimeout() {
        log.info("开始处理超时未确认收货订单...");

        // 计算超时时间点 (发货后X天自动确认收货)
        LocalDateTime timeoutPoint = LocalDateTime.now().minusDays(confirmTimeoutDays);

        // 查询超时未确认收货的订单
        LambdaQueryWrapper<Order> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Order::getStatus, OrderStatus.DELIVERED.getCode())
                .lt(Order::getDeliveryTime, timeoutPoint)
                .eq(Order::getIsDeleted, 0);

        List<Order> timeoutOrders = orderService.list(queryWrapper);
        log.info("找到 {} 个超时未确认收货订单", timeoutOrders.size());

        // 处理每个超时订单
        for (Order order : timeoutOrders) {
            try {
                // 构建状态变更DTO
                OrderStateChangeDTO changeDTO = new OrderStateChangeDTO();
                changeDTO.setOrderId(order.getId());
                changeDTO.setTargetStatus(OrderStatus.RECEIVED.getCode());
                changeDTO.setOperator("system");
                changeDTO.setOperatorType(OperatorType.SYSTEM);
                changeDTO.setReason("超时未确认收货，系统自动确认");

                // 执行状态转换
                orderStateMachine.changeState(changeDTO);
                log.info("成功自动确认收货: {}", order.getOrderNo());
            } catch (Exception e) {
                log.error("自动确认收货失败: {}, 异常: {}", order.getOrderNo(), e.getMessage(), e);
            }
        }
    }

    /**
     * 定时任务：处理收货后超时未评价的订单自动完成
     * 每天凌晨2点执行
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void handleAutoCompleteTimeout() {
        log.info("开始处理超时未评价自动完成订单...");

        // 计算超时时间点 (收货后X天自动完成)
        LocalDateTime timeoutPoint = LocalDateTime.now().minusDays(autoCompleteDays);

        // 查询超时未评价的订单
        LambdaQueryWrapper<Order> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Order::getStatus, OrderStatus.RECEIVED.getCode())
                .lt(Order::getReceiveTime, timeoutPoint)
                .eq(Order::getIsDeleted, 0);

        List<Order> timeoutOrders = orderService.list(queryWrapper);
        log.info("找到 {} 个超时未评价订单", timeoutOrders.size());

        // 处理每个超时订单
        for (Order order : timeoutOrders) {
            try {
                // 构建状态变更DTO
                OrderStateChangeDTO changeDTO = new OrderStateChangeDTO();
                changeDTO.setOrderId(order.getId());
                changeDTO.setTargetStatus(OrderStatus.COMPLETED.getCode());
                changeDTO.setOperator("system");
                changeDTO.setOperatorType(OperatorType.SYSTEM);
                changeDTO.setReason("收货后超时未评价，系统自动完成订单");

                // 执行状态转换
                orderStateMachine.changeState(changeDTO);
                log.info("成功自动完成订单: {}", order.getOrderNo());
            } catch (Exception e) {
                log.error("自动完成订单失败: {}, 异常: {}", order.getOrderNo(), e.getMessage(), e);
            }
        }
    }
}