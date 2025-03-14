package com.hmdp.enums;

import lombok.Getter;

@Getter
public enum VoucherStatus {
    UNUSED(1, "未使用"),
    USED(2, "已使用"),
    EXPIRED(3, "已过期"),
    REFUNDED(4, "已退款");

    private final Integer code;
    private final String desc;

    VoucherStatus(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static VoucherStatus getByCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (VoucherStatus status : VoucherStatus.values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        return null;
    }
}