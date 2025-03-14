package com.hmdp.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hmdp.entity.ShoppingCartItem;
import com.hmdp.dto.Result;

import java.util.List;
import java.util.Map;

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
     * 添加商品到购物车
     * @param item 购物车项
     * @return 是否成功
     */
    boolean addItem(ShoppingCartItem item);

    /**
     * 更新购物车项数量
     * @param id 购物车项ID
     * @param quantity 新数量
     * @return 是否成功
     */
    boolean updateQuantity(Long id, Integer quantity);

    /**
     * 更新购物车项选中状态
     * @param id 购物车项ID
     * @param selected 选中状态
     * @return 是否成功
     */
    boolean updateSelected(Long id, Boolean selected);

    /**
     * 更新购物车所有项的选中状态
     * @param cartId 购物车ID
     * @param selected 选中状态
     * @return 是否成功
     */
    boolean updateAllSelected(Long cartId, Boolean selected);

    /**
     * 删除购物车项
     * @param id 购物车项ID
     * @return 是否成功
     */
    boolean removeItem(Long id);

    /**
     * 批量删除购物车项
     * @param ids 购物车项ID列表
     * @return 是否成功
     */
    boolean removeItems(List<Long> ids);

    /**
     * 清空购物车所有项
     * @param cartId 购物车ID
     * @return 是否成功
     */
    boolean clearItems(Long cartId);

    /**
     * 按商铺ID分组查询购物车项
     * @param cartId 购物车ID
     * @return 按商铺分组的购物车项
     */
    Map<Long, List<ShoppingCartItem>> groupByShopId(Long cartId);

    /**
     * 根据商品类型筛选购物车项
     * @param cartId 购物车ID
     * @param productType 商品类型
     * @return 购物车项列表
     */
    List<ShoppingCartItem> filterByProductType(Long cartId, Integer productType);

    /**
     * 验证购物车商品有效性（库存、上架状态等）
     * @param cartId 购物车ID
     * @return 无效的购物车项信息
     */
    List<ShoppingCartItem> validateCartItems(Long cartId);

    /**
     * 计算购物车中所有已选中商品的总价
     * @param cartId 购物车ID
     * @return 总价
     */
    java.math.BigDecimal calculateSelectedItemsTotal(Long cartId);

    Integer countItems(Long id);
}