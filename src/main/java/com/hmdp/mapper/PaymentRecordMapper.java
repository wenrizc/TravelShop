package com.hmdp.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hmdp.entity.PaymentRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

@Mapper
public interface PaymentRecordMapper extends BaseMapper<PaymentRecord> {

    /**
     * 根据订单ID查询支付记录
     */
    @Select("SELECT * FROM tb_payment_record WHERE order_id = #{orderId} ORDER BY create_time DESC LIMIT 1")
    PaymentRecord getByOrderId(@Param("orderId") Long orderId);

    /**
     * 根据流水号查询支付记录
     */
    @Select("SELECT * FROM tb_payment_record WHERE transaction_id = #{transactionId}")
    PaymentRecord getByTransactionId(@Param("transactionId") String transactionId);

    /**
     * 更新支付状态
     */
    @Update("UPDATE tb_payment_record SET status = #{status}, pay_time = #{payTime}, " +
            "update_time = NOW(), callback_count = callback_count + 1, last_callback_time = NOW() " +
            "WHERE transaction_id = #{transactionId} AND (status = 1 OR status = 2)")
    int updatePaymentStatus(@Param("transactionId") String transactionId,
                            @Param("status") Integer status,
                            @Param("payTime") LocalDateTime payTime);
}