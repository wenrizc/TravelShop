package com.hmdp.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * 添加购物车请求
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CartAddRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 商品ID
     */
    @NotNull(message = "商品ID不能为空")
    private Long productId;

    /**
     * 商品类型：1-普通商品 2-门票 3-优惠券
     */
    @NotNull(message = "商品类型不能为空")
    private Integer productType;

    /**
     * 数量
     */
    @NotNull(message = "商品数量不能为空")
    @Min(value = 1, message = "商品数量必须大于0")
    private Integer quantity;

    /**
     * 商品规格ID
     */
    private Long skuId;

    /**
     * 会话ID（未登录用户）
     */
    private String sessionId;

    /**
     * 是否立即购买
     * true表示不加入购物车，直接进入结算页面
     */
    private Boolean buyNow = false;
}