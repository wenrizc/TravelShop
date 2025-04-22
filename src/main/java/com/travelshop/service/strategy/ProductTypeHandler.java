package com.travelshop.service.strategy;

import com.travelshop.dto.OrderCreateDTO;
import com.travelshop.entity.Order;
import com.travelshop.entity.OrderItem;
import com.travelshop.enums.ProductType;

/**
 * 商品类型处理策略接口
 * 定义不同商品类型的处理行为
 */
public interface ProductTypeHandler {

    /**
     * 获取处理的商品类型
     * @return 商品类型枚举
     */
    ProductType getProductType();

    /**
     * 验证商品有效性
     * @param itemDTO 订单项DTO
     */
    void validateProduct(OrderCreateDTO.OrderItemDTO itemDTO);

    /**
     * 设置订单项信息
     * @param item 订单项
     * @param itemDTO 订单项DTO
     */
    void setupOrderItem(OrderItem item, OrderCreateDTO.OrderItemDTO itemDTO);

    /**
     * 订单创建后处理
     * @param order 订单
     * @param item 订单项
     */
    void processAfterOrderCreation(Order order, OrderItem item);

    /**
     * 订单支付后处理
     * @param order 订单
     * @param item 订单项
     */
    void processAfterPayment(Order order, OrderItem item);

    /**
     * 订单取消后处理
     * @param order 订单
     * @param item 订单项
     */
    void processAfterCancellation(Order order, OrderItem item);
}