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
 * <p>
 * 订单状态变更历史表
 * </p>
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("tb_order_status_history")
public class OrderStatusHistory implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 订单ID
     */
    private Long orderId;

    /**
     * 订单状态
     */
    private Integer orderStatus;

    /**
     * 前一状态
     */
    private Integer previousStatus;

    /**
     * 当前状态
     */
    private Integer currentStatus;

    /**
     * 操作人
     */
    private String operator;

    /**
     * 操作人类型(1用户,2商家,3系统)
     */
    private Integer operatorType;

    /**
     * 变更原因
     */
    private String reason;

    /**
     * 备注
     */
    private String remark;

    /**
     * 操作时间
     */
    private LocalDateTime operateTime;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}