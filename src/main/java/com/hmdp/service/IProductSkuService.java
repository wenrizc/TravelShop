package com.hmdp.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hmdp.entity.ProductSku;

/**
 * 商品规格服务接口
 */
public interface IProductSkuService extends IService<ProductSku> {

    /**
     * 锁定商品库存
     * @param skuId 规格ID
     * @param count 锁定数量
     * @return 是否成功
     */
    boolean lockStock(Long skuId, Integer count);

    /**
     * 解锁商品库存
     * @param skuId 规格ID
     * @param count 解锁数量
     * @return 是否成功
     */
    boolean unlockStock(Long skuId, Integer count);

    /**
     * 确认扣减库存
     * @param skuId 规格ID
     * @param count 扣减数量
     * @return 是否成功
     */
    boolean confirmStock(Long skuId, Integer count);
}