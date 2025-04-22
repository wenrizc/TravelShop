package com.travelshop.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.travelshop.entity.ShoppingCartItem;

import java.util.List;

/**
 * 购物车项服务接口
 */
public interface ShoppingCartItemService extends IService<ShoppingCartItem> {

    /**
     * 根据购物车ID查询所有购物车项
     * @param cartId 购物车ID
     * @return 购物车项列表
     */
    List<ShoppingCartItem> listByCartId(Long cartId);

    /**
     * 查询购物车中已选中的购物车项
     * @param cartId 购物车ID
     * @return 已选中的购物车项列表
     */
    List<ShoppingCartItem> listSelectedByCartId(Long cartId);

    /**
     * 根据商品信息查询购物车项
     * @param cartId 购物车ID
     * @param productId 商品ID
     * @param productType 商品类型
     * @param skuId SKU ID
     * @return 购物车项
     */
    ShoppingCartItem getByProductInfo(Long cartId, Long productId, Integer productType, Long skuId);

    /**
     * 更新购物车项数量
     * @param id 购物车项ID
     * @param quantity 新数量
     * @return 是否成功
     */
    boolean updateQuantity(Long id, Integer quantity);

    /**
     * 批量删除购物车项
     * @param ids 购物车项ID列表
     * @return 是否成功
     */
    boolean removeItems(List<Long> ids);

    /**
     * 验证购物车商品有效性（库存、上架状态等）
     * @param cartId 购物车ID
     * @return 无效的购物车项信息
     */
    List<ShoppingCartItem> validateCartItems(Long cartId);

    /**
     * 更新购物车项的选中状态
     * @param cartId 购物车ID
     * @param itemIds 购物车项ID列表
     * @param selected 选中状态
     * @return 是否成功
     */
    boolean updateSelected(Long cartId, List<Long> itemIds, Boolean selected);

}