package com.travelshop.enums;

import lombok.Getter;

/**
 * 购物车操作类型枚举
 */
@Getter
public enum CartOperationType {

    ADD(1, "添加商品"),
    UPDATE_QUANTITY(2, "修改数量"),
    REMOVE(3, "删除商品"),
    CLEAR(4, "清空购物车"),
    UPDATE_SELECTED(5, "更新选中状态");

    private final Integer code;
    private final String desc;

    CartOperationType(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static CartOperationType getByCode(Integer code) {
        for (CartOperationType type : values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        return null;
    }
}