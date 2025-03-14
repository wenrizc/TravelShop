package com.hmdp.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hmdp.entity.TicketUsage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 门票使用记录 Mapper 接口
 */
@Mapper
public interface TicketUsageMapper extends BaseMapper<TicketUsage> {

    /**
     * 根据核销码查询门票使用记录
     */
    @Select("SELECT * FROM tb_ticket_usage WHERE code = #{code}")
    TicketUsage getByCode(@Param("code") String code);

    /**
     * 根据订单项ID查询门票使用记录
     */
    @Select("SELECT * FROM tb_ticket_usage WHERE order_item_id = #{orderItemId}")
    TicketUsage getByOrderItemId(@Param("orderItemId") Long orderItemId);

    /**
     * 更新门票状态为已使用
     */
    @Update("UPDATE tb_ticket_usage SET status = 2, use_time = #{useTime}, update_time = #{useTime} " +
            "WHERE code = #{code} AND status = 1")
    int updateStatusToUsed(@Param("code") String code, @Param("useTime") LocalDateTime useTime);

    /**
     * 更新门票状态为已过期
     */
    @Update("UPDATE tb_ticket_usage SET status = 3, update_time = #{updateTime} " +
            "WHERE id = #{id} AND status = 1")
    int updateStatusToExpired(@Param("id") Long id, @Param("updateTime") LocalDateTime updateTime);

    /**
     * 更新门票状态为已退款
     */
    @Update("UPDATE tb_ticket_usage SET status = 4, update_time = #{updateTime} " +
            "WHERE order_id = #{orderId} AND status = 1")
    int updateStatusToRefunded(@Param("orderId") Long orderId, @Param("updateTime") LocalDateTime updateTime);

    /**
     * 获取用户的门票列表
     */
    @Select("SELECT * FROM tb_ticket_usage WHERE user_id = #{userId} ORDER BY create_time DESC")
    List<TicketUsage> getUserTickets(@Param("userId") Long userId);

    /**
     * 查询订单项下未使用的门票数量
     */
    @Select("SELECT COUNT(*) FROM tb_ticket_usage WHERE order_item_id = #{orderItemId} AND status = 1")
    int countUnusedTickets(@Param("orderItemId") Long orderItemId);

    /**
     * 查询订单项下的门票总数
     */
    @Select("SELECT COUNT(*) FROM tb_ticket_usage WHERE order_item_id = #{orderItemId}")
    int countTotalTickets(@Param("orderItemId") Long orderItemId);
}