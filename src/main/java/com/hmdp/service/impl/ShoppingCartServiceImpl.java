package com.hmdp.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.Result;
import com.hmdp.dto.ShoppingCartDTO;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.*;
import com.hmdp.enums.CartStatus;
import com.hmdp.mapper.ShoppingCartMapper;
import com.hmdp.service.*;
import com.hmdp.utils.UserHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 购物车服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ShoppingCartServiceImpl extends ServiceImpl<ShoppingCartMapper, ShoppingCart> implements ShoppingCartService {

    private final ShoppingCartMapper shoppingCartMapper;
    private final ShoppingCartItemService shoppingCartItemService;
    private final TempCartService tempCartService;
    private final CartPromotionService cartPromotionService;
    private final IProductService productService;

    @Override
    public ShoppingCart getUserCart(Long userId) {
        log.info("获取用户购物车: userId={}", userId);
        if (userId == null) {
            return null;
        }

        // 查询用户当前活跃购物车
        ShoppingCart cart = shoppingCartMapper.getActiveCartByUserId(userId);
        if (cart != null) {
            // 加载购物车项
            List<ShoppingCartItem> items = shoppingCartItemService.listByCartId(cart.getId());
            cart.setItems(items);
        }

        return cart;
    }

    @Override
    public ShoppingCart getOrCreateCurrentCart() {
        UserDTO userDTO = UserHolder.getUser();
        Long userId = userDTO.getId();

        // 查询用户当前活跃购物车
        ShoppingCart cart = shoppingCartMapper.getActiveCartByUserId(userId);
        
        if (cart == null) {
            log.info("用户没有活跃购物车，创建新购物车: userId={}", userId);
            // 创建新购物车
            cart = new ShoppingCart();
            cart.setUserId(userId);
            cart.setCreatedTime(LocalDateTime.now());
            cart.setUpdatedTime(LocalDateTime.now());
            cart.setStatus(CartStatus.NORMAL.getCode());
            save(cart);
        }

        // 加载购物车项
        List<ShoppingCartItem> items = shoppingCartItemService.listByCartId(cart.getId());
        cart.setItems(items);

        return cart;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result addItem(Long userId, Long productId, Integer productType, Integer quantity, Long skuId) {
        log.info("添加商品到购物车: userId={}, productId={}, productType={}, quantity={}, skuId={}",
                userId, productId, productType, quantity, skuId);

        // 参数校验
        if (userId == null || productId == null || productType == null || quantity == null || quantity <= 0) {
            return Result.fail("参数不完整或无效");
        }

        // 验证商品信息
        Map<String, Object> productInfo = productService.getProductInfo(productId, skuId);
        if (productInfo == null) {
            return Result.fail("商品不存在");
        }

        boolean onSale = (boolean) productInfo.get("onSale");
        if (!onSale) {
            return Result.fail("商品已下架");
        }

        Integer stock = (Integer) productInfo.get("stock");
        if (stock < quantity) {
            return Result.fail("商品库存不足");
        }

        // 获取或创建购物车
        ShoppingCart cart = getOrCreateCurrentCart();

        // 检查是否已存在该商品
        ShoppingCartItem existItem = shoppingCartItemService.getByProductInfo(
                cart.getId(), productId, productType, skuId);

        if (existItem != null) {
            // 已存在，更新数量
            int newQuantity = existItem.getQuantity() + quantity;
            // 再次检查库存
            if (stock < newQuantity) {
                return Result.fail("商品库存不足");
            }

            boolean updated = shoppingCartItemService.updateQuantity(existItem.getId(), newQuantity);
            if (!updated) {
                return Result.fail("更新购物车失败");
            }
            return Result.ok(existItem.getId());
        } else {
            // 创建新购物车项
            ShoppingCartItem newItem = new ShoppingCartItem();
            newItem.setCartId(cart.getId());
            newItem.setProductId(productId);
            newItem.setProductName((String) productInfo.get("name"));
            newItem.setProductImage((String) productInfo.get("imageUrl"));
            newItem.setProductType(productType);
            newItem.setSkuId(skuId);
            newItem.setSkuName((String) productInfo.get("skuName"));
            newItem.setPrice((BigDecimal) productInfo.get("price"));
            newItem.setQuantity(quantity);
            newItem.setSelected(1); // 默认选中
            newItem.setCreatedTime(LocalDateTime.now());
            newItem.setUpdatedTime(LocalDateTime.now());

            boolean saved = shoppingCartItemService.save(newItem);
            if (!saved) {
                return Result.fail("添加购物车失败");
            }

            // 更新购物车时间
            cart.setUpdatedTime(LocalDateTime.now());
            updateById(cart);

            return Result.ok(newItem.getId());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result updateItemQuantity(Long userId, Long itemId, Integer quantity) {
        log.info("更新购物车商品数量: userId={}, itemId={}, quantity={}", userId, itemId, quantity);
        
        // 参数校验
        if (userId == null || itemId == null || quantity == null || quantity <= 0) {
            return Result.fail("参数不完整或无效");
        }

        // 验证购物车项存在且属于用户
        ShoppingCartItem item = shoppingCartItemService.getById(itemId);
        if (item == null) {
            return Result.fail("购物车项不存在");
        }

        ShoppingCart cart = getById(item.getCartId());
        if (cart == null || !cart.getUserId().equals(userId)) {
            return Result.fail("无权操作此购物车项");
        }

        // 验证商品库存
        Map<String, Object> productInfo = productService.getProductInfo(item.getProductId(), item.getSkuId());
        if (productInfo == null) {
            return Result.fail("商品不存在");
        }

        Integer stock = (Integer) productInfo.get("stock");
        if (stock < quantity) {
            return Result.fail("商品库存不足");
        }

        // 更新数量
        boolean updated = shoppingCartItemService.updateQuantity(itemId, quantity);
        if (!updated) {
            return Result.fail("更新失败");
        }

        // 更新购物车时间
        cart.setUpdatedTime(LocalDateTime.now());
        updateById(cart);

        return Result.ok();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result updateItemSelected(Long userId, Long itemId, Boolean selected) {
        log.info("更新购物车商品选中状态: userId={}, itemId={}, selected={}", userId, itemId, selected);
        
        // 参数校验
        if (userId == null || itemId == null || selected == null) {
            return Result.fail("参数不完整");
        }

        // 验证购物车项存在且属于用户
        ShoppingCartItem item = shoppingCartItemService.getById(itemId);
        if (item == null) {
            return Result.fail("购物车项不存在");
        }

        ShoppingCart cart = getById(item.getCartId());
        if (cart == null || !cart.getUserId().equals(userId)) {
            return Result.fail("无权操作此购物车项");
        }

        // 更新选中状态
        boolean updated = shoppingCartItemService.updateSelected(itemId, selected);
        if (!updated) {
            return Result.fail("更新失败");
        }

        // 更新购物车时间
        cart.setUpdatedTime(LocalDateTime.now());
        updateById(cart);

        return Result.ok();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result selectAll(Long userId, Boolean selected) {
        log.info("全选/取消全选购物车商品: userId={}, selected={}", userId, selected);
        
        // 参数校验
        if (userId == null || selected == null) {
            return Result.fail("参数不完整");
        }

        // 获取用户购物车
        ShoppingCart cart = getUserCart(userId);
        if (cart == null) {
            return Result.ok();
        }

        // 更新所有购物车项的选中状态
        boolean updated = shoppingCartItemService.updateAllSelected(cart.getId(), selected ? true : false);
        if (!updated) {
            return Result.fail("更新失败");
        }

        // 更新购物车时间
        cart.setUpdatedTime(LocalDateTime.now());
        updateById(cart);

        return Result.ok();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result removeItem(Long userId, Long itemId) {
        log.info("从购物车中删除商品: userId={}, itemId={}", userId, itemId);
        
        // 参数校验
        if (userId == null || itemId == null) {
            return Result.fail("参数不完整");
        }

        // 验证购物车项存在且属于用户
        ShoppingCartItem item = shoppingCartItemService.getById(itemId);
        if (item == null) {
            return Result.ok(); // 已不存在，视为删除成功
        }

        ShoppingCart cart = getById(item.getCartId());
        if (cart == null || !cart.getUserId().equals(userId)) {
            return Result.fail("无权操作此购物车项");
        }

        // 删除购物车项
        boolean removed = shoppingCartItemService.removeById(itemId);
        if (!removed) {
            return Result.fail("删除失败");
        }

        // 更新购物车时间
        cart.setUpdatedTime(LocalDateTime.now());
        updateById(cart);

        return Result.ok();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result removeItems(Long userId, List<Long> itemIds) {
        log.info("批量删除购物车商品: userId={}, itemIds={}", userId, itemIds);
        
        // 参数校验
        if (userId == null || CollectionUtils.isEmpty(itemIds)) {
            return Result.fail("参数不完整");
        }

        // 获取用户购物车
        ShoppingCart cart = getUserCart(userId);
        if (cart == null) {
            return Result.ok();
        }

        // 验证所有购物车项是否属于用户
        List<ShoppingCartItem> items = shoppingCartItemService.listByIds(itemIds);
        for (ShoppingCartItem item : items) {
            if (!item.getCartId().equals(cart.getId())) {
                return Result.fail("存在无权操作的购物车项");
            }
        }

        // 批量删除
        boolean removed = shoppingCartItemService.removeByIds(itemIds);
        if (!removed) {
            return Result.fail("删除失败");
        }

        // 更新购物车时间
        cart.setUpdatedTime(LocalDateTime.now());
        updateById(cart);

        return Result.ok();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result clearCart(Long userId) {
        log.info("清空购物车: userId={}", userId);
        
        // 参数校验
        if (userId == null) {
            return Result.fail("用户ID不能为空");
        }

        // 获取用户购物车
        ShoppingCart cart = getUserCart(userId);
        if (cart == null) {
            return Result.ok();
        }

        // 清空购物车项
        boolean cleared = shoppingCartItemService.clearItems(cart.getId());
        if (!cleared) {
            return Result.fail("清空失败");
        }

        // 更新购物车时间
        cart.setUpdatedTime(LocalDateTime.now());
        updateById(cart);

        return Result.ok();
    }

    @Override
    public Integer countItems(Long userId) {
        log.info("获取购物车商品数量: userId={}", userId);
        
        if (userId == null) {
            return 0;
        }

        // 获取用户购物车
        ShoppingCart cart = getUserCart(userId);
        if (cart == null) {
            return 0;
        }

        return shoppingCartItemService.countItems(cart.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result checkout(Long userId, Long cartId, List<Long> itemIds) {
        log.info("购物车结算: userId={}, cartId={}, itemIds={}", userId, cartId, itemIds);
        
        // 参数校验
        if (userId == null || cartId == null) {
            return Result.fail("参数不完整");
        }

        // 验证购物车所有权
        ShoppingCart cart = getById(cartId);
        if (cart == null || !cart.getUserId().equals(userId)) {
            return Result.fail("无权操作此购物车");
        }

        // 获取要结算的购物车项
        List<ShoppingCartItem> items;
        if (CollectionUtils.isEmpty(itemIds)) {
            // 结算所有选中的商品
            items = shoppingCartItemService.listSelectedByCartId(cartId);
        } else {
            // 结算指定的商品
            items = shoppingCartItemService.listByIds(itemIds);
            // 验证所有商品属于该购物车
            for (ShoppingCartItem item : items) {
                if (!item.getCartId().equals(cartId)) {
                    return Result.fail("存在不属于当前购物车的商品");
                }
            }
        }

        if (items.isEmpty()) {
            return Result.fail("没有可结算的商品");
        }

        // 验证商品状态和库存
        List<ShoppingCartItem> invalidItems = validateItems(userId, cartId);
        if (!invalidItems.isEmpty()) {
            return Result.fail("存在无效商品，请重新选择");
        }

        // TODO: 创建订单
        // 这里应该调用订单服务创建订单
        // 这部分逻辑需要根据具体的订单服务实现来完成

        // 标记购物车为已下单状态
        cart.setStatus(CartStatus.ORDERED.getCode());
        cart.setUpdatedTime(LocalDateTime.now());
        updateById(cart);

        // 返回订单ID
        Long orderId = null; // 实际应该是创建的订单ID
        return Result.ok(orderId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result mergeCart(Long userId, String sessionId) {
        log.info("合并购物车: userId={}, sessionId={}", userId, sessionId);
        
        // 参数校验
        if (userId == null || sessionId == null || sessionId.trim().isEmpty()) {
            return Result.fail("参数不完整");
        }

        return tempCartService.mergeToUserCart(userId, sessionId);
    }

    @Override
    public Result getCartByShop(Long userId) {
        log.info("按店铺分组获取购物车: userId={}", userId);
        
        // 参数校验
        if (userId == null) {
            return Result.fail("用户ID不能为空");
        }

        // 获取用户购物车
        ShoppingCart cart = getUserCart(userId);
        if (cart == null || cart.getItems() == null || cart.getItems().isEmpty()) {
            return Result.ok(new ArrayList<>());
        }

        // 按店铺分组
        Map<Long, List<ShoppingCartItem>> itemsByShop = shoppingCartItemService.groupByShopId(cart.getId());
        
        // 构建返回结果
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<Long, List<ShoppingCartItem>> entry : itemsByShop.entrySet()) {
            Map<String, Object> shopCart = new HashMap<>();
            shopCart.put("shopId", entry.getKey());
            
            // 查询店铺信息
            // 这里需要调用店铺服务获取店铺名称等信息
            // shopCart.put("shopName", shopService.getById(entry.getKey()).getName());
            
            shopCart.put("items", entry.getValue());
            
            // 计算店铺商品总价
            BigDecimal shopTotal = BigDecimal.ZERO;
            for (ShoppingCartItem item : entry.getValue()) {
                if (item.getSelected() != null && item.getSelected() == 1) {
                    shopTotal = shopTotal.add(item.getSubtotal());
                }
            }
            shopCart.put("total", shopTotal);
            
            result.add(shopCart);
        }

        return Result.ok(result);
    }

    @Override
    public Result buyNow(Long userId, Long productId, Integer productType, Integer quantity, Long skuId) {
        log.info("立即购买: userId={}, productId={}, productType={}, quantity={}, skuId={}",
                userId, productId, productType, quantity, skuId);
        
        // 参数校验
        if (userId == null || productId == null || productType == null || quantity == null || quantity <= 0) {
            return Result.fail("参数不完整或无效");
        }

        // 验证商品信息
        Map<String, Object> productInfo = productService.getProductInfo(productId, skuId);
        if (productInfo == null) {
            return Result.fail("商品不存在");
        }

        boolean onSale = (boolean) productInfo.get("onSale");
        if (!onSale) {
            return Result.fail("商品已下架");
        }

        Integer stock = (Integer) productInfo.get("stock");
        if (stock < quantity) {
            return Result.fail("商品库存不足");
        }

        // 创建临时购物车项（不存入数据库，仅用于结算）
        ShoppingCartItem tempItem = new ShoppingCartItem();
        tempItem.setProductId(productId);
        tempItem.setProductName((String) productInfo.get("name"));
        tempItem.setProductImage((String) productInfo.get("imageUrl"));
        tempItem.setProductType(productType);
        tempItem.setSkuId(skuId);
        tempItem.setSkuName((String) productInfo.get("skuName"));
        tempItem.setPrice((BigDecimal) productInfo.get("price"));
        tempItem.setQuantity(quantity);
        tempItem.setSelected(1); // 默认选中

        // 计算总价
        BigDecimal subtotal = tempItem.getPrice().multiply(new BigDecimal(quantity));

        // 构建返回结果
        Map<String, Object> result = new HashMap<>();
        result.put("item", tempItem);
        result.put("subtotal", subtotal);
        // 这里可以加入其他需要的信息，如配送费、促销信息等

        return Result.ok(result);
    }

    @Override
    public List<ShoppingCartItem> validateItems(Long userId, Long cartId) {
        log.info("验证购物车商品: userId={}, cartId={}", userId, cartId);
        
        // 验证购物车所有权
        if (!validateCartOwnership(userId, cartId)) {
            return new ArrayList<>();
        }

        return shoppingCartItemService.validateCartItems(cartId);
    }

    @Override
    public Result getSettlementInfo(Long userId, Long cartId, List<Long> itemIds) {
        log.info("获取结算信息: userId={}, cartId={}, itemIds={}", userId, cartId, itemIds);
        
        // 参数校验
        if (userId == null || cartId == null) {
            return Result.fail("参数不完整");
        }

        // 验证购物车所有权
        if (!validateCartOwnership(userId, cartId)) {
            return Result.fail("无权操作此购物车");
        }

        // 获取要结算的购物车项
        List<ShoppingCartItem> items;
        if (CollectionUtils.isEmpty(itemIds)) {
            // 结算所有选中的商品
            items = shoppingCartItemService.listSelectedByCartId(cartId);
        } else {
            // 结算指定的商品
            items = shoppingCartItemService.listByIds(itemIds);
            // 验证所有商品属于该购物车
            for (ShoppingCartItem item : items) {
                if (!item.getCartId().equals(cartId)) {
                    return Result.fail("存在不属于当前购物车的商品");
                }
            }
        }

        if (items.isEmpty()) {
            return Result.fail("没有可结算的商品");
        }

        // 验证商品状态和库存
        List<ShoppingCartItem> invalidItems = shoppingCartItemService.validateCartItems(cartId);
        if (!invalidItems.isEmpty()) {
            return Result.fail("存在无效商品，请重新选择");
        }

        // 计算商品总价
        BigDecimal totalAmount = BigDecimal.ZERO;
        for (ShoppingCartItem item : items) {
            totalAmount = totalAmount.add(item.getSubtotal());
        }

        // 获取促销信息和优惠金额
        BigDecimal discountAmount = cartPromotionService.calculateTotalDiscount(cartId);

        // 构建结算信息
        ShoppingCartDTO settlementInfo = new ShoppingCartDTO();
        settlementInfo.setId(cartId);
        settlementInfo.setTotalAmount(totalAmount);
        settlementInfo.setDiscountAmount(discountAmount);
        settlementInfo.setPayAmount(totalAmount.subtract(discountAmount));

        return Result.ok(settlementInfo);
    }

    @Override
    public boolean validateCartOwnership(Long userId, Long cartId) {
        if (userId == null || cartId == null) {
            return false;
        }

        ShoppingCart cart = getById(cartId);
        return cart != null && userId.equals(cart.getUserId());
    }
}