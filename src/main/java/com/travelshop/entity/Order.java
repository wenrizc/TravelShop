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
import java.util.List;

/**
 * <p>
 * 订单表
 * </p>
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("tb_order")
public class Order implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 订单ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 订单编号
     */
    private String orderNo;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 订单总金额
     */
    private BigDecimal totalAmount;

    /**
     * 优惠金额
     */
    private BigDecimal discountAmount;

    /**
     * 配送费
     */
    private BigDecimal deliveryFee;

    /**
     * 实付金额
     */
    private BigDecimal payAmount;

    /**
     * 订单状态
     */
    private Integer status;

    /**
     * 支付方式
     */
    private Integer payType;

    /**
     * 订单来源
     */
    private Integer source;

    /**
     * 订单备注
     */
    private String remark;

    /**
     * 收货地址ID
     */
    private Long addressId;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 支付时间
     */
    private LocalDateTime payTime;

    /**
     * 发货时间
     */
    private LocalDateTime deliveryTime;

    /**
     * 收货时间
     */
    private LocalDateTime receiveTime;

    /**
     * 完成时间
     */
    private LocalDateTime finishTime;

    /**
     * 关闭时间
     */
    private LocalDateTime closeTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    /**
     * 逻辑删除标记
     */
    private Integer isDeleted;

    private String cancelReason;

    private List<OrderItem> orderItems;

}