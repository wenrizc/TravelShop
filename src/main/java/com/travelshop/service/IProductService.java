package com.travelshop.service;


import com.travelshop.entity.Product;

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
     * 增加商品销量
     * @param productId 商品ID
     * @param count 增加数量
     * @return 是否成功
     */
    boolean increaseSales(Long productId, Integer count);

    /**
     * 减少商品销量
     * @param productId 商品ID
     * @param count 减少数量
     * @return 是否成功
     */
    boolean decreaseSales(Long productId, Integer count);

    /**
     * 获取商品信息
     * @param productId 商品ID
     * @return 商品信息
     */
    Product getById(Long productId);
}