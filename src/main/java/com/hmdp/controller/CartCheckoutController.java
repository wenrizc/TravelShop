package com.hmdp.controller;

import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
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

}