package com.travelshop.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.travelshop.entity.ProductSku;
import com.travelshop.mapper.ProductSkuMapper;
import com.travelshop.service.IProductSkuService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 商品规格服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductSkuServiceImpl extends ServiceImpl<ProductSkuMapper, ProductSku> implements IProductSkuService {

    private final ProductSkuMapper productSkuMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean lockStock(Long skuId, Integer count) {
        log.info("锁定商品库存: skuId={}, count={}", skuId, count);
        return productSkuMapper.lockStock(skuId, count);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean unlockStock(Long skuId, Integer count) {
        log.info("解锁商品库存: skuId={}, count={}", skuId, count);
        return productSkuMapper.unlockStock(skuId, count);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean confirmStock(Long skuId, Integer count) {
        log.info("确认商品库存扣减: skuId={}, count={}", skuId, count);
        return productSkuMapper.confirmLockedStock(skuId, count);
    }
}