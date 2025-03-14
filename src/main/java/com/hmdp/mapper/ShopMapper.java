package com.hmdp.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hmdp.entity.Shop;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 店铺Mapper接口
 */
public interface ShopMapper extends BaseMapper<Shop> {

    @Select("SELECT id FROM tb_shop")
    List<Long> selectAllIds();

}
