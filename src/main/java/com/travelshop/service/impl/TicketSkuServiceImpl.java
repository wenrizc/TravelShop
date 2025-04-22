package com.travelshop.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.travelshop.entity.TicketSku;
import com.travelshop.mapper.TicketSkuMapper;
import com.travelshop.service.ITicketSkuService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * <p>
 * 门票规格服务实现类
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TicketSkuServiceImpl extends ServiceImpl<TicketSkuMapper, TicketSku> implements ITicketSkuService {

    @Override
    public List<TicketSku> queryByTicketId(Long ticketId) {
        if (ticketId == null) {
            return null;
        }
        return baseMapper.queryByTicketId(ticketId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean lockStock(Long skuId, Integer count) {
        if (skuId == null || count == null || count <= 0) {
            log.warn("锁定库存参数错误: skuId={}, count={}", skuId, count);
            return false;
        }

        // 检查库存是否充足
        TicketSku ticketSku = getById(skuId);
        if (ticketSku == null || ticketSku.getStock() < count) {
            log.warn("库存不足，无法锁定: skuId={}, 当前库存={}, 请求数量={}",
                    skuId,
                    ticketSku == null ? "规格不存在" : ticketSku.getStock(),
                    count);
            return false;
        }

        // 锁定库存
        int affected = baseMapper.lockStock(skuId, count);
        boolean success = affected > 0;

        if (success) {
            log.info("库存锁定成功: skuId={}, count={}", skuId, count);
        } else {
            log.error("库存锁定失败: skuId={}, count={}", skuId, count);
        }

        return success;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean decreaseStock(Long skuId, Integer count) {
        if (skuId == null || count == null || count <= 0) {
            return false;
        }

        // 减少库存并增加销量
        int affected = baseMapper.decreaseStock(skuId, count);
        return affected > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean increaseStock(Long skuId, Integer count) {
        if (skuId == null || count == null || count <= 0) {
            return false;
        }

        // 增加库存（退款时使用）
        int affected = baseMapper.increaseStock(skuId, count);
        return affected > 0;
    }

    @Override
    public boolean checkStock(Long skuId, Integer count) {
        if (skuId == null || count == null || count <= 0) {
            return false;
        }

        TicketSku ticketSku = getById(skuId);
        return ticketSku != null && ticketSku.getStock() >= count;
    }

}