package com.hmdp.enums;

import com.hmdp.mapper.BlogMapper;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.mapper.VoucherMapper;
import org.springframework.context.ApplicationContext;

public enum BusinessType {

    SHOP("shop", ShopMapper.class, "商铺"),
    BLOG("blog", BlogMapper.class, "博客"),
    VOUCHER("voucher", VoucherMapper.class, "优惠券");

    public static final String CACHE_SHOP_KEY = "shop:";
    public static final String CACHE_BLOG_KEY = "blog:";
    public static final String CACHE_VOUCHER_KEY = "voucher:";

    private final String code;
    private final Class<?> mapperClass;
    private final String description;

    BusinessType(String code, Class<?> mapperClass, String description) {
        this.code = code;
        this.mapperClass = mapperClass;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getKeyPrefix() {
        switch (this) {
            case SHOP: return CACHE_SHOP_KEY;
            case BLOG: return CACHE_BLOG_KEY;
            case VOUCHER: return CACHE_VOUCHER_KEY;
            default: return "";
        }
    }

    public Class<?> getMapperClass() {
        return mapperClass;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 根据code查找枚举实例
     */
    public static BusinessType getByCode(String code) {
        for (BusinessType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        return null;
    }

    /**
     * 根据key查找枚举实例
     */
    public static BusinessType getByKey(String key) {
        if (key.startsWith(CACHE_SHOP_KEY)) {
            return SHOP;
        } else if (key.startsWith(CACHE_BLOG_KEY)) {
            return BLOG;
        } else if (key.startsWith(CACHE_VOUCHER_KEY)) {
            return VOUCHER;
        }
        return null;
    }

    /**
     * 从ApplicationContext获取对应的Mapper实例
     */
    public Object getMapper(ApplicationContext context) {
        if (context == null) {
            return null;
        }
        return context.getBean(mapperClass);
    }

    /**
     * 根据ID构建缓存key
     */
    public String buildCacheKey(Object id) {
        return getKeyPrefix() + id;
    }
}