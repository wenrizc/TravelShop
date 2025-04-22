package com.travelshop.service;

import com.travelshop.dto.Result;
import com.travelshop.entity.PaymentRecord;

public interface PaymentService {

    /**
     * 创建支付订单
     *
     * @param orderId 订单ID
     * @param payType 支付方式
     * @return 支付结果，包含支付链接或二维码等信息
     */
    Result createPayment(Long orderId, Integer payType);

    /**
     * 处理支付回调
     *
     * @param transactionId 支付流水号
     * @param status 支付状态
     * @return 处理结果
     */
    Result handlePaymentCallback(String transactionId, Integer status);

    /**
     * 查询支付状态
     *
     * @param orderId 订单ID
     * @return 支付状态
     */
    Result queryPaymentStatus(Long orderId);

    /**
     * 取消支付
     *
     * @param orderId 订单ID
     * @return 取消结果
     */
    Result cancelPayment(Long orderId);

    /**
     * 根据流水号获取支付记录
     *
     * @param transactionId 支付流水号
     * @return 支付记录
     */
    PaymentRecord getByTransactionId(String transactionId);
}