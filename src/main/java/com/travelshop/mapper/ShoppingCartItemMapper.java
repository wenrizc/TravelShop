package com.travelshop.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.travelshop.entity.ShoppingCartItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Map;

/**
 * 购物车项数据访问接口
 */
@Mapper
public interface ShoppingCartItemMapper extends BaseMapper<ShoppingCartItem> {

    /**
     * 根据购物车ID查询所有商品
     * @param cartId 购物车ID
     * @return 购物车项列表
     */
    @Select("SELECT * FROM tb_shopping_cart_item WHERE cart_id = #{cartId}")
    List<ShoppingCartItem> listByCartId(@Param("cartId") Long cartId);

    /**
     * 查询选中的购物车项
     * @param cartId 购物车ID
     * @return 选中的购物车项列表
     */
    @Select("SELECT * FROM tb_shopping_cart_item WHERE cart_id = #{cartId} AND selected = 1")
    List<ShoppingCartItem> listSelectedByCartId(@Param("cartId") Long cartId);

    /**
     * 根据商品ID和SKU ID查询购物车项
     * @param cartId 购物车ID
     * @param productId 商品ID
     * @param productType 商品类型
     * @param skuId SKU ID
     * @return 购物车项
     */
    @Select("SELECT * FROM tb_shopping_cart_item WHERE cart_id = #{cartId} AND product_id = #{productId} " +
            "AND product_type = #{productType} AND (#{skuId} IS NULL OR sku_id = #{skuId}) LIMIT 1")
    ShoppingCartItem getByProductInfo(@Param("cartId") Long cartId,
                                      @Param("productId") Long productId,
                                      @Param("productType") Integer productType,
                                      @Param("skuId") Long skuId);

    /**
     * 更新购物车项数量
     * @param id 购物车项ID
     * @param quantity 数量
     * @return 影响行数
     */
    @Update("UPDATE tb_shopping_cart_item SET quantity = #{quantity}, updated_time = NOW() WHERE id = #{id}")
    int updateQuantity(@Param("id") Long id, @Param("quantity") Integer quantity);

    /**
     * 更新购物车项选中状态
     * @param id 购物车项ID
     * @param selected 选中状态：0-未选中 1-已选中
     * @return 影响行数
     */
    @Update("UPDATE tb_shopping_cart_item SET selected = #{selected}, updated_time = NOW() WHERE id = #{id}")
    int updateSelected(@Param("id") Long id, @Param("selected") Integer selected);

    /**
     * 批量更新选中状态
     * @param cartId 购物车ID
     * @param selected 选中状态
     * @return 影响行数
     */
    @Update("UPDATE tb_shopping_cart_item SET selected = #{selected}, updated_time = NOW() WHERE cart_id = #{cartId}")
    int updateAllSelected(@Param("cartId") Long cartId, @Param("selected") Integer selected);

    /**
     * 查询购物车中商品总数
     * @param cartId 购物车ID
     * @return 商品总数
     */
    @Select("SELECT IFNULL(SUM(quantity), 0) FROM tb_shopping_cart_item WHERE cart_id = #{cartId}")
    int countItems(@Param("cartId") Long cartId);

    /**
     * 按商店分组查询购物车项
     * @param cartId 购物车ID
     * @return 按商店分组的购物车项列表
     */
    @Select("SELECT i.*, p.shop_id FROM tb_shopping_cart_item i " +
            "LEFT JOIN tb_product p ON i.product_id = p.id AND i.product_type = 1 " +
            "WHERE i.cart_id = #{cartId} AND i.product_type = 1 " +
            "UNION ALL " +
            "SELECT i.*, t.shop_id FROM tb_shopping_cart_item i " +
            "LEFT JOIN tb_ticket t ON i.product_id = t.id AND i.product_type = 2 " +
            "WHERE i.cart_id = #{cartId} AND i.product_type = 2 " +
            "UNION ALL " +
            "SELECT i.*, v.shop_id FROM tb_shopping_cart_item i " +
            "LEFT JOIN tb_voucher v ON i.product_id = v.id AND i.product_type = 3 " +
            "WHERE i.cart_id = #{cartId} AND i.product_type = 3")
    List<Map<String, Object>> listItemsByShopId(@Param("cartId") Long cartId);

    /**
     * 根据商品类型查询购物车项
     * @param cartId 购物车ID
     * @param productType 商品类型
     * @return 购物车项列表
     */
    @Select("SELECT * FROM tb_shopping_cart_item WHERE cart_id = #{cartId} AND product_type = #{productType}")
    List<ShoppingCartItem> listByProductType(@Param("cartId") Long cartId, @Param("productType") Integer productType);
}