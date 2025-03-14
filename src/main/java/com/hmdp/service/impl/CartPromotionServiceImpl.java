package com.hmdp.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.Result;
import com.hmdp.entity.CartPromotion;
import com.hmdp.entity.ShoppingCart;
import com.hmdp.entity.ShoppingCartItem;
import com.hmdp.mapper.CartPromotionMapper;
import com.hmdp.service.CartPromotionService;
import com.hmdp.service.ShoppingCartItemService;
import com.hmdp.service.ShoppingCartService;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 购物车促销服务实现类
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class CartPromotionServiceImpl extends ServiceImpl<CartPromotionMapper, CartPromotion> implements CartPromotionService {

    private final CartPromotionMapper cartPromotionMapper;
    private final ShoppingCartService shoppingCartService;
    private final ShoppingCartItemService shoppingCartItemService;

    // 促销类型常量
    private static final int PROMOTION_TYPE_FULL_REDUCTION = 1; // 满减
    private static final int PROMOTION_TYPE_FULL_DISCOUNT = 2;  // 满折
    private static final int PROMOTION_TYPE_DIRECT_REDUCTION = 3; // 直减

    @Override
    public List<CartPromotion> listByCartId(Long cartId) {
        log.info("查询购物车关联的所有促销活动: cartId={}", cartId);
        return cartPromotionMapper.listByCartId(cartId);
    }

    @Override
    public List<CartPromotion> listByType(Long cartId, Integer promotionType) {
        log.info("查询购物车关联的特定类型促销活动: cartId={}, promotionType={}", cartId, promotionType);
        return cartPromotionMapper.listByType(cartId, promotionType);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result addPromotion(Long cartId, Long promotionId, Integer promotionType, BigDecimal discountAmount) {
        log.info("添加促销活动到购物车: cartId={}, promotionId={}, promotionType={}, discountAmount={}",
                cartId, promotionId, promotionType, discountAmount);

        // 校验参数
        if (cartId == null || promotionId == null || promotionType == null) {
            return Result.fail("参数不完整");
        }

        // 验证购物车存在
        ShoppingCart cart = shoppingCartService.getById(cartId);
        if (cart == null) {
            return Result.fail("购物车不存在");
        }

        // 验证金额有效性
        if (discountAmount == null || discountAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return Result.fail("优惠金额必须大于0");
        }

        // 检查是否已存在相同促销活动
        if (cartPromotionMapper.hasPromotion(cartId, promotionId) > 0) {
            return Result.fail("该促销活动已添加");
        }

        // 创建促销关联
        CartPromotion promotion = new CartPromotion();
        promotion.setCartId(cartId);
        promotion.setPromotionId(promotionId);
        promotion.setPromotionType(promotionType);
        promotion.setDiscountAmount(discountAmount);
        promotion.setCreatedTime(LocalDateTime.now());

        // 保存促销关联
        boolean success = save(promotion);
        if (!success) {
            return Result.fail("添加促销活动失败");
        }

        return Result.ok(promotion.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result removePromotion(Long cartId, Long promotionId) {
        log.info("从购物车移除促销活动: cartId={}, promotionId={}", cartId, promotionId);

        // 校验参数
        if (cartId == null || promotionId == null) {
            return Result.fail("参数不完整");
        }

        // 删除促销关联
        int rows = cartPromotionMapper.deletePromotion(cartId, promotionId);
        if (rows == 0) {
            return Result.fail("移除失败，促销活动不存在");
        }

        return Result.ok("促销活动已移除");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result clearPromotions(Long cartId) {
        log.info("清空购物车所有促销关联: cartId={}", cartId);

        // 校验参数
        if (cartId == null) {
            return Result.fail("参数不完整");
        }

        // 删除所有促销关联
        int rows = cartPromotionMapper.clearPromotions(cartId);

        return Result.ok("已清除" + rows + "个促销活动");
    }

    @Override
    public BigDecimal calculateTotalDiscount(Long cartId) {
        log.info("计算购物车所有促销活动优惠总额: cartId={}", cartId);

        if (cartId == null) {
            return BigDecimal.ZERO;
        }

        return cartPromotionMapper.calculateTotalDiscount(cartId);
    }

    @Override
    public boolean hasPromotion(Long cartId, Long promotionId) {
        log.info("检查购物车是否包含指定促销活动: cartId={}, promotionId={}", cartId, promotionId);

        if (cartId == null || promotionId == null) {
            return false;
        }

        return cartPromotionMapper.hasPromotion(cartId, promotionId) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result applyFullReduction(Long cartId) {
        log.info("应用满减促销: cartId={}", cartId);

        // 校验参数
        if (cartId == null) {
            return Result.fail("参数不完整");
        }

        // 获取购物车信息
        ShoppingCart cart = shoppingCartService.getById(cartId);
        if (cart == null) {
            return Result.fail("购物车不存在");
        }

        // 获取购物车中选中的商品项
        List<ShoppingCartItem> selectedItems = shoppingCartItemService.listSelectedByCartId(cartId);
        if (selectedItems.isEmpty()) {
            return Result.fail("没有选中商品");
        }

        // 计算总金额
        BigDecimal totalAmount = selectedItems.stream()
                .map(ShoppingCartItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 获取可用的满减规则（实际项目中应从促销规则表中查询）
        List<FullReductionRule> rules = getAvailableFullReductionRules();

        // 根据总金额选择适用的满减规则
        FullReductionRule applicableRule = null;
        for (FullReductionRule rule : rules) {
            if (totalAmount.compareTo(rule.getFullAmount()) >= 0) {
                if (applicableRule == null || rule.getFullAmount().compareTo(applicableRule.getFullAmount()) > 0) {
                    applicableRule = rule;
                }
            }
        }

        if (applicableRule == null) {
            return Result.fail("未达到满减条件");
        }

        // 创建促销关联
        CartPromotion promotion = new CartPromotion();
        promotion.setCartId(cartId);
        promotion.setPromotionId(applicableRule.getId());
        promotion.setPromotionType(PROMOTION_TYPE_FULL_REDUCTION);
        promotion.setDiscountAmount(applicableRule.getReductionAmount());
        promotion.setCreatedTime(LocalDateTime.now());

        // 先删除已有的满减类型促销
        cartPromotionMapper.clearPromotionsOfType(cartId, PROMOTION_TYPE_FULL_REDUCTION);

        // 保存新的满减促销
        boolean success = save(promotion);
        if (!success) {
            return Result.fail("应用满减促销失败");
        }

        return Result.ok("已应用满" + applicableRule.getFullAmount() + "减" + applicableRule.getReductionAmount() + "促销");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result applyDiscount(Long cartId) {
        log.info("应用满折促销: cartId={}", cartId);

        // 校验参数
        if (cartId == null) {
            return Result.fail("参数不完整");
        }

        // 获取购物车信息
        ShoppingCart cart = shoppingCartService.getById(cartId);
        if (cart == null) {
            return Result.fail("购物车不存在");
        }

        // 获取购物车中选中的商品项
        List<ShoppingCartItem> selectedItems = shoppingCartItemService.listSelectedByCartId(cartId);
        if (selectedItems.isEmpty()) {
            return Result.fail("没有选中商品");
        }

        // 计算总金额
        BigDecimal totalAmount = selectedItems.stream()
                .map(ShoppingCartItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 获取可用的满折规则（实际项目中应从促销规则表中查询）
        List<FullDiscountRule> rules = getAvailableFullDiscountRules();

        // 根据总金额选择适用的满折规则
        FullDiscountRule applicableRule = null;
        for (FullDiscountRule rule : rules) {
            if (totalAmount.compareTo(rule.getFullAmount()) >= 0) {
                if (applicableRule == null || rule.getFullAmount().compareTo(applicableRule.getFullAmount()) > 0) {
                    applicableRule = rule;
                }
            }
        }

        if (applicableRule == null) {
            return Result.fail("未达到满折条件");
        }

        // 计算折扣金额
        BigDecimal discountRate = applicableRule.getDiscountRate();
        BigDecimal discountAmount = totalAmount.multiply(BigDecimal.ONE.subtract(discountRate))
                .setScale(2, RoundingMode.HALF_UP);

        // 创建促销关联
        CartPromotion promotion = new CartPromotion();
        promotion.setCartId(cartId);
        promotion.setPromotionId(applicableRule.getId());
        promotion.setPromotionType(PROMOTION_TYPE_FULL_DISCOUNT);
        promotion.setDiscountAmount(discountAmount);
        promotion.setCreatedTime(LocalDateTime.now());

        // 先删除已有的满折类型促销
        cartPromotionMapper.clearPromotionsOfType(cartId, PROMOTION_TYPE_FULL_DISCOUNT);

        // 保存新的满折促销
        boolean success = save(promotion);
        if (!success) {
            return Result.fail("应用满折促销失败");
        }

        return Result.ok("已应用满" + applicableRule.getFullAmount() + "打" + applicableRule.getDiscountRate().multiply(new BigDecimal(10)) + "折促销");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result applyDirectReduction(Long cartId, List<Long> itemIds) {
        log.info("应用商品直减促销: cartId={}, itemIds={}", cartId, itemIds);

        // 校验参数
        if (cartId == null || itemIds == null || itemIds.isEmpty()) {
            return Result.fail("参数不完整");
        }

        // 获取购物车信息
        ShoppingCart cart = shoppingCartService.getById(cartId);
        if (cart == null) {
            return Result.fail("购物车不存在");
        }

        // 获取指定的购物车项
        List<ShoppingCartItem> items = shoppingCartItemService.listByIds(itemIds);
        if (items.isEmpty()) {
            return Result.fail("指定的商品不存在");
        }

        // 验证所有购物车项属于当前购物车
        for (ShoppingCartItem item : items) {
            if (!item.getCartId().equals(cartId)) {
                return Result.fail("存在不属于当前购物车的商品");
            }
        }

        // 计算直减金额（实际项目中应根据具体产品的促销规则计算）
        BigDecimal totalDiscount = BigDecimal.ZERO;

        for (ShoppingCartItem item : items) {
            // 示例：假设每个商品有5元直减
            BigDecimal itemDiscount = new BigDecimal("5").multiply(new BigDecimal(item.getQuantity()));
            totalDiscount = totalDiscount.add(itemDiscount);
        }

        // 创建促销关联
        CartPromotion promotion = new CartPromotion();
        promotion.setCartId(cartId);
        promotion.setPromotionId(1L); // 假设直减活动ID为1，实际应从活动表获取
        promotion.setPromotionType(PROMOTION_TYPE_DIRECT_REDUCTION);
        promotion.setDiscountAmount(totalDiscount);
        promotion.setCreatedTime(LocalDateTime.now());

        // 先删除已有的直减类型促销
        cartPromotionMapper.clearPromotionsOfType(cartId, PROMOTION_TYPE_DIRECT_REDUCTION);

        // 保存新的直减促销
        boolean success = save(promotion);
        if (!success) {
            return Result.fail("应用直减促销失败");
        }

        return Result.ok("已应用直减" + totalDiscount + "元促销");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result applyBestPromotion(Long cartId) {
        log.info("自动应用最优促销方案: cartId={}", cartId);

        // 校验参数
        if (cartId == null) {
            return Result.fail("参数不完整");
        }

        // 获取购物车信息
        ShoppingCart cart = shoppingCartService.getById(cartId);
        if (cart == null) {
            return Result.fail("购物车不存在");
        }

        // 尝试应用各种促销方式并获取优惠金额
        List<PromotionResult> results = new ArrayList<>();

        try {
            // 先清除所有已有促销
            clearPromotions(cartId);

            // 尝试满减
            Result fullReductionResult = applyFullReduction(cartId);
            if (fullReductionResult.getSuccess()) {
                BigDecimal discount = calculateTotalDiscount(cartId);
                results.add(new PromotionResult(PROMOTION_TYPE_FULL_REDUCTION, discount, fullReductionResult.getData()));
                clearPromotions(cartId);
            }

            // 尝试满折
            Result discountResult = applyDiscount(cartId);
            if (discountResult.getSuccess()) {
                BigDecimal discount = calculateTotalDiscount(cartId);
                results.add(new PromotionResult(PROMOTION_TYPE_FULL_DISCOUNT, discount, discountResult.getData()));
                clearPromotions(cartId);
            }

            // 尝试直减（针对所有商品）
            List<ShoppingCartItem> items = shoppingCartItemService.listSelectedByCartId(cartId);
            if (!items.isEmpty()) {
                List<Long> itemIds = items.stream().map(ShoppingCartItem::getId).collect(Collectors.toList());
                Result directResult = applyDirectReduction(cartId, itemIds);
                if (directResult.getSuccess()) {
                    BigDecimal discount = calculateTotalDiscount(cartId);
                    results.add(new PromotionResult(PROMOTION_TYPE_DIRECT_REDUCTION, discount, directResult.getData()));
                    clearPromotions(cartId);
                }
            }

            // 找出最优的促销方案
            if (results.isEmpty()) {
                return Result.fail("没有符合条件的促销活动");
            }

            // 根据优惠金额排序，选择最优的
            results.sort(Comparator.comparing(PromotionResult::getDiscountAmount).reversed());
            PromotionResult bestPromotion = results.get(0);

            // 应用最优促销方案
            switch (bestPromotion.getType()) {
                case PROMOTION_TYPE_FULL_REDUCTION:
                    applyFullReduction(cartId);
                    break;
                case PROMOTION_TYPE_FULL_DISCOUNT:
                    applyDiscount(cartId);
                    break;
                case PROMOTION_TYPE_DIRECT_REDUCTION:
                    List<Long> allItemIds = items.stream().map(ShoppingCartItem::getId).collect(Collectors.toList());
                    applyDirectReduction(cartId, allItemIds);
                    break;
                default:
                    return Result.fail("未知促销类型");
            }

            return Result.ok("已应用最优促销方案: " + bestPromotion.getMessage());

        } catch (Exception e) {
            log.error("应用最优促销方案异常", e);
            return Result.fail("应用促销方案失败: " + e.getMessage());
        }
    }

    /**
     * 获取可用的满减规则
     * 实际应从数据库中查询促销规则
     */
    private List<FullReductionRule> getAvailableFullReductionRules() {
        // 模拟数据，实际应从数据库获取
        List<FullReductionRule> rules = new ArrayList<>();
        rules.add(new FullReductionRule(1L, new BigDecimal("100"), new BigDecimal("10")));
        rules.add(new FullReductionRule(2L, new BigDecimal("200"), new BigDecimal("25")));
        rules.add(new FullReductionRule(3L, new BigDecimal("300"), new BigDecimal("50")));
        return rules;
    }

    /**
     * 获取可用的满折规则
     * 实际应从数据库中查询促销规则
     */
    private List<FullDiscountRule> getAvailableFullDiscountRules() {
        // 模拟数据，实际应从数据库获取
        List<FullDiscountRule> rules = new ArrayList<>();
        rules.add(new FullDiscountRule(4L, new BigDecimal("100"), new BigDecimal("0.9"))); // 满100打9折
        rules.add(new FullDiscountRule(5L, new BigDecimal("200"), new BigDecimal("0.8"))); // 满200打8折
        rules.add(new FullDiscountRule(6L, new BigDecimal("300"), new BigDecimal("0.7"))); // 满300打7折
        return rules;
    }

    /**
     * 满减规则内部类
     */
    @Data
    @AllArgsConstructor
    private static class FullReductionRule {
        private Long id;
        private BigDecimal fullAmount;
        private BigDecimal reductionAmount;
    }

    /**
     * 满折规则内部类
     */
    @Data
    @AllArgsConstructor
    private static class FullDiscountRule {
        private Long id;
        private BigDecimal fullAmount;
        private BigDecimal discountRate; // 折扣率，例如0.9表示9折
    }

    /**
     * 促销结果内部类
     */
    @Data
    @AllArgsConstructor
    private static class PromotionResult {
        private Integer type;
        private BigDecimal discountAmount;
        private Object message;
    }
}