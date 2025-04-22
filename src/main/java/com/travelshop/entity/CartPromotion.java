package com.travelshop.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 购物车促销关联实体类
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("tb_cart_promotion")
public class CartPromotion implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 关联ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 购物车ID
     */
    private Long cartId;

    /**
     * 促销活动ID
     */
    private Long promotionId;

    /**
     * 促销类型：1-满减 2-满折 3-直减
     */
    private Integer promotionType;

    /**
     * 优惠金额
     */
    private BigDecimal discountAmount;

    /**
     * 创建时间
     */
    private LocalDateTime createdTime;
}