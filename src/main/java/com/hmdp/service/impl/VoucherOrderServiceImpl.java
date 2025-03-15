package com.hmdp.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.mapper.VoucherOrderMapper;
import com.hmdp.service.ISeckillVoucherService;
import com.hmdp.service.IVoucherOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.time.LocalDateTime;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {
    private final ISeckillVoucherService seckillVoucherService;


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
        return baseMapper.getUserVouchers(userId);
    }
}