package com.hmdp.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.entity.Product;
import com.hmdp.entity.ProductSku;
import com.hmdp.entity.Shop;
import com.hmdp.mapper.ProductMapper;
import com.hmdp.mapper.ProductSkuMapper;
import com.hmdp.service.IProductService;
import com.hmdp.service.IShopService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 商品服务实现类
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ProductServiceImpl extends ServiceImpl<ProductMapper, Product> implements IProductService {

    private final ProductMapper productMapper;
    private final ProductSkuMapper productSkuMapper;
    private final IShopService shopService;

    @Override
    public Map<String, Object> getProductInfo(Long productId, Long skuId) {
        log.info("获取商品信息: productId={}, skuId={}", productId, skuId);

        Map<String, Object> result = new HashMap<>();

        // 获取商品基本信息
        Product product = getById(productId);
        if (product == null) {
            log.warn("商品不存在: productId={}", productId);
            return null;
        }

        // 将商品信息存入结果
        result.put("product", product);
        result.put("productId", product.getId());
        result.put("name", product.getName());
        result.put("imageUrl", product.getCover());
        result.put("onSale", product.getStatus() == 1);

        // 获取SKU信息
        ProductSku sku;
        if (skuId != null) {
            sku = productSkuMapper.selectById(skuId);
            if (sku == null || !sku.getProductId().equals(productId)) {
                log.warn("商品规格不存在或不属于该商品: productId={}, skuId={}", productId, skuId);
                return null;
            }
        } else {
            // 获取默认SKU（如第一个）
            sku = productSkuMapper.getDefaultSku(productId);
            if (sku == null) {
                log.warn("商品没有默认规格: productId={}", productId);
                return null;
            }
        }

        // 将SKU信息存入结果
        result.put("sku", sku);
        result.put("skuId", sku.getId());
        result.put("skuName", sku.getName());
        result.put("price", sku.getPrice());
        result.put("stock", sku.getStock() - sku.getStockLocked());

        // 更新onSale标志，同时考虑SKU状态
        result.put("onSale", (boolean)result.get("onSale") && sku.getStatus() == 1);

        // 获取店铺信息
        if (product.getShopId() != null) {
            Shop shop = shopService.getById(product.getShopId());
            if (shop != null) {
                result.put("shopName", shop.getName());
                result.put("shopId", shop.getId());
            }
        }

        return result;
    }

    // 其他方法保持不变

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean lockStock(Long productId, Long skuId, Integer quantity) {
        log.info("锁定商品库存: productId={}, skuId={}, quantity={}", productId, skuId, quantity);

        if (productId == null || quantity == null || quantity <= 0) {
            log.warn("锁定库存参数无效");
            return false;
        }

        // 获取SKU
        ProductSku sku;
        if (skuId != null) {
            sku = productSkuMapper.selectById(skuId);
            if (sku == null || !sku.getProductId().equals(productId)) {
                log.warn("商品规格不存在或不属于该商品: productId={}, skuId={}", productId, skuId);
                return false;
            }
        } else {
            // 获取默认SKU
            sku = productSkuMapper.getDefaultSku(productId);
            if (sku == null) {
                log.warn("商品没有默认规格: productId={}", productId);
                return false;
            }
        }

        // 检查商品状态
        Product product = getById(productId);
        if (product == null || product.getStatus() != 1) {
            log.warn("商品不存在或已下架: productId={}", productId);
            return false;
        }

        // 检查SKU状态
        if (sku.getStatus() != 1) {
            log.warn("商品规格已下架: skuId={}", skuId);
            return false;
        }

        // 检查库存
        if (sku.getStock() - sku.getStockLocked() < quantity) {
            log.warn("库存不足: 可用={}, 需要={}", sku.getStock() - sku.getStockLocked(), quantity);
            return false;
        }

        // 锁定库存
        int affected = productSkuMapper.incrementLockedStock(sku.getId(), quantity);

        return affected > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean unlockStock(Long productId, Long skuId, Integer quantity) {
        // 此方法实现保持不变
        // ...省略实现代码...
        return false; // 实际应返回操作结果
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deductStock(Long productId, Long skuId, Integer quantity) {
        // 此方法实现保持不变
        // ...省略实现代码...
        return false; // 实际应返回操作结果
    }

    @Override
    public List<ProductSku> getProductSkus(Long productId) {
        log.info("获取商品所有规格: productId={}", productId);

        if (productId == null) {
            return null;
        }

        return productSkuMapper.selectList(
                new LambdaQueryWrapper<ProductSku>()
                        .eq(ProductSku::getProductId, productId)
                        .eq(ProductSku::getStatus, 1)
                        .orderByAsc(ProductSku::getSort)
        );
    }

    @Override
    public BigDecimal getProductMinPrice(Long productId) {
        return productSkuMapper.selectMinPrice(productId);
    }
}