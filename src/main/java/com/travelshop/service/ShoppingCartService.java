package com.travelshop.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.travelshop.dto.Result;
import com.travelshop.entity.ShoppingCart;

import java.util.List;

/**
 * 购物车服务接口
 */
public interface ShoppingCartService extends IService<ShoppingCart> {

    /**
     * 获取用户当前购物车
     * @param userId 用户ID
     * @return 购物车对象
     */
    ShoppingCart getUserCart(Long userId);

    /**
     * 获取当前用户购物车，如果没有则创建新购物车
     * @return 购物车对象
     */
    ShoppingCart getOrCreateCurrentCart();

    /**
     * 添加商品到购物车
     * @param userId 用户ID
     * @param productId 商品ID
     * @param productType 商品类型：1-普通商品 2-门票 3-优惠券
     * @param quantity 数量
     * @param skuId SKU ID，可选
     * @return 结果
     */
    Result addItem(Long userId, Long productId, Integer productType, Integer quantity, Long skuId);

    /**
     * 更新购物车商品数量
     * @param userId 用户ID
     * @param itemId 购物车项ID
     * @param quantity 新数量
     * @return 结果
     */
    Result updateItemQuantity(Long userId, Long itemId, Integer quantity);

    /**
     * 从购物车中删除商品
     * @param userId 用户ID
     * @param itemId 购物车项ID
     * @return 结果
     */
    Result removeItem(Long userId, Long itemId);

    /**
     * 购物车结算
     * @param userId 用户ID
     * @param cartId 购物车ID
     * @param itemIds 要结算的商品ID列表，如果为空则结算所有选中商品
     * @return 订单ID
     */
    Result checkout(Long userId, Long cartId, List<Long> itemIds);

    /**
     * "立即购买"功能实现
     * @param userId 用户ID
     * @param productId 商品ID
     * @param productType 商品类型
     * @param quantity 数量
     * @param skuId SKU ID
     * @return 临时订单信息
     */
    Result buyNow(Long userId, Long productId, Integer productType, Integer quantity, Long skuId);

    /**
     * 选中购物车商品
     * @param userId 用户ID
     * @param itemIds 购物车项ID列表
     * @return 结果
     */
    Result selectItems(Long userId, List<Long> itemIds);
}