package com.hmdp.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.OrderCreateDTO;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.SeckillVoucher;
import com.hmdp.entity.Voucher;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.enums.ProductType;
import com.hmdp.mapper.VoucherOrderMapper;
import com.hmdp.service.IOrderService;
import com.hmdp.service.ISeckillVoucherService;
import com.hmdp.service.IVoucherOrderService;
import com.hmdp.service.IVoucherService;
import com.hmdp.utils.RedisIdWorker;
import com.hmdp.utils.UserHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {
    private final ISeckillVoucherService seckillVoucherService;
    private final IVoucherService voucherService;
    private final RedisIdWorker redisIdWorker;
    private final IOrderService orderService;

    @Override
    public Result seckillVoucher(Long voucherId) {
        // 获取用户
        UserDTO user = UserHolder.getUser();

        // 检查库存
        SeckillVoucher sv = seckillVoucherService.getById(voucherId);
        if (sv == null || sv.getStock() <= 0) {
            return Result.fail("库存不足");
        }

        // 检查是否已购买
        Long count = query().eq("voucher_id", voucherId)
                .eq("user_id", user.getId())
                .count();
        if (count > 0) {
            return Result.fail("禁止重复购买");
        }

        // 获取优惠券信息
        Voucher voucher = voucherService.getById(voucherId);
        if (voucher == null) {
            return Result.fail("优惠券不存在");
        }

        // 创建标准订单
        OrderCreateDTO createDTO = new OrderCreateDTO();
        createDTO.setUserId(user.getId());

        // 创建订单项
        OrderCreateDTO.OrderItemDTO itemDTO = new OrderCreateDTO.OrderItemDTO();
        itemDTO.setProductId(voucherId);
        itemDTO.setProductType(ProductType.VOUCHER.getCode()); // 标记为优惠券类型
        itemDTO.setSkuId(voucherId); // 优惠券没有SKU，用券ID代替
        itemDTO.setCount(1);
        itemDTO.setPrice(BigDecimal.valueOf(voucher.getPayValue()));

        createDTO.setOrderItems(Collections.singletonList(itemDTO));
        createDTO.setPayType(1); // 默认支付方式
        createDTO.setSource(3); // 来源（如小程序）

        // 创建订单
        Long orderId = orderService.createOrder(createDTO);
        return Result.ok(orderId);
    }

    @Override
    public VoucherOrder getByOrderItemId(Long orderItemId) {
        return baseMapper.getByOrderItemId(orderItemId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean useVoucher(Long id) {
        return baseMapper.updateStatusToUsed(id, LocalDateTime.now()) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean refundVoucher(Long id) {
        VoucherOrder voucherOrder = getById(id);
        if (voucherOrder == null || voucherOrder.getStatus() != 1) {
            return false;
        }

        // 退回优惠券库存
        seckillVoucherService.update()
                .setSql("stock = stock + 1")
                .eq("voucher_id", voucherOrder.getVoucherId())
                .update();

        // 更新状态
        return baseMapper.updateStatusToRefunded(id, LocalDateTime.now()) > 0;
    }
    @Override
    public List<VoucherOrder> getUserVouchers(Long userId) {
        // 调用Mapper层的方法获取用户的优惠券订单列表
        return baseMapper.getUserVouchers(userId);
    }
}