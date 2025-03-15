package com.hmdp.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hmdp.dto.Result;
import com.hmdp.entity.VoucherOrder;
import org.jetbrains.annotations.NotNull;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface IVoucherOrderService extends IService<VoucherOrder> {


    /**
     * 根据订单项ID查询优惠券订单
     * @param orderItemId 订单项ID
     * @return 优惠券订单
     */
    VoucherOrder getByOrderItemId(Long orderItemId);

    /**
     * 使用优惠券
     * @param id 优惠券订单ID
     * @return 是否成功
     */
    boolean useVoucher(Long id);

    /**
     * 退款优惠券
     * @param id 优惠券订单ID
     * @return 是否成功
     */
    boolean refundVoucher(Long id);

    List<VoucherOrder> getUserVouchers(Long id);
}