package com.travelshop.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.travelshop.entity.ShopOwner;
import com.travelshop.entity.User;
import com.travelshop.mapper.ShopOwnerMapper;
import com.travelshop.mapper.UserMapper;
import com.travelshop.service.IShopOwnerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class ShopOwnerServiceImpl extends ServiceImpl<ShopOwnerMapper, ShopOwner> implements IShopOwnerService {

    @Resource
    private UserMapper userMapper;

    @Override
    public List<Long> getShopIdsByUserId(Long userId) {
        return baseMapper.getShopIdsByUserId(userId);
    }

    @Override
    public Long getOwnerIdByShopId(Long shopId) {
        return baseMapper.getOwnerIdByShopId(shopId);
    }

    @Override
    public boolean checkOwnership(Long userId, Long shopId) {
        return baseMapper.checkOwnership(userId, shopId) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean assignShopToUser(Long userId, Long shopId) {
        try {
            // 查询用户是否存在
            User user = userMapper.selectById(userId);
            if (user == null) {
                log.error("用户不存在, userId: {}", userId);
                return false;
            }

            // 检查是否已经是店铺所有者
            if (baseMapper.checkOwnership(userId, shopId) > 0) {
                log.info("用户 {} 已经是商铺 {} 的所有者", userId, shopId);
                return true;
            }

            // 创建关联记录
            ShopOwner shopOwner = new ShopOwner();
            shopOwner.setUserId(userId);
            shopOwner.setShopId(shopId);

            // 确保用户角色为商家
            if (user.getRole() == null || user.getRole() < 1) {
                user.setRole(1); // 设置为商家角色
                userMapper.updateById(user);
                log.info("用户 {} 角色已更新为商家", userId);
            }

            return save(shopOwner);
        } catch (Exception e) {
            log.error("为用户 {} 分配商铺 {} 时发生错误", userId, shopId, e);
            throw e;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeShopFromUser(Long userId, Long shopId) {
        try {
            // 删除关联关系
            int rows = lambdaUpdate()
                    .eq(ShopOwner::getUserId, userId)
                    .eq(ShopOwner::getShopId, shopId)
                    .remove() ? 1 : 0;

            // 如果用户没有其他店铺，将角色恢复为普通用户
            List<Long> remainingShops = getShopIdsByUserId(userId);
            if (remainingShops == null || remainingShops.isEmpty()) {
                User user = userMapper.selectById(userId);
                if (user != null && user.getRole() != null && user.getRole() == 1) {
                    user.setRole(0); // 恢复为普通用户
                    userMapper.updateById(user);
                    log.info("用户 {} 已没有商铺，角色已更新为普通用户", userId);
                }
            }

            return rows > 0;
        } catch (Exception e) {
            log.error("移除用户 {} 的商铺 {} 时发生错误", userId, shopId, e);
            throw e;
        }
    }
}