package com.hmdp.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 购物车项数据传输对象
 */
@Data
public class CartItemDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 购物车项ID
     */
    private Long id;

    /**
     * 商品ID
     */
    private Long productId;

    /**
     * 商品名称
     */
    private String productName;

    /**
     * 商品图片
     */
    private String productImage;

    /**
     * 商品类型：1-普通商品 2-门票 3-优惠券
     */
    private Integer productType;

    /**
     * 商品规格ID
     */
    private Long skuId;

    /**
     * 商品规格名称
     */
    private String skuName;

    /**
     * 单价
     */
    private BigDecimal price;

    /**
     * 数量
     */
    private Integer quantity;

    /**
     * 是否选中
     */
    private Boolean selected;

    /**
     * 小计金额（前端展示用）
     */
    private BigDecimal subtotal;

    /**
     * 商品所属店铺ID
     */
    private Long shopId;

    /**
     * 商品所属店铺名称
     */
    private String shopName;

    /**
     * 计算小计金额
     */
    public BigDecimal calculateSubtotal() {
        if (price != null && quantity != null) {
            return price.multiply(new BigDecimal(quantity));
        }
        return BigDecimal.ZERO;
    }
}