package com.hmdp.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.entity.ShoppingCartItem;
import com.hmdp.mapper.ShoppingCartItemMapper;
import com.hmdp.service.IProductService;
import com.hmdp.service.ShoppingCartItemService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 购物车项服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ShoppingCartItemServiceImpl extends ServiceImpl<ShoppingCartItemMapper, ShoppingCartItem> implements ShoppingCartItemService {

    private final ShoppingCartItemMapper shoppingCartItemMapper;
    private final IProductService productService;

    @Override
    public List<ShoppingCartItem> listByCartId(Long cartId) {
        if (cartId == null) {
            return new ArrayList<>();
        }
        List<ShoppingCartItem> items = shoppingCartItemMapper.listByCartId(cartId);
        // 计算每个购物车项的小计金额
        items.forEach(item -> {
            if (item.getPrice() != null && item.getQuantity() != null) {
                item.setSubtotal(item.getPrice().multiply(new BigDecimal(item.getQuantity())));
            }
        });
        return items;
    }

    @Override
    public List<ShoppingCartItem> listSelectedByCartId(Long cartId) {
        if (cartId == null) {
            return new ArrayList<>();
        }
        List<ShoppingCartItem> items = shoppingCartItemMapper.listSelectedByCartId(cartId);
        // 计算每个购物车项的小计金额
        items.forEach(item -> {
            if (item.getPrice() != null && item.getQuantity() != null) {
                item.setSubtotal(item.getPrice().multiply(new BigDecimal(item.getQuantity())));
            }
        });
        return items;
    }

    @Override
    public ShoppingCartItem getByProductInfo(Long cartId, Long productId, Integer productType, Long skuId) {
        if (cartId == null || productId == null || productType == null) {
            return null;
        }
        return shoppingCartItemMapper.getByProductInfo(cartId, productId, productType, skuId);
    }

    @Override
    public boolean addItem(ShoppingCartItem item) {
        if (item == null) {
            return false;
        }
        // 设置创建和更新时间
        if (item.getCreatedTime() == null) {
            item.setCreatedTime(LocalDateTime.now());
        }
        if (item.getUpdatedTime() == null) {
            item.setUpdatedTime(LocalDateTime.now());
        }
        return save(item);
    }

    @Override
    public boolean updateQuantity(Long id, Integer quantity) {
        if (id == null || quantity == null || quantity <= 0) {
            return false;
        }
        LambdaUpdateWrapper<ShoppingCartItem> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(ShoppingCartItem::getId, id)
                .set(ShoppingCartItem::getQuantity, quantity)
                .set(ShoppingCartItem::getUpdatedTime, LocalDateTime.now());
        return update(wrapper);
    }

    @Override
    public boolean updateSelected(Long id, Boolean selected) {
        if (id == null || selected == null) {
            return false;
        }
        LambdaUpdateWrapper<ShoppingCartItem> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(ShoppingCartItem::getId, id)
                .set(ShoppingCartItem::getSelected, selected ? 1 : 0)
                .set(ShoppingCartItem::getUpdatedTime, LocalDateTime.now());
        return update(wrapper);
    }

    @Override
    public boolean updateAllSelected(Long cartId, Boolean selected) {
        if (cartId == null || (selected != false && selected != true)) {
            return false;
        }
        LambdaUpdateWrapper<ShoppingCartItem> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(ShoppingCartItem::getCartId, cartId)
                .set(ShoppingCartItem::getSelected, selected)
                .set(ShoppingCartItem::getUpdatedTime, LocalDateTime.now());
        return update(wrapper);    }


    @Override
    public boolean removeItem(Long id) {
        if (id == null) {
            return false;
        }
        return removeById(id);
    }

    @Override
    public boolean removeItems(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return false;
        }
        return removeByIds(ids);
    }

    @Override
    public boolean clearItems(Long cartId) {
        if (cartId == null) {
            return false;
        }
        LambdaQueryWrapper<ShoppingCartItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ShoppingCartItem::getCartId, cartId);
        return remove(wrapper);
    }

    @Override
    public Map<Long, List<ShoppingCartItem>> groupByShopId(Long cartId) {
        if (cartId == null) {
            return new HashMap<>();
        }
        
        // 查询购物车所有项
        List<ShoppingCartItem> items = listByCartId(cartId);
        if (items.isEmpty()) {
            return new HashMap<>();
        }
        
        // 从商品信息中获取店铺ID并分组
        Map<Long, List<ShoppingCartItem>> result = new HashMap<>();
        for (ShoppingCartItem item : items) {
            // 获取商品信息（包含店铺信息）
            Map<String, Object> productInfo = productService.getProductInfo(item.getProductId(), item.getSkuId());
            if (productInfo != null && productInfo.containsKey("shopId")) {
                Long shopId = (Long) productInfo.get("shopId");
                if (!result.containsKey(shopId)) {
                    result.put(shopId, new ArrayList<>());
                }
                result.get(shopId).add(item);
            }
        }
        
        return result;
    }

    @Override
    public List<ShoppingCartItem> filterByProductType(Long cartId, Integer productType) {
        if (cartId == null || productType == null) {
            return new ArrayList<>();
        }
        return shoppingCartItemMapper.listByProductType(cartId, productType);
    }

    @Override
    public List<ShoppingCartItem> validateCartItems(Long cartId) {
        if (cartId == null) {
            return new ArrayList<>();
        }
        
        List<ShoppingCartItem> invalidItems = new ArrayList<>();
        List<ShoppingCartItem> items = listSelectedByCartId(cartId);
        
        for (ShoppingCartItem item : items) {
            // 验证商品状态和库存
            Map<String, Object> productInfo = productService.getProductInfo(item.getProductId(), item.getSkuId());
            if (productInfo == null) {
                // 商品不存在
                invalidItems.add(item);
                continue;
            }
            
            boolean onSale = (boolean) productInfo.get("onSale");
            if (!onSale) {
                // 商品已下架
                invalidItems.add(item);
                continue;
            }
            
            Integer stock = (Integer) productInfo.get("stock");
            if (stock < item.getQuantity()) {
                // 库存不足
                invalidItems.add(item);
            }
        }
        
        return invalidItems;
    }

    @Override
    public BigDecimal calculateSelectedItemsTotal(Long cartId) {
        if (cartId == null) {
            return BigDecimal.ZERO;
        }
        
        List<ShoppingCartItem> selectedItems = listSelectedByCartId(cartId);
        if (selectedItems.isEmpty()) {
            return BigDecimal.ZERO;
        }
        
        BigDecimal total = BigDecimal.ZERO;
        for (ShoppingCartItem item : selectedItems) {
            if (item.getSubtotal() != null) {
                total = total.add(item.getSubtotal());
            } else if (item.getPrice() != null && item.getQuantity() != null) {
                total = total.add(item.getPrice().multiply(new BigDecimal(item.getQuantity())));
            }
        }
        
        return total;
    }
    
    @Override
    public Integer countItems(Long cartId) {
        if (cartId == null) {
            return 0;
        }
        
        LambdaQueryWrapper<ShoppingCartItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ShoppingCartItem::getCartId, cartId);
        return Math.toIntExact(count(wrapper));
    }
}