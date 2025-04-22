package com.travelshop.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.travelshop.dto.Result;
import com.travelshop.entity.ShopType;

/**
 * <p>
 *  服务类
 * </p>
*/
public interface IShopTypeService extends IService<ShopType> {

    /**
     * 获取商品类型列表
     *
     * @return {@link Result}
     */
    Result getTypeList();
}
