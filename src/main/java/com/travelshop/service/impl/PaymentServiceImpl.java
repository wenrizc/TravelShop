package com.travelshop.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.travelshop.dto.Result;
import com.travelshop.entity.Order;
import com.travelshop.entity.PaymentRecord;
import com.travelshop.enums.OrderStatus;
import com.travelshop.event.PaymentEvent;
import com.travelshop.event.PaymentEventPublisher;
import com.travelshop.mapper.OrderMapper;
import com.travelshop.mapper.PaymentRecordMapper;
import com.travelshop.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 支付服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl extends ServiceImpl<PaymentRecordMapper, PaymentRecord> implements PaymentService {

    private final OrderMapper orderMapper;
    private final PaymentRecordMapper paymentRecordMapper;
    private final PaymentEventPublisher paymentEventPublisher;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result createPayment(Long orderId, Integer payType) {
        // 1. 检查订单状态
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            return Result.fail("订单不存在");
        }

        if (!OrderStatus.WAIT_PAY.getCode().equals(order.getStatus())) {
            return Result.fail("订单状态不允许支付");
        }

        // 2. 检查是否已有支付中的记录，确保幂等性
        PaymentRecord existingRecord = paymentRecordMapper.getByOrderId(orderId);
        if (existingRecord != null && (existingRecord.getStatus() == 2 || existingRecord.getStatus() == 3)) {
            return Result.fail("订单支付已处理");
        }

        // 3. 生成支付记录
        String transactionId = generateTransactionId();
        PaymentRecord paymentRecord = new PaymentRecord();
        paymentRecord.setOrderId(orderId);
        paymentRecord.setTransactionId(transactionId);
        paymentRecord.setAmount(order.getPayAmount());
        paymentRecord.setPayType(payType);
        paymentRecord.setStatus(1); // 待支付
        paymentRecord.setCreateTime(LocalDateTime.now());
        paymentRecord.setUpdateTime(LocalDateTime.now());
        paymentRecord.setCallbackCount(0);
        save(paymentRecord);

        // 4. 调用第三方支付接口，这里只是演示
        // 实际项目需根据payType调用不同支付渠道API，获取支付链接或二维码等
        String paymentUrl = "https://pay.example.com?orderId=" + orderId + "&amount=" + order.getPayAmount();

        log.info("创建支付订单成功: orderId={}, transactionId={}, paymentUrl={}",
                orderId, transactionId, paymentUrl);

        return Result.ok(paymentUrl);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result handlePaymentCallback(String transactionId, Integer status) {
        log.info("接收支付回调: transactionId={}, status={}", transactionId, status);

        // 1. 验证支付记录存在
        PaymentRecord paymentRecord = paymentRecordMapper.getByTransactionId(transactionId);
        if (paymentRecord == null) {
            log.error("支付记录不存在: {}", transactionId);
            return Result.fail("支付记录不存在");
        }

        // 2. 防止重复处理：已处理的支付不再处理
        if (paymentRecord.getStatus() == 3 || paymentRecord.getStatus() == 4) {
            log.info("支付已处理，忽略重复回调: transactionId={}, status={}", transactionId, paymentRecord.getStatus());
            return Result.ok("支付已处理");
        }

        // 3. 更新支付记录状态
        LocalDateTime payTime = status == 3 ? LocalDateTime.now() : null;
        int updated = paymentRecordMapper.updatePaymentStatus(transactionId, status, payTime);
        if (updated == 0) {
            log.warn("支付状态更新失败，可能已被其他线程处理: {}", transactionId);
            return Result.fail("更新支付状态失败");
        }

        // 4. 发布事件处理后续业务
        Long orderId = paymentRecord.getOrderId();
        if (status == 3) {  // 支付成功
            PaymentEvent event = PaymentEvent.paymentSuccess(
                    orderId, transactionId, paymentRecord.getPayType());
            paymentEventPublisher.publishEvent(event);
            return Result.ok("支付成功处理中");
        } else if (status == 4) {  // 支付失败
            PaymentEvent event = PaymentEvent.paymentFailed(
                    orderId, transactionId, paymentRecord.getPayType(), "第三方支付失败");
            paymentEventPublisher.publishEvent(event);
            return Result.ok("支付失败已记录");
        }

        return Result.ok("支付回调处理完成");
    }

    @Override
    public Result queryPaymentStatus(Long orderId) {
        PaymentRecord paymentRecord = paymentRecordMapper.getByOrderId(orderId);
        if (paymentRecord == null) {
            return Result.fail("支付记录不存在");
        }

        return Result.ok(paymentRecord);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result cancelPayment(Long orderId) {
        PaymentRecord paymentRecord = paymentRecordMapper.getByOrderId(orderId);
        if (paymentRecord == null) {
            return Result.fail("支付记录不存在");
        }

        // 只能取消待支付或支付中的记录
        if (paymentRecord.getStatus() != 1 && paymentRecord.getStatus() != 2) {
            return Result.fail("当前支付状态不能取消");
        }

        // 更新支付状态为取消
        paymentRecord.setStatus(5); // 已取消
        paymentRecord.setUpdateTime(LocalDateTime.now());
        updateById(paymentRecord);

        return Result.ok("取消支付成功");
    }

    @Override
    public PaymentRecord getByTransactionId(String transactionId) {
        return paymentRecordMapper.getByTransactionId(transactionId);
    }

    /**
     * 生成唯一支付流水号
     */
    private String generateTransactionId() {
        return "PAY" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 8);
    }
}