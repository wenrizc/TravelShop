package com.hmdp.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 购物车项实体类
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("tb_shopping_cart_item")
public class ShoppingCartItem implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 购物车项ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 购物车ID
     */
    private Long cartId;

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
     * SKU ID
     */
    private Long skuId;

    /**
     * SKU名称
     */
    private String skuName;

    /**
     * 单价
     */
    private BigDecimal price;

    /**
     * 商品数量
     */
    private Integer quantity;

    /**
     * 是否选中：0-未选中 1-已选中
     */
    private Integer selected;

    /**
     * 创建时间
     */
    private LocalDateTime createdTime;

    /**
     * 更新时间
     */
    private LocalDateTime updatedTime;

    /**
     * 小计金额（非数据库字段）
     */
    @TableField(exist = false)
    private BigDecimal subtotal;

    /**
     * 获取小计金额
     */
    public BigDecimal getSubtotal() {
        if (this.price != null && this.quantity != null) {
            return this.price.multiply(new BigDecimal(this.quantity));
        }
        return BigDecimal.ZERO;
    }
}