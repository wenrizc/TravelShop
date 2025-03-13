package com.hmdp.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hmdp.dto.OrderQueryDTO;
import com.hmdp.dto.OrderStatisticsDTO;
import com.hmdp.entity.Order;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单Mapper接口
 */
@Mapper
public interface OrderMapper extends BaseMapper<Order> {

    /**
     * 根据订单编号查询订单
     */
    @Select("SELECT * FROM tb_order WHERE order_no = #{orderNo} AND is_deleted = 0")
    Order selectByOrderNo(@Param("orderNo") String orderNo);

    /**
     * 根据用户ID和状态查询订单
     */
    @Select("SELECT * FROM tb_order WHERE user_id = #{userId} AND status = #{status} AND is_deleted = 0 ORDER BY create_time DESC")
    List<Order> selectByUserIdAndStatus(@Param("userId") Long userId, @Param("status") Integer status);

    /**
     * 批量更新订单状态（超时订单处理）
     */
    @Update("UPDATE tb_order SET status = #{targetStatus}, update_time = NOW(), close_time = NOW() " +
            "WHERE status = #{currentStatus} AND create_time < #{deadline} AND is_deleted = 0")
    int batchUpdateStatus(@Param("currentStatus") Integer currentStatus,
                          @Param("targetStatus") Integer targetStatus,
                          @Param("deadline") LocalDateTime deadline);

    /**
     * 统计用户各状态订单数量
     */
    @Select("SELECT status, COUNT(*) as count FROM tb_order " +
            "WHERE user_id = #{userId} AND is_deleted = 0 GROUP BY status")
    List<OrderStatisticsDTO> countOrdersByStatus(@Param("userId") Long userId);

    /**
     * 获取指定时间段内的订单数量
     */
    @Select("SELECT COUNT(*) FROM tb_order " +
            "WHERE create_time BETWEEN #{startTime} AND #{endTime} AND is_deleted = 0")
    int countOrdersByTimeRange(@Param("startTime") LocalDateTime startTime,
                               @Param("endTime") LocalDateTime endTime);

    /**
     * 获取指定时间段内的订单金额总和
     */
    @Select("SELECT IFNULL(SUM(pay_amount), 0) FROM tb_order " +
            "WHERE pay_time IS NOT NULL AND pay_time BETWEEN #{startTime} AND #{endTime} AND is_deleted = 0")
    BigDecimal sumOrderAmountByTimeRange(@Param("startTime") LocalDateTime startTime,
                                         @Param("endTime") LocalDateTime endTime);

    /**
     * 分页查询指定日期的订单
     */
    @Select("SELECT * FROM tb_order WHERE DATE(create_time) = DATE(#{date}) AND is_deleted = 0 ORDER BY create_time DESC")
    IPage<Order> selectOrdersByDate(Page<Order> page, @Param("date") LocalDateTime date);

    /**
     * 查询用户最近一次下单
     */
    @Select("SELECT * FROM tb_order WHERE user_id = #{userId} AND is_deleted = 0 ORDER BY create_time DESC LIMIT 1")
    Order selectLatestOrder(@Param("userId") Long userId);

    /**
     * 更新物流信息
     */
    @Update("UPDATE tb_order SET logistics_code = #{logisticsCode}, update_time = NOW() WHERE id = #{orderId}")
    int updateLogistics(@Param("orderId") Long orderId, @Param("logisticsCode") String logisticsCode);

    /**
     * 查询超过指定时长未支付的订单
     */
    @Select("SELECT * FROM tb_order WHERE status = #{status} AND create_time < #{deadline} AND is_deleted = 0")
    List<Order> selectTimeoutOrders(@Param("status") Integer status, @Param("deadline") LocalDateTime deadline);

    /**
     * 分页查询订单列表
     */
    @Select("<script>" +
            "SELECT * FROM tb_order " +
            "<where>" +
            "<if test='queryDTO.userId != null'> AND user_id = #{queryDTO.userId} </if>" +
            "<if test='queryDTO.status != null'> AND status = #{queryDTO.status} </if>" +
            "<if test='queryDTO.orderNo != null'> AND order_no = #{queryDTO.orderNo} </if>" +
            "<if test='queryDTO.startTime != null'> AND create_time &gt;= #{queryDTO.startTime} </if>" +
            "<if test='queryDTO.endTime != null'> AND create_time &lt;= #{queryDTO.endTime} </if>" +
            "</where>" +
            "ORDER BY create_time DESC" +
            "</script>")
    Page<Order> queryOrders(OrderQueryDTO queryDTO);
}