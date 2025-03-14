package com.hmdp.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hmdp.entity.ProductSku;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;
import java.util.List;

/**
 * 商品规格数据访问接口
 */
@Mapper
public interface ProductSkuMapper extends BaseMapper<ProductSku> {

    /**
     * 获取商品默认规格
     * @param productId 商品ID
     * @return 默认规格
     */
    @Select("SELECT * FROM tb_product_sku WHERE product_id = #{productId} AND status = 1 ORDER BY id ASC LIMIT 1")
    ProductSku getDefaultSku(@Param("productId") Long productId);

    /**
     * 查询商品所有规格
     * @param productId 商品ID
     * @return 规格列表
     */
    @Select("SELECT * FROM tb_product_sku WHERE product_id = #{productId} AND status = 1 ORDER BY sort ASC")
    List<ProductSku> listByProductId(@Param("productId") Long productId);

    /**
     * 增加锁定库存
     * @param skuId SKU ID
     * @param quantity 数量
     * @return 影响行数
     */
    @Update("UPDATE tb_product_sku SET stock_locked = stock_locked + #{quantity}, update_time = NOW() " +
            "WHERE id = #{skuId} AND stock - stock_locked >= #{quantity}")
    int incrementLockedStock(@Param("skuId") Long skuId, @Param("quantity") Integer quantity);

    /**
     * 减少锁定库存
     * @param skuId SKU ID
     * @param quantity 数量
     * @return 影响行数
     */
    @Update("UPDATE tb_product_sku SET stock_locked = GREATEST(0, stock_locked - #{quantity}), update_time = NOW() " +
            "WHERE id = #{skuId}")
    int decrementLockedStock(@Param("skuId") Long skuId, @Param("quantity") Integer quantity);

    /**
     * 减少实际库存和锁定库存（订单支付完成后）
     * @param skuId SKU ID
     * @param quantity 数量
     * @return 影响行数
     */
    @Update("UPDATE tb_product_sku SET stock = stock - #{quantity}, " +
            "stock_locked = stock_locked - #{quantity}, " +
            "sales = sales + #{quantity}, " +
            "update_time = NOW() " +
            "WHERE id = #{skuId} AND stock_locked >= #{quantity}")
    int decrementStock(@Param("skuId") Long skuId, @Param("quantity") Integer quantity);

    /**
     * 查询商品最低价格
     * @param productId 商品ID
     * @return 最低价格
     */
    @Select("SELECT MIN(price) FROM tb_product_sku WHERE product_id = #{productId} AND status = 1")
    BigDecimal selectMinPrice(@Param("productId") Long productId);

    /**
     * 更新SKU状态
     */
    @Update("UPDATE tb_product_sku SET status = #{status}, update_time = NOW() WHERE id = #{id}")
    int updateStatus(@Param("id") Long id, @Param("status") Integer status);

    /**
     * 批量更新SKU状态
     */
    @Update("UPDATE tb_product_sku SET status = #{status}, update_time = NOW() WHERE product_id = #{productId}")
    int batchUpdateStatusByProductId(@Param("productId") Long productId, @Param("status") Integer status);

    /**
     * 查询指定商品可用库存总和
     */
    @Select("SELECT SUM(stock - stock_locked) FROM tb_product_sku WHERE product_id = #{productId} AND status = 1")
    Integer sumAvailableStockByProductId(@Param("productId") Long productId);

    /**
     * 根据SKU ID查询商品ID
     */
    @Select("SELECT product_id FROM tb_product_sku WHERE id = #{skuId}")
    Long getProductIdBySkuId(@Param("skuId") Long skuId);
}