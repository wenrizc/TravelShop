package com.travelshop.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.travelshop.entity.ShoppingCartLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 购物车操作日志数据访问接口
 */
@Mapper
public interface ShoppingCartLogMapper extends BaseMapper<ShoppingCartLog> {

    /**
     * 查询用户购物车操作历史
     * @param userId 用户ID
     * @param limit 限制条数
     * @return 操作日志列表
     */
    @Select("SELECT * FROM tb_shopping_cart_log WHERE user_id = #{userId} ORDER BY created_time DESC LIMIT #{limit}")
    List<ShoppingCartLog> getUserRecentLogs(@Param("userId") Long userId, @Param("limit") Integer limit);

    /**
     * 查询购物车的操作日志
     * @param cartId 购物车ID
     * @return 操作日志列表
     */
    @Select("SELECT * FROM tb_shopping_cart_log WHERE cart_id = #{cartId} ORDER BY created_time DESC")
    List<ShoppingCartLog> getCartLogs(@Param("cartId") Long cartId);

    /**
     * 查询特定商品的操作日志
     * @param cartId 购物车ID
     * @param productId 商品ID
     * @return 操作日志列表
     */
    @Select("SELECT * FROM tb_shopping_cart_log WHERE cart_id = #{cartId} AND product_id = #{productId} ORDER BY created_time DESC")
    List<ShoppingCartLog> getProductLogs(@Param("cartId") Long cartId, @Param("productId") Long productId);

    /**
     * 查询特定操作类型的日志
     * @param userId 用户ID
     * @param operationType 操作类型
     * @param limit 限制条数
     * @return 操作日志列表
     */
    @Select("SELECT * FROM tb_shopping_cart_log WHERE user_id = #{userId} AND operation_type = #{operationType} " +
            "ORDER BY created_time DESC LIMIT #{limit}")
    List<ShoppingCartLog> getLogsByOperationType(@Param("userId") Long userId,
                                                 @Param("operationType") Integer operationType,
                                                 @Param("limit") Integer limit);

    /**
     * 统计用户购物车操作次数
     * @param userId 用户ID
     * @return 操作次数
     */
    @Select("SELECT COUNT(*) FROM tb_shopping_cart_log WHERE user_id = #{userId}")
    int countUserOperations(@Param("userId") Long userId);

    /**
     * 查询最近一次操作日志
     * @param cartId 购物车ID
     * @return 操作日志
     */
    @Select("SELECT * FROM tb_shopping_cart_log WHERE cart_id = #{cartId} ORDER BY created_time DESC LIMIT 1")
    ShoppingCartLog getLatestLog(@Param("cartId") Long cartId);
}