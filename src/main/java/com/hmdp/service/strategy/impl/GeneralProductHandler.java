package com.hmdp.service.strategy.impl;

import com.hmdp.dto.OrderCreateDTO;
import com.hmdp.entity.*;
import com.hmdp.enums.ProductType;
import com.hmdp.mapper.ProductSkuMapper;
import com.hmdp.service.IProductService;
import com.hmdp.service.IProductSkuService;
import com.hmdp.service.IProductSkuService;
import com.hmdp.service.strategy.ProductTypeHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 普通商品处理策略实现
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GeneralProductHandler implements ProductTypeHandler {

    private final IProductService productService;
    private final IProductSkuService productSkuService;
    private final ProductSkuMapper productSkuMapper;

    @Override
    public ProductType getProductType() {
        return ProductType.NORMAL;
    }

    @Override
    public void validateProduct(OrderCreateDTO.OrderItemDTO itemDTO) {
        // 1. 验证商品是否存在
        Product product = productService.getById(itemDTO.getProductId());
        if (product == null) {
            log.error("商品不存在: productId={}", itemDTO.getProductId());
            throw new RuntimeException("商品不存在");
        }

        // 2. 验证商品是否上架
        if (product.getStatus() != 1) {
            log.error("商品未上架: productId={}", itemDTO.getProductId());
            throw new RuntimeException("商品已下架");
        }

        // 3. 验证商品规格是否存在
        ProductSku productSku = productSkuService.getById(itemDTO.getSkuId());
        if (productSku == null) {
            log.error("商品规格不存在: skuId={}", itemDTO.getSkuId());
            throw new RuntimeException("商品规格不存在");
        }

        // 4. 验证库存是否足够
        if (productSku.getStock() < itemDTO.getCount()) {
            log.error("商品库存不足: productId={}, skuId={}, stock={}, count={}",
                    itemDTO.getProductId(), itemDTO.getSkuId(), productSku.getStock(), itemDTO.getCount());
            throw new RuntimeException("商品库存不足");
        }
    }

    @Override
    public void setupOrderItem(OrderItem item, OrderCreateDTO.OrderItemDTO itemDTO) {
        // 1. 获取商品和规格信息
        Product product = productService.getById(itemDTO.getProductId());
        ProductSku productSku = productSkuService.getById(itemDTO.getSkuId());

        // 2. 设置订单项信息
        item.setProductName(product.getName());
        item.setProductImg(product.getImages().split(",")[0]); // 取第一张图片
        item.setSkuName(productSku.getSpecsValues());
        item.setPrice(productSku.getPrice());

    }

    @Override
    public void processAfterOrderCreation(Order order, OrderItem item) {
        // 订单创建后，锁定库存（预扣库存）
        log.info("锁定商品库存: productId={}, skuId={}, count={}",
                item.getProductId(), item.getSkuId(), item.getCount());

        boolean success = productSkuMapper.lockStock(item.getSkuId(), item.getCount());
        if (!success) {
            log.error("锁定商品库存失败: productId={}, skuId={}, count={}",
                    item.getProductId(), item.getSkuId(), item.getCount());
            throw new RuntimeException("锁定商品库存失败");
        }
    }

    @Override
    public void processAfterPayment(Order order, OrderItem item) {
        // 1. 支付成功后，实际扣减库存（如果没有在创建订单时预扣）
        log.info("支付成功，扣减商品库存: productId={}, skuId={}, count={}",
                item.getProductId(), item.getSkuId(), item.getCount());

        // 如果已经锁定库存，则将锁定状态改为已扣减
        boolean success = productSkuMapper.confirmLockedStock(item.getSkuId(), item.getCount());
        if (!success) {
            log.error("更新商品库存状态失败: productId={}, skuId={}, count={}",
                    item.getProductId(), item.getSkuId(), item.getCount());
        }

        // 2. 更新商品销量
        productService.increaseSales(item.getProductId(), item.getCount());

    }

    @Override
    public void processAfterCancellation(Order order, OrderItem item) {
        // 取消订单，恢复库存
        log.info("取消订单，恢复商品库存: productId={}, skuId={}, count={}",
                item.getProductId(), item.getSkuId(), item.getCount());

        // 释放锁定的库存
        boolean success = productSkuMapper.unlockStock(item.getSkuId(), item.getCount());
        if (!success) {
            log.error("恢复商品库存失败: productId={}, skuId={}, count={}",
                    item.getProductId(), item.getSkuId(), item.getCount());
        }

        // 如果已经支付并扣减了销量，则恢复销量（根据实际业务逻辑决定是否需要）
        if (order.getPayTime() != null) {
            productService.decreaseSales(item.getProductId(), item.getCount());
        }
    }
}