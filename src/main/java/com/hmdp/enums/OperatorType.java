package com.hmdp.enums;

import lombok.Getter;

@Getter
public enum OperatorType {
    USER(1, "用户"),
    MERCHANT(2, "商家"),
    SYSTEM(3, "系统"),
    ADMIN(4, "管理员");

    private final Integer code;
    private final String desc;

    OperatorType(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
