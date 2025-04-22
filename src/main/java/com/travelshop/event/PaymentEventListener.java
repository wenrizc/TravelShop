package com.travelshop.event;

import com.travelshop.entity.Order;
import com.travelshop.entity.OrderItem;
import com.travelshop.entity.PaymentRecord;
import com.travelshop.enums.OrderStatus;
import com.travelshop.mapper.OrderItemMapper;
import com.travelshop.mapper.OrderMapper;
import com.travelshop.service.PaymentService;
import com.travelshop.service.strategy.ProductTypeHandler;
import com.travelshop.service.strategy.ProductTypeHandlerFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 支付事件监听器
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventListener {

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final ProductTypeHandlerFactory productTypeHandlerFactory;
    private final PaymentService paymentService;

    /**
     * 处理支付成功事件
     */
    @Async
    @EventListener
    @Transactional(rollbackFor = Exception.class)
    public void handlePaymentSuccessEvent(PaymentEvent event) {
        if (event.getType() != PaymentEvent.Type.PAYMENT_SUCCESS) {
            return;
        }

        log.info("处理支付成功事件: {}", event);
        Long orderId = event.getOrderId();
        String transactionId = event.getTransactionId();

        // 1. 验证订单存在且状态正确
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            log.error("订单不存在: {}", orderId);
            return;
        }

        // 2. 检查支付状态，确保幂等性
        PaymentRecord paymentRecord = paymentService.getByTransactionId(transactionId);
        if (paymentRecord == null) {
            log.error("支付记录不存在: {}", transactionId);
            return;
        }

        // 已处理过的支付，直接返回
        if (paymentRecord.getStatus() == 3 && order.getStatus().equals(OrderStatus.PAID.getCode())) {
            log.info("订单已处理支付: orderId={}, transactionId={}", orderId, transactionId);
            return;
        }

        // 3. 更新订单状态
        Order updateOrder = new Order();
        updateOrder.setId(orderId);
        updateOrder.setStatus(OrderStatus.PAID.getCode());
        updateOrder.setUpdateTime(LocalDateTime.now());
        updateOrder.setPayTime(LocalDateTime.now());
        updateOrder.setPayType(event.getPayType());
        orderMapper.updateById(updateOrder);


        // 5. 处理订单项相关业务
        List<OrderItem> orderItems = orderItemMapper.selectByOrderId(orderId);
        for (OrderItem item : orderItems) {
            try {
                ProductTypeHandler handler = productTypeHandlerFactory.getHandler(item.getProductType());
                handler.processAfterPayment(order, item);
            } catch (Exception e) {
                log.error("处理订单项支付后业务失败: orderItemId={}", item.getId(), e);
                // 异常处理，可以记录失败日志或发送告警，但不影响整体流程
            }
        }

        log.info("订单支付成功处理完成: orderId={}, transactionId={}", orderId, transactionId);
    }

    /**
     * 处理支付失败事件
     */
    @Async
    @EventListener
    @Transactional(rollbackFor = Exception.class)
    public void handlePaymentFailedEvent(PaymentEvent event) {
        if (event.getType() != PaymentEvent.Type.PAYMENT_FAILED) {
            return;
        }

        log.info("处理支付失败事件: {}", event);
        // 处理支付失败逻辑...
    }
}