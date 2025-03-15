package com.hmdp.mapper;

import java.math.BigDecimal;
import java.util.List;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hmdp.entity.CartPromotion;

/**
 * 购物车促销关联数据访问接口
 */
@Mapper
public interface CartPromotionMapper extends BaseMapper<CartPromotion> {

    /**
     * 查询购物车关联的促销活动
     * @param cartId 购物车ID
     * @return 促销关联列表
     */
    @Select("SELECT * FROM tb_cart_promotion WHERE cart_id = #{cartId}")
    List<CartPromotion> listByCartId(@Param("cartId") Long cartId);

    /**
     * 查询购物车关联的特定类型促销活动
     * @param cartId 购物车ID
     * @param promotionType 促销类型
     * @return 促销关联列表
     */
    @Select("SELECT * FROM tb_cart_promotion WHERE cart_id = #{cartId} AND promotion_type = #{promotionType}")
    List<CartPromotion> listByType(@Param("cartId") Long cartId, @Param("promotionType") Integer promotionType);

    /**
     * 删除购物车促销关联
     * @param cartId 购物车ID
     * @param promotionId 促销活动ID
     * @return 影响行数
     */
    @Delete("DELETE FROM tb_cart_promotion WHERE cart_id = #{cartId} AND promotion_id = #{promotionId}")
    int deletePromotion(@Param("cartId") Long cartId, @Param("promotionId") Long promotionId);

    /**
     * 清空购物车所有促销关联
     * @param cartId 购物车ID
     * @return 影响行数
     */
    @Delete("DELETE FROM tb_cart_promotion WHERE cart_id = #{cartId}")
    int clearPromotions(@Param("cartId") Long cartId);

    /**
     * 计算购物车所有促销活动优惠总额
     * @param cartId 购物车ID
     * @return 优惠总额
     */
    @Select("SELECT IFNULL(SUM(discount_amount), 0) FROM tb_cart_promotion WHERE cart_id = #{cartId}")
    BigDecimal calculateTotalDiscount(@Param("cartId") Long cartId);

    /**
     * 检查购物车是否包含指定促销活动
     * @param cartId 购物车ID
     * @param promotionId 促销活动ID
     * @return 是否存在
     */
    @Select("SELECT COUNT(*) FROM tb_cart_promotion WHERE cart_id = #{cartId} AND promotion_id = #{promotionId}")
    int hasPromotion(@Param("cartId") Long cartId, @Param("promotionId") Long promotionId);


}