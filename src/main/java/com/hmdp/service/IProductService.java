package com.hmdp.service;


import java.util.Map;

public interface IProductService {

    /**
     * 获取商品信息
     * @param productId 商品ID
     * @param skuId SKU ID
     * @return 商品和SKU信息
     */
    Map<String, Object> getProductInfo(Long productId, Long skuId);

}