package com.hmdp.service;

import com.hmdp.entity.Product;
import com.hmdp.entity.ProductSku;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface IProductService {

    /**
     * 获取商品信息
     * @param productId 商品ID
     * @param skuId SKU ID
     * @return 商品和SKU信息
     */
    Map<String, Object> getProductInfo(Long productId, Long skuId);

    /**
     * 锁定商品库存
     * @param productId 商品ID
     * @param skuId SKU ID
     * @param quantity 数量
     * @return 是否成功
     */
    boolean lockStock(Long productId, Long skuId, Integer quantity);

    /**
     * 释放商品库存
     * @param productId 商品ID
     * @param skuId SKU ID
     * @param quantity 数量
     * @return 是否成功
     */
    boolean unlockStock(Long productId, Long skuId, Integer quantity);

    /**
     * 扣减库存
     * @param productId 商品ID
     * @param skuId SKU ID
     * @param quantity 数量
     * @return 是否成功
     */
    boolean deductStock(Long productId, Long skuId, Integer quantity);

    /**
     * 获取商品所有规格
     * @param productId 商品ID
     * @return 规格列表
     */
    List<ProductSku> getProductSkus(Long productId);

    /**
     * 获取商品最低价格
     * @param productId 商品ID
     * @return 最低价格
     */
    BigDecimal getProductMinPrice(Long productId);
}