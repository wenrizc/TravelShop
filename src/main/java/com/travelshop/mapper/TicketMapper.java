package com.travelshop.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.travelshop.entity.Ticket;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 门票数据访问层
 */
@Mapper
public interface TicketMapper extends BaseMapper<Ticket> {

    /**
     * 根据ID查询门票及其商铺信息
     */
    @Select("SELECT t.*, s.name as shop_name FROM tb_ticket t LEFT JOIN tb_shop s ON t.shop_id = s.id WHERE t.id = #{id}")
    Ticket getTicketWithShop(@Param("id") Long id);

    /**
     * 根据商铺ID查询门票列表
     */
    @Select("SELECT * FROM tb_ticket WHERE shop_id = #{shopId} AND status = 1")
    List<Ticket> queryTicketsByShopId(@Param("shopId") Long shopId);

    /**
     * 更新门票状态
     */
    @Update("UPDATE tb_ticket SET status = #{status}, update_time = NOW() WHERE id = #{id}")
    int updateStatus(@Param("id") Long id, @Param("status") Integer status);

    /**
     * 增加销量
     */
    @Update("UPDATE tb_ticket SET sale_count = sale_count + #{count}, update_time = NOW() WHERE id = #{id}")
    int increaseSaleCount(@Param("id") Long id, @Param("count") Integer count);
}