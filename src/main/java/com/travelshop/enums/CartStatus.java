package com.travelshop.enums;

import lombok.Getter;

/**
 * 购物车状态枚举
 */
@Getter
public enum CartStatus {

    NORMAL(1, "正常"),
    ORDERED(2, "已下单"),
    EXPIRED(3, "已过期");

    private final Integer code;
    private final String desc;

    CartStatus(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static CartStatus getByCode(Integer code) {
        for (CartStatus status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        return null;
    }
}