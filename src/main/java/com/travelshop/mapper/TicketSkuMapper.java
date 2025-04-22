package com.travelshop.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.travelshop.entity.TicketSku;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 门票规格数据访问层
 */
@Mapper
public interface TicketSkuMapper extends BaseMapper<TicketSku> {

    /**
     * 根据门票ID查询所有规格
     */
    @Select("SELECT * FROM tb_ticket_sku WHERE ticket_id = #{ticketId}")
    List<TicketSku> queryByTicketId(@Param("ticketId") Long ticketId);

    /**
     * 减少库存并增加销量
     */
    @Update("UPDATE tb_ticket_sku SET stock = stock - #{count}, sale_count = sale_count + #{count} " +
            "WHERE id = #{skuId} AND stock >= #{count}")
    int decreaseStock(@Param("skuId") Long skuId, @Param("count") Integer count);

    /**
     * 增加库存（退款时使用）
     */
    @Update("UPDATE tb_ticket_sku SET stock = stock + #{count}, stock_locked = stock_locked - #{count} " +
            "WHERE id = #{skuId}")
    int increaseStock(@Param("skuId") Long skuId, @Param("count") Integer count);

    /**
     * 锁定库存（下单时使用）
     */
    @Update("UPDATE tb_ticket_sku SET stock = stock - #{count}, stock_locked = stock_locked + #{count} " +
            "WHERE id = #{skuId} AND stock >= #{count}")
    int lockStock(@Param("skuId") Long skuId, @Param("count") Integer count);

    /**
     * 解锁库存（订单超时未支付时使用）
     */
    @Update("UPDATE tb_ticket_sku SET stock = stock + #{count}, stock_locked = stock_locked - #{count} " +
            "WHERE id = #{skuId}")
    Integer selectStockById(Long skuId);
}