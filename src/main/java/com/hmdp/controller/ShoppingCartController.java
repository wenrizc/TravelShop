package com.hmdp.controller;

import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.service.ShoppingCartService;
import com.hmdp.service.TempCartService;
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
    private final TempCartService tempCartService;

    /**
     * 获取购物车信息
     */
    @GetMapping
    public Result getCart() {
        UserDTO user = UserHolder.getUser();
        return Result.ok(shoppingCartService.getUserCart(user.getId()));
    }

    /**
     * 获取购物车数量
     */
    @GetMapping("/count")
    public Result getCartCount() {
        UserDTO user = UserHolder.getUser();
        return Result.ok(shoppingCartService.countItems(user.getId()));
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
     * 更新购物车项选中状态
     */
    @PutMapping("/items/{itemId}/selected")
    public Result updateSelected(
            @PathVariable("itemId") Long itemId,
            @RequestParam("selected") Boolean selected) {
        UserDTO user = UserHolder.getUser();
        return shoppingCartService.updateItemSelected(user.getId(), itemId, selected);
    }

    /**
     * 全选/取消全选
     */
    @PutMapping("/selected")
    public Result selectAll(@RequestParam("selected") Boolean selected) {
        UserDTO user = UserHolder.getUser();
        return shoppingCartService.selectAll(user.getId(), selected);
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
     * 批量删除购物车项
     */
    @DeleteMapping("/items")
    public Result batchRemoveItems(@RequestBody List<Long> itemIds) {
        UserDTO user = UserHolder.getUser();
        return shoppingCartService.removeItems(user.getId(), itemIds);
    }

    /**
     * 清空购物车
     */
    @DeleteMapping("/clear")
    public Result clearCart() {
        UserDTO user = UserHolder.getUser();
        return shoppingCartService.clearCart(user.getId());
    }

    /**
     * 按店铺分组获取购物车
     */
    @GetMapping("/shop-group")
    public Result getCartByShop() {
        UserDTO user = UserHolder.getUser();
        return shoppingCartService.getCartByShop(user.getId());
    }

    /**
     * 合并临时购物车
     */
    @PostMapping("/merge")
    public Result mergeCart(@RequestParam("sessionId") String sessionId) {
        UserDTO user = UserHolder.getUser();
        return shoppingCartService.mergeCart(user.getId(), sessionId);
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
     * 未登录用户添加商品到临时购物车
     */
    @PostMapping("/temp/add")
    public Result addToTempCart(
            @RequestParam("sessionId") String sessionId,
            @RequestParam("productId") Long productId,
            @RequestParam("productType") Integer productType,
            @RequestParam("quantity") Integer quantity,
            @RequestParam(value = "skuId", required = false) Long skuId) {
        return tempCartService.addItem(sessionId, productId, productType, quantity, skuId);
    }

    /**
     * 获取临时购物车
     */
    @GetMapping("/temp")
    public Result getTempCart(@RequestParam("sessionId") String sessionId) {
        return Result.ok(tempCartService.getBySessionId(sessionId));
    }

    /**
     * 从临时购物车移除商品
     */
    @DeleteMapping("/temp/items/{itemId}")
    public Result removeTempCartItem(
            @RequestParam("sessionId") String sessionId,
            @PathVariable("itemId") Long itemId) {
        return tempCartService.removeItem(sessionId, itemId);
    }

    /**
     * 清空临时购物车
     */
    @DeleteMapping("/temp/clear")
    public Result clearTempCart(@RequestParam("sessionId") String sessionId) {
        return tempCartService.clearCart(sessionId);
    }
}