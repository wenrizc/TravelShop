package com.travelshop.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.travelshop.entity.ShopOwner;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ShopOwnerMapper extends BaseMapper<ShopOwner> {

    /**
     * 查询用户拥有的商铺ID列表
     */
    @Select("SELECT shop_id FROM tb_shop_owner WHERE user_id = #{userId}")
    List<Long> getShopIdsByUserId(@Param("userId") Long userId);

    /**
     * 查询商铺的所有者ID
     */
    @Select("SELECT user_id FROM tb_shop_owner WHERE shop_id = #{shopId}")
    Long getOwnerIdByShopId(@Param("shopId") Long shopId);

    /**
     * 检查用户是否拥有特定商铺
     */
    @Select("SELECT COUNT(1) FROM tb_shop_owner WHERE user_id = #{userId} AND shop_id = #{shopId}")
    int checkOwnership(@Param("userId") Long userId, @Param("shopId") Long shopId);
}