package com.travelshop.dto;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * 创建订单数据传输对象
 */
@Data
public class OrderCreateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 收货地址ID
     */
    private Long addressId;

    /**
     * 订单商品列表
     */
    private List<OrderItemDTO> orderItems;

    /**
     * 支付方式(1-支付宝 2-微信支付 3-银联)
     */
    private Integer payType;

    /**
     * 订单来源(1-APP 2-PC 3-小程序)
     */
    private Integer source;

    /**
     * 优惠券ID
     */
    private Long couponId;

    /**
     * 订单备注
     */
    private String remark;

    /**
     * 订单项DTO
     */
    @Data
    public static class OrderItemDTO implements Serializable {

        private static final long serialVersionUID = 1L;

        /**
         * 商品ID
         */
        private Long productId;

        /**
         * SKU ID
         */
        private Long skuId;

        /**
         * 购买数量
         */
        private Integer count;

        /**
         * 商品单价（用于前端展示，实际价格以后端为准）
         */
        private BigDecimal price;

        /**
         * 商品类型
         */
        private Integer productType;

    }

    /**
     * 构建方法
     */
    public static OrderCreateDTO builder() {
        return new OrderCreateDTO();
    }

    /**
     * 设置用户ID并返回实例
     */
    public OrderCreateDTO userId(Long userId) {
        this.userId = userId;
        return this;
    }

    /**
     * 设置收货地址ID并返回实例
     */
    public OrderCreateDTO addressId(Long addressId) {
        this.addressId = addressId;
        return this;
    }

    /**
     * 设置订单商品列表并返回实例
     */
    public OrderCreateDTO orderItems(List<OrderItemDTO> orderItems) {
        this.orderItems = orderItems;
        return this;
    }

    /**
     * 设置支付方式并返回实例
     */
    public OrderCreateDTO payType(Integer payType) {
        this.payType = payType;
        return this;
    }

    /**
     * 设置订单来源并返回实例
     */
    public OrderCreateDTO source(Integer source) {
        this.source = source;
        return this;
    }

    /**
     * 设置优惠券ID并返回实例
     */
    public OrderCreateDTO couponId(Long couponId) {
        this.couponId = couponId;
        return this;
    }

    /**
     * 设置订单备注并返回实例
     */
    public OrderCreateDTO remark(String remark) {
        this.remark = remark;
        return this;
    }

    /**
     * 参数校验
     * @return 是否有效
     */
    public boolean isValid() {
        if (userId == null || addressId == null) {
            return false;
        }

        if (orderItems == null || orderItems.isEmpty()) {
            return false;
        }

        for (OrderItemDTO item : orderItems) {
            if (item.getProductId() == null || item.getSkuId() == null ||
                    item.getCount() == null || item.getCount() <= 0) {
                return false;
            }
        }

        return true;
    }
}