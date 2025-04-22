package com.travelshop.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.travelshop.entity.TempCart;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 临时购物车数据访问接口
 */
@Mapper
public interface TempCartMapper extends BaseMapper<TempCart> {

    /**
     * 查询会话购物车项
     * @param sessionId 会话ID
     * @return 购物车项列表
     */
    @Select("SELECT * FROM tb_temp_cart WHERE session_id = #{sessionId} AND expire_time > NOW()")
    List<TempCart> getBySessionId(@Param("sessionId") String sessionId);

    /**
     * 根据商品ID和SKU ID查询临时购物车项
     * @param sessionId 会话ID
     * @param productId 商品ID
     * @param productType 商品类型
     * @param skuId SKU ID
     * @return 购物车项
     */
    @Select("SELECT * FROM tb_temp_cart WHERE session_id = #{sessionId} AND product_id = #{productId} " +
            "AND product_type = #{productType} AND (#{skuId} IS NULL OR sku_id = #{skuId}) AND expire_time > NOW() LIMIT 1")
    TempCart getByProductInfo(@Param("sessionId") String sessionId,
                              @Param("productId") Long productId,
                              @Param("productType") Integer productType,
                              @Param("skuId") Long skuId);

    /**
     * 更新临时购物车项数量
     * @param id 临时购物车项ID
     * @param quantity 数量
     * @return 影响行数
     */
    @Update("UPDATE tb_temp_cart SET quantity = #{quantity}, expire_time = DATE_ADD(NOW(), INTERVAL 3 DAY) WHERE id = #{id}")
    int updateQuantity(@Param("id") Long id, @Param("quantity") Integer quantity);

    /**
     * 删除临时购物车项
     * @param sessionId 会话ID
     * @param productId 商品ID
     * @param productType 商品类型
     * @param skuId SKU ID
     * @return 影响行数
     */
    @Delete("DELETE FROM tb_temp_cart WHERE session_id = #{sessionId} AND product_id = #{productId} " +
            "AND product_type = #{productType} AND (#{skuId} IS NULL OR sku_id = #{skuId})")
    int deleteByProductInfo(@Param("sessionId") String sessionId,
                            @Param("productId") Long productId,
                            @Param("productType") Integer productType,
                            @Param("skuId") Long skuId);

    /**
     * 清空临时购物车
     * @param sessionId 会话ID
     * @return 影响行数
     */
    @Delete("DELETE FROM tb_temp_cart WHERE session_id = #{sessionId}")
    int clearBySessionId(@Param("sessionId") String sessionId);

    /**
     * 清理过期购物车项
     * @return 影响行数
     */
    @Delete("DELETE FROM tb_temp_cart WHERE expire_time < NOW()")
    int cleanExpiredItems();

    /**
     * 将临时购物车项移动到正式购物车（用于合并操作）
     * 此方法通常在服务层实现，因为它涉及跨表操作
     */
}