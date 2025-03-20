package com.hmdp.service;

import com.hmdp.dto.Result;
import com.hmdp.entity.es.ProductDocument;
import org.springframework.data.domain.Page;

public interface ProductSearchService {
    // 通过关键词搜索商品
    Result searchProducts(String keyword, Long categoryId, Integer page, Integer size);

    // 将商品信息同步到ES
    void syncProductToES(Long productId);

    // 删除ES中商品数据
    void deleteProductFromES(Long productId);
}