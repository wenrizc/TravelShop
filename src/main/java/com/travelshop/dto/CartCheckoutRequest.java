package com.travelshop.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.List;

/**
 * 购物车结算请求
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CartCheckoutRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 购物车ID
     */
    @NotNull(message = "购物车ID不能为空")
    private Long cartId;

    /**
     * 要结算的购物车项ID列表
     * 为空时结算所有选中的项
     */
    private List<Long> itemIds;

    /**
     * 收货地址ID
     */
    @NotNull(message = "收货地址不能为空")
    private Long addressId;

    /**
     * 支付方式：1-微信支付 2-支付宝支付 3-银联支付
     */
    @NotNull(message = "支付方式不能为空")
    private Integer payType;

    /**
     * 优惠券ID
     */
    private Long couponId;

    /**
     * 订单备注
     */
    private String remark;

    /**
     * 是否使用优惠活动
     */
    private Boolean usePromotion = true;
}