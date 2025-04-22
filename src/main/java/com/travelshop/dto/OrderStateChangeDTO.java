package com.travelshop.dto;

import com.travelshop.enums.OperatorType;
import lombok.Data;

import java.io.Serializable;

/**
 * 订单状态变更数据传输对象
 * 用于封装订单状态变更的相关参数
 */
@Data
public class OrderStateChangeDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 订单ID
     */
    private Long orderId;

    /**
     * 目标状态
     */
    private Integer targetStatus;

    /**
     * 操作人
     */
    private String operator;

    /**
     * 操作人类型
     */
    private OperatorType operatorType;

    /**
     * 状态变更原因
     */
    private String reason;

    /**
     * 额外备注
     */
    private String remark;

    /**
     * 构造一个订单状态变更DTO
     */
    public static OrderStateChangeDTO builder() {
        return new OrderStateChangeDTO();
    }

    /**
     * 设置订单ID并返回实例
     */
    public OrderStateChangeDTO orderId(Long orderId) {
        this.orderId = orderId;
        return this;
    }

    /**
     * 设置目标状态并返回实例
     */
    public OrderStateChangeDTO targetStatus(Integer targetStatus) {
        this.targetStatus = targetStatus;
        return this;
    }

    /**
     * 设置操作人并返回实例
     */
    public OrderStateChangeDTO operator(String operator) {
        this.operator = operator;
        return this;
    }

    /**
     * 设置操作人类型并返回实例
     */
    public OrderStateChangeDTO operatorType(OperatorType operatorType) {
        this.operatorType = operatorType;
        return this;
    }

    /**
     * 设置状态变更原因并返回实例
     */
    public OrderStateChangeDTO reason(String reason) {
        this.reason = reason;
        return this;
    }

    /**
     * 设置额外备注并返回实例
     */
    public OrderStateChangeDTO remark(String remark) {
        this.remark = remark;
        return this;
    }
}