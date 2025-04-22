package com.travelshop.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 支付事件
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentEvent {

    /**
     * 支付事件类型
     */
    public enum Type {
        PAYMENT_CREATED,    // 支付创建
        PAYMENT_SUCCESS,    // 支付成功
        PAYMENT_FAILED,     // 支付失败
        PAYMENT_CANCELLED   // 支付取消
    }

    /**
     * 事件ID
     */
    private String eventId;

    /**
     * 事件类型
     */
    private Type type;

    /**
     * 订单ID
     */
    private Long orderId;

    /**
     * 支付流水号
     */
    private String transactionId;

    /**
     * 事件时间
     */
    private LocalDateTime eventTime;

    /**
     * 支付方式
     */
    private Integer payType;

    /**
     * 额外数据
     */
    private String data;

    /**
     * 生成支付成功事件
     */
    public static PaymentEvent paymentSuccess(Long orderId, String transactionId, Integer payType) {
        return new PaymentEvent(
                java.util.UUID.randomUUID().toString(),
                Type.PAYMENT_SUCCESS,
                orderId,
                transactionId,
                LocalDateTime.now(),
                payType,
                null
        );
    }

    /**
     * 生成支付失败事件
     */
    public static PaymentEvent paymentFailed(Long orderId, String transactionId, Integer payType, String reason) {
        return new PaymentEvent(
                java.util.UUID.randomUUID().toString(),
                Type.PAYMENT_FAILED,
                orderId,
                transactionId,
                LocalDateTime.now(),
                payType,
                reason
        );
    }
}