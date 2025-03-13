package com.hmdp.enums;

import lombok.Getter;

@Getter
public enum OrderStatus {
    WAIT_PAY(10, "待付款"),
    PAID(20, "已付款/待发货"),
    DELIVERED(30, "已发货/配送中"),
    RECEIVED(40, "已签收/待评价"),
    COMPLETED(50, "已完成"),
    CANCELLED(60, "已取消"),
    REFUND_APPLY(70, "申请退款中"),
    REFUNDED(80, "退款成功"),
    CLOSED(90, "交易关闭"),
    WAIT_DELIVER(25, "待发货");

    private final Integer code;
    private final String desc;

    OrderStatus(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    /**
     * 根据状态码获取枚举
     */
    public static OrderStatus getByCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (OrderStatus status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        return null;
    }
}
