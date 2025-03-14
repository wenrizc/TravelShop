package com.hmdp.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hmdp.entity.ShoppingCart;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 购物车数据访问接口
 */
@Mapper
public interface ShoppingCartMapper extends BaseMapper<ShoppingCart> {

    /**
     * 查询用户当前有效购物车
     * @param userId 用户ID
     * @return 购物车对象
     */
    @Select("SELECT * FROM tb_shopping_cart WHERE user_id = #{userId} AND status = 1 ORDER BY created_time DESC LIMIT 1")
    ShoppingCart getActiveCartByUserId(@Param("userId") Long userId);

    /**
     * 创建购物车
     * 注意：通常使用MyBatis-Plus的insert方法，此处仅作为示例
     */
    // BaseMapper的insert方法已满足需求

    /**
     * 更新购物车状态
     * @param id 购物车ID
     * @param status 状态值
     * @return 影响行数
     */
    @Update("UPDATE tb_shopping_cart SET status = #{status}, updated_time = NOW() WHERE id = #{id}")
    int updateStatus(@Param("id") Long id, @Param("status") Integer status);

    /**
     * 更新购物车为已下单状态
     * @param id 购物车ID
     * @return 影响行数
     */
    @Update("UPDATE tb_shopping_cart SET status = 2, updated_time = NOW() WHERE id = #{id} AND status = 1")
    int markAsOrdered(@Param("id") Long id);

    /**
     * 更新购物车为已过期状态
     * @param id 购物车ID
     * @return 影响行数
     */
    @Update("UPDATE tb_shopping_cart SET status = 3, updated_time = NOW() WHERE id = #{id}")
    int markAsExpired(@Param("id") Long id);

    /**
     * 清理过期购物车（超过7天未更新且状态为正常）
     * @return 影响行数
     */
    @Update("UPDATE tb_shopping_cart SET status = 3, updated_time = NOW() " +
            "WHERE status = 1 AND updated_time < DATE_SUB(NOW(), INTERVAL 7 DAY)")
    int cleanExpiredCarts();
}