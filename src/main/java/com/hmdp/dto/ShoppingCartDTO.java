package com.hmdp.dto;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 购物车数据传输对象
 */
@Data
public class ShoppingCartDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 购物车ID
     */
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 创建时间
     */
    private LocalDateTime createdTime;

    /**
     * 更新时间
     */
    private LocalDateTime updatedTime;

    /**
     * 购物车状态：1-正常 2-已下单 3-已过期
     */
    private Integer status;

    /**
     * 购物车项列表
     */
    private List<CartItemDTO> items = new ArrayList<>();

    /**
     * 购物车总金额
     */
    private BigDecimal totalAmount;

    /**
     * 优惠金额
     */
    private BigDecimal discountAmount = BigDecimal.ZERO;

    /**
     * 实付金额
     */
    private BigDecimal payAmount;

    /**
     * 商品总数量
     */
    private Integer totalQuantity;

    /**
     * 被选中的商品数量
     */
    private Integer selectedQuantity;

    /**
     * 按店铺分组的购物车项
     */
    private Map<Long, List<CartItemDTO>> itemsByShop;

    /**
     * 计算总金额
     */
    public BigDecimal calculateTotalAmount() {
        if (items == null || items.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return items.stream()
                .filter(item -> item.getSelected() != null && item.getSelected())
                .map(item -> item.getPrice().multiply(new BigDecimal(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * 计算商品总数量
     */
    public Integer calculateTotalQuantity() {
        if (items == null || items.isEmpty()) {
            return 0;
        }
        return items.stream()
                .mapToInt(CartItemDTO::getQuantity)
                .sum();
    }

    /**
     * 计算被选中的商品数量
     */
    public Integer calculateSelectedQuantity() {
        if (items == null || items.isEmpty()) {
            return 0;
        }
        return items.stream()
                .filter(item -> item.getSelected() != null && item.getSelected())
                .mapToInt(CartItemDTO::getQuantity)
                .sum();
    }

    /**
     * 按店铺分组购物车项
     */
    public Map<Long, List<CartItemDTO>> groupItemsByShop() {
        if (items == null || items.isEmpty()) {
            return Map.of();
        }
        return items.stream()
                .filter(item -> item.getShopId() != null)
                .collect(Collectors.groupingBy(CartItemDTO::getShopId));
    }

    /**
     * 更新计算属性
     */
    public ShoppingCartDTO refreshCalculations() {
        this.totalAmount = calculateTotalAmount();
        this.payAmount = this.totalAmount.subtract(this.discountAmount);
        this.totalQuantity = calculateTotalQuantity();
        this.selectedQuantity = calculateSelectedQuantity();
        this.itemsByShop = groupItemsByShop();
        return this;
    }
}