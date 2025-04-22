package com.travelshop.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.travelshop.dto.Result;
import com.travelshop.entity.Voucher;

/**
 * <p>
 *  服务类
 * </p>
 */
public interface IVoucherService extends IService<Voucher> {

    /**
     * 查询店铺的优惠券
     * @param shopId 店铺ID
     * @return 优惠券列表
     */
    Result queryVoucherOfShop(Long shopId);

    /**
     * 添加秒杀优惠券
     * @param voucher 优惠券
     */
    void addSeckillVoucher(Voucher voucher);
}
