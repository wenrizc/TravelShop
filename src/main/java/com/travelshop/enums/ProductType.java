package com.travelshop.enums;

import lombok.Getter;

@Getter
public enum ProductType {
    NORMAL(1, "普通商品"),
    TICKET(2, "门票类商品"),
    VOUCHER(3, "优惠券商品");

    private final Integer code;
    private final String desc;

    ProductType(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static ProductType getByCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (ProductType type : ProductType.values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        return null;
    }
}