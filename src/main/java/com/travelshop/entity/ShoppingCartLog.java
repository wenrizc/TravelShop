package com.travelshop.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 购物车操作日志实体类
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("tb_shopping_cart_log")
public class ShoppingCartLog implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 日志ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 购物车ID
     */
    private Long cartId;

    /**
     * 操作类型：1-添加 2-修改数量 3-删除 4-清空 5-选中/取消选中
     */
    private Integer operationType;

    /**
     * 商品ID
     */
    private Long productId;

    /**
     * 操作数量
     */
    private Integer quantity;

    /**
     * 创建时间
     */
    private LocalDateTime createdTime;
}