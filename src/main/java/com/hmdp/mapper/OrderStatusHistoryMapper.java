package com.hmdp.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hmdp.entity.OrderStatusHistory;
import com.hmdp.enums.OrderStatus;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Insert;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface OrderStatusHistoryMapper extends BaseMapper<OrderStatusHistory> {

    /**
     * 查询订单状态历史
     * @param orderId 订单ID
     * @return 订单状态历史列表
     */
    @Select("SELECT * FROM order_status_history WHERE order_id = #{orderId} ORDER BY operate_time DESC")
    List<OrderStatusHistory> findByOrderId(@Param("orderId") Long orderId);

    /**
     * 获取订单当前状态
     * @param orderId 订单ID
     * @return 当前状态编码
     */
    @Select("SELECT order_status FROM order_status_history WHERE order_id = #{orderId} ORDER BY operate_time DESC LIMIT 1")
    Integer getCurrentStatus(@Param("orderId") Long orderId);

    /**
     * 添加订单状态历史记录
     * @param orderId 订单ID
     * @param orderStatus 订单状态
     * @param operator 操作人
     * @param operatorType 操作人类型
     * @param remark 备注
     * @param operateTime 操作时间
     * @return 影响行数
     */
    @Insert("INSERT INTO order_status_history(order_id, order_status, operator, operator_type, remark, operate_time) " +
            "VALUES(#{orderId}, #{orderStatus}, #{operator}, #{operatorType}, #{remark}, #{operateTime})")
    int insertStatusHistory(@Param("orderId") Long orderId,
                            @Param("orderStatus") Integer orderStatus,
                            @Param("operator") String operator,
                            @Param("operatorType") Integer operatorType,
                            @Param("remark") String remark,
                            @Param("operateTime") LocalDateTime operateTime);

    /**
     * 查询指定状态的最近一条历史记录
     * @param orderId 订单ID
     * @param status 状态编码
     * @return 历史记录
     */
    @Select("SELECT * FROM order_status_history WHERE order_id = #{orderId} AND order_status = #{status} " +
            "ORDER BY operate_time DESC LIMIT 1")
    OrderStatusHistory findLastStatusRecord(@Param("orderId") Long orderId, @Param("status") Integer status);
}