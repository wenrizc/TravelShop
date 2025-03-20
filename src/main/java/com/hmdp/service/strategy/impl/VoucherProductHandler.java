package com.hmdp.service.strategy.impl;

import com.hmdp.dto.OrderCreateDTO;
import com.hmdp.entity.*;
import com.hmdp.enums.ProductType;
import com.hmdp.mapper.VoucherOrderMapper;
import com.hmdp.service.ISeckillVoucherService;
import com.hmdp.service.IVoucherOrderService;
import com.hmdp.service.IVoucherService;
import com.hmdp.service.strategy.ProductTypeHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 优惠券商品处理策略实现
 */
@Component
@RequiredArgsConstructor
public class VoucherProductHandler implements ProductTypeHandler {

    private final IVoucherService voucherService;
    private final IVoucherOrderService voucherOrderService;
    private final ISeckillVoucherService seckillVoucherService;
    private final VoucherOrderMapper voucherOrderMapper;

    @Override
    public ProductType getProductType() {
        return ProductType.VOUCHER;
    }

    @Override
    public void validateProduct(OrderCreateDTO.OrderItemDTO itemDTO) {
        // 验证优惠券有效性
        Voucher voucher = voucherService.getById(itemDTO.getProductId());
        if (voucher == null) {
            throw new RuntimeException("优惠券不存在");
        }

        // 验证库存（秒杀券需要验证）
        if (voucher.getType() == 2) {
            SeckillVoucher seckillVoucher = seckillVoucherService.getById(itemDTO.getProductId());
            if (seckillVoucher == null || seckillVoucher.getStock() < itemDTO.getCount()) {
                throw new RuntimeException("优惠券库存不足");
            }

            // 验证秒杀时间
            LocalDateTime now = LocalDateTime.now();
            if (now.isBefore(seckillVoucher.getBeginTime())) {
                throw new RuntimeException("秒杀未开始");
            }
            if (now.isAfter(seckillVoucher.getEndTime())) {
                throw new RuntimeException("秒杀已结束");
            }
        }
    }

    @Override
    public void setupOrderItem(OrderItem item, OrderCreateDTO.OrderItemDTO itemDTO) {
        // 获取优惠券信息
        Voucher voucher = voucherService.getById(itemDTO.getProductId());

        // 设置优惠券商品信息
        item.setProductName(voucher.getTitle());
        item.setSkuName("标准券");
        item.setPrice(BigDecimal.valueOf(voucher.getPayValue()));
    }

    @Override
    public void processAfterOrderCreation(Order order, OrderItem item) {
        // 处理优惠券核销逻辑
        // 创建优惠券订单记录
        VoucherOrder voucherOrder = new VoucherOrder();
        voucherOrder.setUserId(order.getUserId());
        voucherOrder.setVoucherId(item.getProductId());
        voucherOrder.setPayType(order.getPayType());
        voucherOrder.setStatus(1); // 未使用状态
        voucherOrder.setCreateTime(LocalDateTime.now());
        voucherOrder.setUpdateTime(LocalDateTime.now());
        voucherOrderService.save(voucherOrder);

        // 如果是秒杀券，预扣库存
        Voucher voucher = voucherService.getById(item.getProductId());
        if (voucher.getType() == 2) {
            seckillVoucherService.update()
                    .setSql("stock = stock - " + item.getCount())
                    .eq("voucher_id", item.getProductId())
                    .gt("stock", 0)
                    .update();
        }
    }

    @Override
    public void processAfterPayment(Order order, OrderItem item) {
        // 更新优惠券订单状态
        voucherOrderMapper.updateStatusToPaid(item.getId(), LocalDateTime.now());
    }

    @Override
    public void processAfterCancellation(Order order, OrderItem item) {
        // 恢复优惠券库存
        Voucher voucher = voucherService.getById(item.getProductId());
        if (voucher.getType() == 2) {
            seckillVoucherService.update()
                    .setSql("stock = stock + " + item.getCount())
                    .eq("voucher_id", item.getProductId())
                    .update();
        }

        // 更新优惠券订单状态
        VoucherOrder voucherOrder = voucherOrderMapper.getByOrderItemId(item.getId());
        if (voucherOrder != null) {
            voucherOrderService.refundVoucher(voucherOrder.getId());
        }
    }
}