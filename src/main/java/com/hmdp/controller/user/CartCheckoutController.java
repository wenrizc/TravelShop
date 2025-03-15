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
}