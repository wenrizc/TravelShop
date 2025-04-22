package com.travelshop.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.travelshop.entity.ProductSku;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 商品规格数据访问接口
 */
@Mapper
public interface ProductSkuMapper extends BaseMapper<ProductSku> {

    /**
     * 锁定商品库存
     * @param skuId 规格ID
     * @param count 锁定数量
     * @return 是否成功
     */
    @Update("UPDATE tb_product_sku SET stock_locked = stock_locked + #{count}, " +
            "update_time = NOW() WHERE id = #{skuId} AND stock >= stock_locked + #{count}")
    boolean lockStock(@Param("skuId") Long skuId, @Param("count") Integer count);

    /**
     * 确认已锁定的库存（实际扣减）
     * @param skuId 规格ID
     * @param count 确认数量
     * @return 是否成功
     */
    @Update("UPDATE tb_product_sku SET stock = stock - #{count}, " +
            "stock_locked = stock_locked - #{count}, update_time = NOW() " +
            "WHERE id = #{skuId} AND stock_locked >= #{count}")
    boolean confirmLockedStock(@Param("skuId") Long skuId, @Param("count") Integer count);

    /**
     * 解锁商品库存
     * @param skuId 规格ID
     * @param count 解锁数量
     * @return 是否成功
     */
    @Update("UPDATE tb_product_sku SET stock_locked = stock_locked - #{count}, " +
            "update_time = NOW() WHERE id = #{skuId} AND stock_locked >= #{count}")
    boolean unlockStock(@Param("skuId") Long skuId, @Param("count") Integer count);
}