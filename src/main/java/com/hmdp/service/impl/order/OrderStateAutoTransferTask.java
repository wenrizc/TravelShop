package com.hmdp.service.impl.order;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hmdp.dto.OrderStateChangeDTO;
import com.hmdp.entity.Order;
import com.hmdp.enums.OperatorType;
import com.hmdp.enums.OrderStatus;
import com.hmdp.service.IOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderStateAutoTransferTask {

    private final IOrderService orderService;
    private final OrderStateMachine orderStateMachine;

    /**
     * 自动取消超时未支付订单
     * 每5分钟执行一次
     */
    @Scheduled(cron = "0 */5 * * * ?")
    public void autoCancelUnpaidOrders() {
        log.info("开始执行超时未支付订单自动取消任务");

        // 设置支付超时时间（例如：30分钟）
        LocalDateTime deadline = LocalDateTime.now().minusMinutes(30);

        // 查询超时未支付的订单
        LambdaQueryWrapper<Order> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Order::getStatus, OrderStatus.WAIT_PAY.getCode())
                .lt(Order::getCreateTime, deadline);

        List<Order> orders = orderService.list(queryWrapper);

        // 批量处理超时订单
        for (Order order : orders) {
            try {
                // 构建状态变更参数
                OrderStateChangeDTO changeDTO = new OrderStateChangeDTO();
                changeDTO.setOrderId(order.getId());
                changeDTO.setTargetStatus(OrderStatus.CANCELLED.getCode());
                changeDTO.setOperator("system");
                changeDTO.setOperatorType(OperatorType.SYSTEM);
                changeDTO.setReason("支付超时，系统自动取消");

                // 调用状态机进行状态变更
                orderStateMachine.changeState(changeDTO);
                log.info("订单[{}]超时未支付，已自动取消", order.getOrderNo());
            } catch (Exception e) {
                log.error("取消超时订单[{}]失败", order.getOrderNo(), e);
            }
        }

        log.info("超时未支付订单自动取消任务执行完成，共处理{}个订单", orders.size());
    }

    /**
     * 自动确认收货
     * 每天凌晨2点执行
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void autoConfirmReceive() {
        log.info("开始执行超时未确认收货订单自动收货任务");

        // 设置自动确认收货时间（例如：发货后15天）
        LocalDateTime deadline = LocalDateTime.now().minusDays(15);

        // 查询超时未确认收货的订单
        LambdaQueryWrapper<Order> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Order::getStatus, OrderStatus.DELIVERED.getCode())
                .lt(Order::getDeliveryTime, deadline);

        List<Order> orders = orderService.list(queryWrapper);

        // 批量处理超时订单
        for (Order order : orders) {
            try {
                // 构建状态变更参数
                OrderStateChangeDTO changeDTO = new OrderStateChangeDTO();
                changeDTO.setOrderId(order.getId());
                changeDTO.setTargetStatus(OrderStatus.RECEIVED.getCode());
                changeDTO.setOperator("system");
                changeDTO.setOperatorType(OperatorType.SYSTEM);
                changeDTO.setReason("发货超过15天，系统自动确认收货");

                // 调用状态机进行状态变更
                orderStateMachine.changeState(changeDTO);
                log.info("订单[{}]发货超过15天，已自动确认收货", order.getOrderNo());
            } catch (Exception e) {
                log.error("自动确认收货订单[{}]失败", order.getOrderNo(), e);
            }
        }

        log.info("超时未确认收货订单自动收货任务执行完成，共处理{}个订单", orders.size());
    }

    /**
     * 自动完成订单
     * 每天凌晨3点执行
     */
    @Scheduled(cron = "0 0 3 * * ?")
    public void autoCompleteOrders() {
        log.info("开始执行待评价订单自动完成任务");

        // 设置自动完成订单时间（例如：确认收货后7天）
        LocalDateTime deadline = LocalDateTime.now().minusDays(7);

        // 查询超时未评价的订单
        LambdaQueryWrapper<Order> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Order::getStatus, OrderStatus.RECEIVED.getCode())
                .lt(Order::getReceiveTime, deadline);

        List<Order> orders = orderService.list(queryWrapper);

        // 批量处理超时订单
        for (Order order : orders) {
            try {
                // 构建状态变更参数
                OrderStateChangeDTO changeDTO = new OrderStateChangeDTO();
                changeDTO.setOrderId(order.getId());
                changeDTO.setTargetStatus(OrderStatus.COMPLETED.getCode());
                changeDTO.setOperator("system");
                changeDTO.setOperatorType(OperatorType.SYSTEM);
                changeDTO.setReason("确认收货超过7天，系统自动完成订单");

                // 调用状态机进行状态变更
                orderStateMachine.changeState(changeDTO);
                log.info("订单[{}]确认收货超过7天，已自动完成", order.getOrderNo());
            } catch (Exception e) {
                log.error("自动完成订单[{}]失败", order.getOrderNo(), e);
            }
        }

        log.info("待评价订单自动完成任务执行完成，共处理{}个订单", orders.size());
    }

    /**
     * 自动关闭长期申请退款但未处理的订单
     * 每周一凌晨4点执行
     */
    @Scheduled(cron = "0 0 4 ? * MON")
    public void autoCloseStaleRefundOrders() {
        log.info("开始执行长期退款申请订单自动关闭任务");

        // 设置超时时间（例如：申请退款后30天仍未处理）
        LocalDateTime deadline = LocalDateTime.now().minusDays(30);

        // 查询长期处于退款申请状态的订单
        LambdaQueryWrapper<Order> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Order::getStatus, OrderStatus.REFUND_APPLY.getCode())
                .lt(Order::getUpdateTime, deadline);

        List<Order> orders = orderService.list(queryWrapper);

        // 批量处理超时订单
        for (Order order : orders) {
            try {
                // 构建状态变更参数
                OrderStateChangeDTO changeDTO = new OrderStateChangeDTO();
                changeDTO.setOrderId(order.getId());
                changeDTO.setTargetStatus(OrderStatus.CLOSED.getCode());
                changeDTO.setOperator("system");
                changeDTO.setOperatorType(OperatorType.SYSTEM);
                changeDTO.setReason("退款申请长期未处理，系统自动关闭订单");

                // 调用状态机进行状态变更
                orderStateMachine.changeState(changeDTO);
                log.info("订单[{}]退款申请长期未处理，已自动关闭", order.getOrderNo());
            } catch (Exception e) {
                log.error("自动关闭长期退款申请订单[{}]失败", order.getOrderNo(), e);
            }
        }

        log.info("长期退款申请订单自动关闭任务执行完成，共处理{}个订单", orders.size());
    }
}