package com.hmdp.service.strategy;

import com.hmdp.enums.ProductType;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 商品类型处理策略工厂
 */
@Component
public class ProductTypeHandlerFactory {

    private final Map<Integer, ProductTypeHandler> handlerMap = new HashMap<>();

    public ProductTypeHandlerFactory(List<ProductTypeHandler> handlers) {
        // 将所有策略实现注册到Map中，以商品类型代码为key
        for (ProductTypeHandler handler : handlers) {
            handlerMap.put(handler.getProductType().getCode(), handler);
        }
    }

    /**
     * 获取商品类型对应的处理策略
     * @param productType 商品类型代码
     * @return 商品类型处理策略
     */
    public ProductTypeHandler getHandler(Integer productType) {
        ProductTypeHandler handler = handlerMap.get(productType);
        if (handler == null) {
            throw new IllegalArgumentException("未知商品类型: " + productType);
        }
        return handler;
    }
}