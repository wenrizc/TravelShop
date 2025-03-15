package com.hmdp.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hmdp.dto.Result;
import com.hmdp.entity.Voucher;

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
