package com.hmdp.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.Result;
import com.hmdp.entity.*;
import com.hmdp.mapper.ProductMapper;
import com.hmdp.mapper.ProductSkuMapper;
import com.hmdp.mapper.TempCartMapper;
import com.hmdp.service.IProductService;
import com.hmdp.service.ShoppingCartItemService;
import com.hmdp.service.ShoppingCartService;
import com.hmdp.service.TempCartService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 临时购物车服务实现类（处理未登录用户的购物车）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TempCartServiceImpl extends ServiceImpl<TempCartMapper, TempCart> implements TempCartService {

    private final TempCartMapper tempCartMapper;
    private final ShoppingCartService shoppingCartService;
    private final ShoppingCartItemService shoppingCartItemService;
    private final ProductMapper productMapper;
    private final ProductSkuMapper productSkuMapper;

    // 临时购物车过期时间（天）
    private static final int TEMP_CART_EXPIRE_DAYS = 3;


    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result updateQuantity(String sessionId, Long id, Integer quantity) {
        if (!StringUtils.hasText(sessionId) || id == null) {
            return Result.fail("参数不完整");
        }

        if (quantity == null || quantity <= 0) {
            return Result.fail("数量必须大于0");
        }

        log.info("更新临时购物车项数量: sessionId={}, id={}, quantity={}", sessionId, id, quantity);

        // 验证项是否存在且属于当前会话
        TempCart item = getById(id);
        if (item == null || !item.getSessionId().equals(sessionId)) {
            return Result.fail("购物车项不存在或不属于当前会话");
        }

        // 校验商品和库存
        Product product = productMapper.selectById(item.getProductId());
        if (product == null) {
            return Result.fail("商品不存在");
        }

        if (product.getStatus() != 1) {
            return Result.fail("商品已下架");
        }

        // 获取SKU信息
        ProductSku sku = item.getSkuId() != null ?
                productSkuMapper.selectById(item.getSkuId()) :
                productSkuMapper.getDefaultSku(item.getProductId());

        if (sku == null) {
            return Result.fail("商品规格不存在");
        }

        if (sku.getStatus() != 1) {
            return Result.fail("商品规格已下架");
        }

        // 检查库存
        int availableStock = sku.getStock() - sku.getStockLocked();
        if (availableStock < quantity) {
            return Result.fail("商品库存不足");
        }

        int rows = tempCartMapper.updateQuantity(id, quantity);
        return rows > 0 ? Result.ok() : Result.fail("更新失败");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result addItem(String sessionId, Long productId, Integer productType, Integer quantity, Long skuId) {
        // 参数校验
        if (!StringUtils.hasText(sessionId)) {
            return Result.fail("会话ID不能为空");
        }

        if (productId == null || productType == null) {
            return Result.fail("商品信息不完整");
        }

        if (quantity == null || quantity <= 0) {
            return Result.fail("商品数量必须大于0");
        }

        log.info("添加商品到临时购物车: sessionId={}, productId={}, productType={}, quantity={}, skuId={}",
                sessionId, productId, productType, quantity, skuId);

        // 校验商品信息
        Product product = productMapper.selectById(productId);
        if (product == null) {
            return Result.fail("商品不存在");
        }

        if (product.getStatus() != 1) {
            return Result.fail("商品已下架");
        }

        // 获取SKU信息
        ProductSku sku;
        if (skuId != null) {
            sku = productSkuMapper.selectById(skuId);
            if (sku == null || !sku.getProductId().equals(productId)) {
                return Result.fail("商品规格不存在或不属于该商品");
            }
        } else {
            // 获取默认SKU
            sku = productSkuMapper.getDefaultSku(productId);
            if (sku == null) {
                return Result.fail("商品规格不存在");
            }
        }

        // 检查SKU状态
        if (sku.getStatus() != 1) {
            return Result.fail("商品规格已下架");
        }

        // 检查库存
        int availableStock = sku.getStock() - sku.getStockLocked();
        if (availableStock < quantity) {
            return Result.fail("商品库存不足");
        }

        // 查询是否已存在相同商品
        TempCart existItem = getByProductInfo(sessionId, productId, productType, skuId);
        if (existItem != null) {
            // 已存在，更新数量
            int newQuantity = existItem.getQuantity() + quantity;
            if (availableStock < newQuantity) {
                return Result.fail("商品库存不足");
            }

            boolean success = updateQuantity(sessionId, existItem.getId(), newQuantity).getSuccess();
            return success ? Result.ok(existItem.getId()) : Result.fail("更新数量失败");
        } else {
            // 不存在，创建新项
            TempCart tempCart = new TempCart();
            tempCart.setSessionId(sessionId);
            tempCart.setProductId(productId);
            tempCart.setProductName(product.getName());
            tempCart.setProductImage(product.getCover());
            tempCart.setProductType(productType);
            tempCart.setSkuId(sku.getId());
            tempCart.setSkuName(sku.getName());
            tempCart.setPrice(sku.getPrice());
            tempCart.setQuantity(quantity);
            tempCart.setCreatedTime(LocalDateTime.now());
            tempCart.setExpireTime(LocalDateTime.now().plusDays(TEMP_CART_EXPIRE_DAYS));

            boolean success = save(tempCart);
            return success ? Result.ok(tempCart.getId()) : Result.fail("添加商品失败");
        }
    }



    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result buyNow(String sessionId, Long productId, Integer productType, Integer quantity, Long skuId) {
        // 参数校验
        if (!StringUtils.hasText(sessionId) || productId == null || productType == null || quantity == null) {
            return Result.fail("参数不完整");
        }

        log.info("临时购物车立即购买: sessionId={}, productId={}, productType={}, quantity={}, skuId={}",
                sessionId, productId, productType, quantity, skuId);

        // 校验商品信息
        Product product = productMapper.selectById(productId);
        if (product == null) {
            return Result.fail("商品不存在");
        }

        if (product.getStatus() != 1) {
            return Result.fail("商品已下架");
        }

        // 获取SKU信息
        ProductSku sku;
        if (skuId != null) {
            sku = productSkuMapper.selectById(skuId);
            if (sku == null || !sku.getProductId().equals(productId)) {
                return Result.fail("商品规格不存在或不属于该商品");
            }
        } else {
            // 获取默认SKU
            sku = productSkuMapper.getDefaultSku(productId);
            if (sku == null) {
                return Result.fail("商品规格不存在");
            }
        }

        // 检查SKU状态和库存
        if (sku.getStatus() != 1) {
            return Result.fail("商品规格已下架");
        }

        int availableStock = sku.getStock() - sku.getStockLocked();
        if (availableStock < quantity) {
            return Result.fail("商品库存不足");
        }

        // 创建临时数据用于结算
        TempCart tempItem = new TempCart();
        tempItem.setSessionId(sessionId);
        tempItem.setProductId(productId);
        tempItem.setProductName(product.getName());
        tempItem.setProductImage(product.getCover());
        tempItem.setProductType(productType);
        tempItem.setSkuId(sku.getId());
        tempItem.setSkuName(sku.getName());
        tempItem.setPrice(sku.getPrice());
        tempItem.setQuantity(quantity);
        tempItem.setCreatedTime(LocalDateTime.now());
        tempItem.setExpireTime(LocalDateTime.now().plusHours(1)); // 临时数据，1小时过期

        save(tempItem);

        // 返回临时订单信息
        return Result.ok(tempItem);
    }

    @Override
    public List<TempCart> getBySessionId(String sessionId) {
        if (!StringUtils.hasText(sessionId)) {
            log.warn("会话ID为空");
            return List.of();
        }

        log.info("查询临时购物车: sessionId={}", sessionId);
        return tempCartMapper.getBySessionId(sessionId);
    }

    @Override
    public TempCart getByProductInfo(String sessionId, Long productId, Integer productType, Long skuId) {
        if (!StringUtils.hasText(sessionId) || productId == null || productType == null) {
            log.warn("参数不完整: sessionId={}, productId={}, productType={}", sessionId, productId, productType);
            return null;
        }

        log.info("查询临时购物车项: sessionId={}, productId={}, productType={}, skuId={}",
                sessionId, productId, productType, skuId);
        return tempCartMapper.getByProductInfo(sessionId, productId, productType, skuId);
    }






    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result removeItem(String sessionId, Long id) {
        if (!StringUtils.hasText(sessionId) || id == null) {
            return Result.fail("参数不完整");
        }

        log.info("删除临时购物车项: sessionId={}, id={}", sessionId, id);

        // 验证项是否存在且属于当前会话
        TempCart item = getById(id);
        if (item == null || !item.getSessionId().equals(sessionId)) {
            return Result.fail("购物车项不存在或不属于当前会话");
        }

        boolean success = removeById(id);
        return success ? Result.ok() : Result.fail("删除失败");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result clearCart(String sessionId) {
        if (!StringUtils.hasText(sessionId)) {
            return Result.fail("会话ID不能为空");
        }

        log.info("清空临时购物车: sessionId={}", sessionId);

        int rows = tempCartMapper.clearBySessionId(sessionId);
        return Result.ok(rows);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result mergeToUserCart(Long userId, String sessionId) {
        if (userId == null || !StringUtils.hasText(sessionId)) {
            return Result.fail("参数不完整");
        }

        log.info("合并临时购物车到用户购物车: userId={}, sessionId={}", userId, sessionId);

        // 获取临时购物车项
        List<TempCart> tempItems = getBySessionId(sessionId);
        if (CollectionUtils.isEmpty(tempItems)) {
            log.info("临时购物车为空，无需合并");
            return Result.ok(0);
        }

        // 获取或创建用户购物车
        ShoppingCart userCart = shoppingCartService.getOrCreateCurrentCart();
        if (userCart == null) {
            return Result.fail("获取用户购物车失败");
        }

        // 合并购物车项
        int mergeCount = 0;
        for (TempCart tempItem : tempItems) {
            // 检查用户购物车中是否已存在相同商品
            ShoppingCartItem existItem = shoppingCartItemService.getByProductInfo(
                    userCart.getId(), tempItem.getProductId(), tempItem.getProductType(), tempItem.getSkuId());

            if (existItem != null) {
                // 更新数量
                int newQuantity = existItem.getQuantity() + tempItem.getQuantity();
                shoppingCartItemService.updateQuantity(existItem.getId(), newQuantity);
            } else {
                // 添加新项
                ShoppingCartItem newItem = new ShoppingCartItem();
                newItem.setCartId(userCart.getId());
                newItem.setProductId(tempItem.getProductId());
                newItem.setProductName(tempItem.getProductName());
                newItem.setProductImage(tempItem.getProductImage());
                newItem.setProductType(tempItem.getProductType());
                newItem.setSkuId(tempItem.getSkuId());
                newItem.setSkuName(tempItem.getSkuName());
                newItem.setPrice(tempItem.getPrice());
                newItem.setQuantity(tempItem.getQuantity());
                newItem.setSelected(1); // 默认选中
                newItem.setCreatedTime(LocalDateTime.now());

                shoppingCartItemService.save(newItem);
            }

            mergeCount++;
        }

        // 清空临时购物车
        clearCart(sessionId);

        return Result.ok(mergeCount);
    }



    @Override
    public int cleanExpiredItems() {
        log.info("清理过期购物车项");
        int rows = tempCartMapper.cleanExpiredItems();
        log.info("已清理{}条过期购物车项", rows);
        return rows;
    }

    @Override
    public int countItems(String sessionId) {
        if (!StringUtils.hasText(sessionId)) {
            return 0;
        }

        LambdaQueryWrapper<TempCart> wrapper = new LambdaQueryWrapper<TempCart>()
                .eq(TempCart::getSessionId, sessionId)
                .gt(TempCart::getExpireTime, LocalDateTime.now());

        return Math.toIntExact(count(wrapper));
    }
}