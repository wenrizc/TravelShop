package com.hmdp.controller.user;

import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.service.ShoppingCartService;
import com.hmdp.utils.UserHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;


/**
 * 购物车控制器
 */
@Slf4j
@RestController
@RequestMapping("/cart")
@RequiredArgsConstructor
public class ShoppingCartController {

    private final ShoppingCartService shoppingCartService;

    /**
     * 获取购物车信息
     */
    @GetMapping
    public Result getCart() {
        UserDTO user = UserHolder.getUser();
        return Result.ok(shoppingCartService.getUserCart(user.getId()));
    }

    /**
     * 添加商品到购物车
     */
    @PostMapping("/add")
    public Result addToCart(
            @RequestParam("productId") Long productId,
            @RequestParam("productType") Integer productType,
            @RequestParam("quantity") Integer quantity,
            @RequestParam(value = "skuId", required = false) Long skuId) {
        UserDTO user = UserHolder.getUser();
        return shoppingCartService.addItem(user.getId(), productId, productType, quantity, skuId);
    }

    /**
     * 更新购物车项数量
     */
    @PutMapping("/items/{itemId}/quantity")
    public Result updateQuantity(
            @PathVariable("itemId") Long itemId,
            @RequestParam("quantity") Integer quantity) {
        UserDTO user = UserHolder.getUser();
        return shoppingCartService.updateItemQuantity(user.getId(), itemId, quantity);
    }


    /**
     * 删除购物车项
     */
    @DeleteMapping("/items/{itemId}")
    public Result removeCartItem(@PathVariable("itemId") Long itemId) {
        UserDTO user = UserHolder.getUser();
        return shoppingCartService.removeItem(user.getId(), itemId);
    }

    /**
     * 立即购买
     */
    @PostMapping("/buy-now")
    public Result buyNow(
            @RequestParam("productId") Long productId,
            @RequestParam("productType") Integer productType,
            @RequestParam("quantity") Integer quantity,
            @RequestParam(value = "skuId", required = false) Long skuId) {
        UserDTO user = UserHolder.getUser();
        return shoppingCartService.buyNow(user.getId(), productId, productType, quantity, skuId);
    }

    /**
     * 添加选中商品到购物车
     */
    @PostMapping("/checkout/{cartId}")
    public Result checkout(@PathVariable("cartId") Long cartId, @RequestParam("itemIds") List<Long> itemIds) {
        UserDTO user = UserHolder.getUser();
        return shoppingCartService.checkout(user.getId(), cartId, itemIds);
    }

}