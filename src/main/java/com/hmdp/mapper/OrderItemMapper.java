package com.hmdp.service;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hmdp.entity.OrderItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 订单商品Mapper接口
 */
@Mapper
public interface OrderItemMapper extends BaseMapper<OrderItem> {

    /**
     * 根据订单ID查询订单商品
     */
    @Select("SELECT * FROM tb_order_item WHERE order_id = #{orderId}")
    List<OrderItem> selectByOrderId(@Param("orderId") Long orderId);

    /**
     * 根据商品ID统计订单数量
     */
    @Select("SELECT COUNT(*) FROM tb_order_item WHERE product_id = #{productId}")
    Integer countByProductId(@Param("productId") Long productId);

    /**
     * 根据订单ID和商品ID查询订单商品
     */
    @Select("SELECT * FROM tb_order_item WHERE order_id = #{orderId} AND product_id = #{productId}")
    List<OrderItem> selectByOrderIdAndProductId(@Param("orderId") Long orderId, @Param("productId") Long productId);

    /**
     * 根据产品类型统计销售数量
     */
    @Select("SELECT SUM(count) FROM tb_order_item WHERE product_type = #{productType}")
    Integer sumCountByProductType(@Param("productType") Integer productType);


}