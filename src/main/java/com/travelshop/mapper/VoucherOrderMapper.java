package com.travelshop.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.travelshop.entity.VoucherOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 优惠券订单 Mapper 接口
 */
@Mapper
public interface VoucherOrderMapper extends BaseMapper<VoucherOrder> {

    /**
     * 根据订单项ID查询优惠券订单
     */
    @Select("SELECT * FROM tb_voucher_order WHERE order_item_id = #{orderItemId}")
    VoucherOrder getByOrderItemId(@Param("orderItemId") Long orderItemId);

    /**
     * 更新优惠券状态为已使用
     */
    @Update("UPDATE tb_voucher_order SET status = 2, use_time = #{useTime}, update_time = #{useTime} " +
            "WHERE id = #{id} AND status = 1")
    int updateStatusToUsed(@Param("id") Long id, @Param("useTime") LocalDateTime useTime);

    /**
     * 更新优惠券状态为已退款
     */
    @Update("UPDATE tb_voucher_order SET status = 4, refund_time = #{refundTime}, update_time = #{refundTime} " +
            "WHERE id = #{id} AND status = 1")
    int updateStatusToRefunded(@Param("id") Long id, @Param("refundTime") LocalDateTime refundTime);

    /**
     * 更新优惠券状态为已支付
     */
    @Update("UPDATE tb_voucher_order SET status = 2, pay_time = #{payTime}, update_time = #{payTime} " +
            "WHERE order_id = #{orderId}")
    int updateStatusToPaid(@Param("orderId") Long orderId, @Param("payTime") LocalDateTime payTime);

    /**
     * 获取用户的优惠券订单列表
     */
    @Select("SELECT * FROM tb_voucher_order WHERE user_id = #{userId} ORDER BY create_time DESC")
    List<VoucherOrder> getUserVouchers(@Param("userId") Long userId);
}