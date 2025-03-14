package com.hmdp.controller;

import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.service.CartPromotionService;
import com.hmdp.service.ShoppingCartService;
import com.hmdp.utils.UserHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 购物车结算控制器
 */
@Slf4j
@RestController
@RequestMapping("/cart/checkout")
@RequiredArgsConstructor
public class CartCheckoutController {

    private final ShoppingCartService shoppingCartService;
    private final CartPromotionService cartPromotionService;

    /**
     * 结算购物车
     * @param cartId 购物车ID
     * @param itemIds 要结算的购物车项ID列表（可选）
     * @return 创建的订单ID
     */
    @PostMapping
    public Result checkout(
            @RequestParam("cartId") Long cartId,
            @RequestParam(value = "itemIds", required = false) List<Long> itemIds) {
        UserDTO user = UserHolder.getUser();
        return shoppingCartService.checkout(user.getId(), cartId, itemIds);
    }

    /**
     * 结算前验证购物车商品
     * @param cartId 购物车ID
     * @return 无效商品列表
     */
    @GetMapping("/validate")
    public Result validateCartItems(@RequestParam("cartId") Long cartId) {
        UserDTO user = UserHolder.getUser();
        // 验证购物车所有权
        return Result.ok(shoppingCartService.validateItems(user.getId(), cartId));
    }

    /**
     * 获取结算页面数据
     * @param cartId 购物车ID
     * @param itemIds 要结算的购物车项ID列表（可选）
     * @return 结算页面所需数据
     */
    @GetMapping("/settlement")
    public Result getSettlementInfo(
            @RequestParam("cartId") Long cartId,
            @RequestParam(value = "itemIds", required = false) List<Long> itemIds) {
        UserDTO user = UserHolder.getUser();
        return shoppingCartService.getSettlementInfo(user.getId(), cartId, itemIds);
    }

    /**
     * 应用满减促销
     * @param cartId 购物车ID
     * @return 促销结果
     */
    @PostMapping("/promotion/full-reduction")
    public Result applyFullReduction(@RequestParam("cartId") Long cartId) {
        UserDTO user = UserHolder.getUser();
        // 验证购物车所有权
        if (!shoppingCartService.validateCartOwnership(user.getId(), cartId)) {
            return Result.fail("无权操作此购物车");
        }
        return cartPromotionService.applyFullReduction(cartId);
    }

    /**
     * 应用满折促销
     * @param cartId 购物车ID
     * @return 促销结果
     */
    @PostMapping("/promotion/discount")
    public Result applyDiscount(@RequestParam("cartId") Long cartId) {
        UserDTO user = UserHolder.getUser();
        // 验证购物车所有权
        if (!shoppingCartService.validateCartOwnership(user.getId(), cartId)) {
            return Result.fail("无权操作此购物车");
        }
        return cartPromotionService.applyDiscount(cartId);
    }

    /**
     * 应用直减促销
     * @param cartId 购物车ID
     * @param itemIds 要应用直减的购物车项ID列表
     * @return 促销结果
     */
    @PostMapping("/promotion/direct-reduction")
    public Result applyDirectReduction(
            @RequestParam("cartId") Long cartId,
            @RequestBody List<Long> itemIds) {
        UserDTO user = UserHolder.getUser();
        // 验证购物车所有权
        if (!shoppingCartService.validateCartOwnership(user.getId(), cartId)) {
            return Result.fail("无权操作此购物车");
        }
        return cartPromotionService.applyDirectReduction(cartId, itemIds);
    }

    /**
     * 应用最优促销方案
     * @param cartId 购物车ID
     * @return 促销结果
     */
    @PostMapping("/promotion/best")
    public Result applyBestPromotion(@RequestParam("cartId") Long cartId) {
        UserDTO user = UserHolder.getUser();
        // 验证购物车所有权
        if (!shoppingCartService.validateCartOwnership(user.getId(), cartId)) {
            return Result.fail("无权操作此购物车");
        }
        return cartPromotionService.applyBestPromotion(cartId);
    }

    /**
     * 清除购物车所有促销
     * @param cartId 购物车ID
     * @return 操作结果
     */
    @DeleteMapping("/promotion/clear")
    public Result clearPromotions(@RequestParam("cartId") Long cartId) {
        UserDTO user = UserHolder.getUser();
        // 验证购物车所有权
        if (!shoppingCartService.validateCartOwnership(user.getId(), cartId)) {
            return Result.fail("无权操作此购物车");
        }
        return cartPromotionService.clearPromotions(cartId);
    }
}