package com.travelshop.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.travelshop.entity.ShopOwner;

import java.util.List;

public interface IShopOwnerService extends IService<ShopOwner> {

    /**
     * 查询用户拥有的商铺ID列表
     */
    List<Long> getShopIdsByUserId(Long userId);

    /**
     * 查询商铺的所有者ID
     */
    Long getOwnerIdByShopId(Long shopId);

    /**
     * 检查用户是否拥有特定商铺
     */
    boolean checkOwnership(Long userId, Long shopId);

    /**
     * 为用户分配店铺所有权
     */
    boolean assignShopToUser(Long userId, Long shopId);

    /**
     * 撤销店铺所有权
     */
    boolean removeShopFromUser(Long userId, Long shopId);
}