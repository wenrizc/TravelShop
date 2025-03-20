package com.hmdp.service.impl;

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

import java.util.HashMap;
import java.util.Map;

/**
 * 商品服务实现类
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ProductServiceImpl extends ServiceImpl<ProductMapper, Product> implements IProductService {

    private final ProductSkuMapper productSkuMapper;
    private final IShopService shopService;
    private final ProductMapper productMapper;

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
        ProductSku sku = productSkuMapper.selectById(skuId);
        if (sku == null || !sku.getProductId().equals(productId)) {
            log.warn("商品规格不存在或不属于该商品: productId={}, skuId={}", productId, skuId);
            return null;
        }
        // 将SKU信息存入结果
        result.put("sku", sku);
        result.put("skuId", sku.getId());
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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean increaseSales(Long productId, Integer count) {
        log.info("增加商品销量: productId={}, count={}", productId, count);
        return productMapper.increaseSales(productId, count);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean decreaseSales(Long productId, Integer count) {
        log.info("减少商品销量: productId={}, count={}", productId, count);
        return productMapper.decreaseSales(productId, count);
    }

    @Override
    public Product getById(Long productId) {
        return productMapper.selectById(productId);
    }
}