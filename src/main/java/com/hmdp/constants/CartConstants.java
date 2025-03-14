package com.hmdp.constants;

/**
 * 购物车系统常量类
 */
public class CartConstants {

    /**
     * 缓存相关常量
     */
    public static final String CACHE_CART_KEY_PREFIX = "cart:";
    public static final String CACHE_TEMP_CART_KEY_PREFIX = "temp_cart:";
    public static final long CACHE_CART_TTL_HOURS = 24 * 7; // 购物车缓存过期时间：7天
    public static final long CACHE_TEMP_CART_TTL_HOURS = 24 * 3; // 临时购物车过期时间：3天

    /**
     * 购物车状态常量
     */
    public static final int CART_STATUS_NORMAL = 1;  // 正常
    public static final int CART_STATUS_ORDERED = 2; // 已下单
    public static final int CART_STATUS_EXPIRED = 3; // 已过期

    /**
     * 商品类型常量
     */
    public static final int PRODUCT_TYPE_NORMAL = 1;  // 普通商品
    public static final int PRODUCT_TYPE_TICKET = 2;  // 门票
    public static final int PRODUCT_TYPE_VOUCHER = 3; // 优惠券

    /**
     * 购物车操作类型常量
     */
    public static final int OPERATION_ADD = 1;           // 添加商品
    public static final int OPERATION_UPDATE_QUANTITY = 2; // 修改数量
    public static final int OPERATION_REMOVE = 3;        // 删除商品
    public static final int OPERATION_CLEAR = 4;         // 清空购物车
    public static final int OPERATION_UPDATE_SELECTED = 5; // 更新选中状态

    /**
     * 选中状态常量
     */
    public static final int SELECTED_FALSE = 0; // 未选中
    public static final int SELECTED_TRUE = 1;  // 已选中

    /**
     * 购物车最大容量限制
     */
    public static final int CART_MAX_ITEMS = 99;

    /**
     * 促销类型常量
     */
    public static final int PROMOTION_TYPE_FULL_REDUCTION = 1; // 满减
    public static final int PROMOTION_TYPE_FULL_DISCOUNT = 2;  // 满折
    public static final int PROMOTION_TYPE_DIRECT_REDUCTION = 3; // 直减

    /**
     * 购物车合并策略
     */
    public static final int MERGE_STRATEGY_ADD = 1;      // 数量累加
    public static final int MERGE_STRATEGY_TEMP = 2;     // 使用临时购物车数据
    public static final int MERGE_STRATEGY_USER = 3;     // 使用用户购物车数据
    public static final int MERGE_STRATEGY_LARGER = 4;   // 取两者较大值

    /**
     * 购物车错误码
     */
    public static final String ERROR_CART_NOT_FOUND = "cart_not_found";      // 购物车不存在
    public static final String ERROR_ITEM_NOT_FOUND = "item_not_found";      // 购物车项不存在
    public static final String ERROR_ITEM_OUT_OF_STOCK = "item_out_of_stock"; // 商品库存不足
    public static final String ERROR_CART_FULL = "cart_full";                // 购物车已满

    /**
     * 业务参数
     */
    public static final int DEFAULT_CART_ITEM_QUANTITY = 1; // 默认加入购物车的数量
    public static final int CART_ITEM_MAX_QUANTITY = 999;   // 单个商品最大数量限制
}