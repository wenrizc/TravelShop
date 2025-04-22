package com.travelshop.service.impl.order;

import com.travelshop.dto.OrderStateChangeDTO;
import com.travelshop.entity.Order;
import com.travelshop.enums.OrderStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class OrderNotifier {

    /**
     * 发送订单状态变更通知
     */
    public void notifyStatusChange(Order order, OrderStatus oldStatus,
                                   OrderStatus newStatus, OrderStateChangeDTO changeDTO) {
        // 记录通知日志
        log.info("订单状态变更: 订单号[{}], 从[{}]变更为[{}], 操作人[{}], 原因[{}]",
                order.getOrderNo(), oldStatus.getDesc(), newStatus.getDesc(),
                changeDTO.getOperator(), changeDTO.getReason());

        // 不同状态变更发送不同类型通知
        switch (newStatus) {
            case PAID:
                sendPaidNotification(order);
                break;
            case DELIVERED:
                sendDeliveryNotification(order);
                break;
            case RECEIVED:
                sendReceiveNotification(order);
                break;
            case COMPLETED:
                sendCompletedNotification(order);
                break;
            case CANCELLED:
                sendCancelledNotification(order, changeDTO.getReason());
                break;
            case REFUND_APPLY:
                sendRefundApplyNotification(order);
                break;
            case REFUNDED:
                sendRefundedNotification(order);
                break;
            default:
                // 其他状态变更不发送特定通知
                break;
        }
    }

    // 以下是各种状态通知的具体实现方法

    private void sendPaidNotification(Order order) {
        // 1. 向用户发送支付成功通知
        log.info("向用户发送支付成功通知: {}", order.getOrderNo());
        // TODO: 接入短信、推送等通知服务

        // 2. 向商家发送新订单通知
        log.info("向商家发送新订单通知: {}", order.getOrderNo());
        // TODO: 接入商家通知系统
    }

    private void sendDeliveryNotification(Order order) {
        // 向用户发送订单已发货通知
        log.info("向用户发送订单已发货通知: {}", order.getOrderNo());
        // TODO: 接入短信、推送等通知服务
    }

    private void sendReceiveNotification(Order order) {
        // 向用户发送待评价通知
        log.info("向用户发送待评价通知: {}", order.getOrderNo());
        // TODO: 接入推送等通知服务
    }

    private void sendCompletedNotification(Order order) {
        // 向用户发送订单完成通知
        log.info("向用户发送订单完成通知: {}", order.getOrderNo());
        // TODO: 接入推送等通知服务
    }

    private void sendCancelledNotification(Order order, String reason) {
        // 向用户发送订单取消通知
        log.info("向用户发送订单取消通知: {}, 原因: {}", order.getOrderNo(), reason);
        // TODO: 接入短信、推送等通知服务
    }

    private void sendRefundApplyNotification(Order order) {
        // 向商家发送退款申请通知
        log.info("向商家发送退款申请通知: {}", order.getOrderNo());
        // TODO: 接入商家通知系统
    }

    private void sendRefundedNotification(Order order) {
        // 向用户发送退款成功通知
        log.info("向用户发送退款成功通知: {}", order.getOrderNo());
        // TODO: 接入短信、推送等通知服务
    }
}