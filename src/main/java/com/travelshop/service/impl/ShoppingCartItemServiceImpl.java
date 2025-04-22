package com.travelshop.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.travelshop.entity.ShoppingCartItem;
import com.travelshop.mapper.ShoppingCartItemMapper;
import com.travelshop.service.IProductService;
import com.travelshop.service.ShoppingCartItemService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
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
    public boolean removeItems(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return false;
        }
        return removeByIds(ids);
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
    public boolean updateSelected(Long cartId, List<Long> itemIds, Boolean selected) {
        try {
            // 将布尔类型转换为整数（0-未选中，1-已选中）
            int selectedValue = selected ? 1 : 0;
            if (CollectionUtils.isEmpty(itemIds)) {
                // 如果未指定购物车项，则更新该购物车下的所有项
                return baseMapper.updateAllSelected(cartId, selectedValue) >= 0;
            } else {
                // 如果指定了购物车项ID列表，则只更新这些项
                // 首先验证这些购物车项是否属于指定的购物车
                QueryWrapper<ShoppingCartItem> queryWrapper = new QueryWrapper<>();
                queryWrapper.eq("cart_id", cartId).in("id", itemIds);
                List<ShoppingCartItem> items = baseMapper.selectList(queryWrapper);

                if (items.size() != itemIds.size()) {
                    // 有购物车项不属于指定的购物车
                    log.warn("部分购物车项不属于购物车: cartId={}, itemIds={}", cartId, itemIds);
                    return false;
                }

                // 逐个更新选中状态
                int count = 0;
                for (Long itemId : itemIds) {
                    count += baseMapper.updateSelected(itemId, selectedValue);
                }
                return count == itemIds.size();
            }
        } catch (Exception e) {
            log.error("更新购物车项选中状态失败: cartId={}, itemIds={}, selected={}",
                    cartId, itemIds, selected, e);
            return false;
        }
    }

}