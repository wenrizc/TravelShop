package com.travelshop.controller.merchant;

import com.travelshop.dto.Result;
import com.travelshop.dto.UserDTO;
import com.travelshop.entity.Shop;
import com.travelshop.service.IShopOwnerService;
import com.travelshop.service.IShopService;
import com.travelshop.utils.UserHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/shopManage")
@RequiredArgsConstructor
public class ShopManageController {

    private final IShopService shopService;
    private final IShopOwnerService shopOwnerService;

    /**
     * 根据id查询商铺信息
     * @param id 商铺id
     * @return 商铺详情数据
     */
    @GetMapping("/{id}")
    public Result queryShopById(@PathVariable("id") Long id) {
        UserDTO user = UserHolder.getUser();

        // 管理员可以查看任意商铺，商家只能查看自己的商铺
        if (user.getRole() != null && user.getRole() == 1) {
            if (!shopOwnerService.checkOwnership(user.getId(), id)) {
                return Result.fail("无权访问该商铺");
            }
        }

        return shopService.queryById(id);
    }

    /**
     * 新增商铺信息
     * @param shop 商铺数据
     * @return 商铺id
     */
    @PostMapping
    public Result saveShop(@RequestBody Shop shop) {
        UserDTO user = UserHolder.getUser();

        // 写入数据库
        boolean success = shopService.save(shop);
        if (success) {
            // 创建商铺所有者关联
            shopOwnerService.assignShopToUser(user.getId(), shop.getId());
        }
        return Result.ok(shop.getId());
    }

    /**
     * 更新商铺信息
     * @param shop 商铺数据
     * @return 无
     */
    @PutMapping
    public Result updateShop(@RequestBody Shop shop) {
        UserDTO user = UserHolder.getUser();

        // 管理员可以更新任意商铺，商家只能更新自己的商铺
        if (user.getRole() != null && user.getRole() == 1) {
            if (!shopOwnerService.checkOwnership(user.getId(), shop.getId())) {
                return Result.fail("无权更新该商铺");
            }
        }

        return shopService.update(shop);
    }

    /**
     * 删除商铺
     * @param id 商铺id
     * @return 操作结果
     */
    @DeleteMapping("/{id}")
    public Result deleteShop(@PathVariable("id") Long id) {
        UserDTO user = UserHolder.getUser();

        // 只有管理员可以删除商铺
        if (user.getRole() == null || user.getRole() < 2) {
            return Result.fail("无权删除商铺");
        }

        boolean success = shopService.removeById(id);
        return success ? Result.ok() : Result.fail("删除失败");
    }
}