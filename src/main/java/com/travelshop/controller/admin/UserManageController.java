package com.travelshop.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.travelshop.dto.Result;
import com.travelshop.dto.UserDTO;
import com.travelshop.entity.User;
import com.travelshop.service.IShopOwnerService;
import com.travelshop.service.IUserService;
import com.travelshop.utils.UserHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/admin/users")
@RequiredArgsConstructor
public class UserManageController {

    private final IUserService userService;
    private final IShopOwnerService shopOwnerService;

    /**
     * 分页查询用户列表
     */
    @GetMapping
    public Result listUsers(
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "size", defaultValue = "10") Integer size,
            @RequestParam(value = "role", required = false) Integer role,
            @RequestParam(value = "keyword", required = false) String keyword) {

        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();

        // 按角色筛选
        if (role != null) {
            queryWrapper.eq(User::getRole, role);
        }

        // 按关键词搜索
        if (keyword != null && !keyword.isEmpty()) {
            queryWrapper.and(w -> w
                    .like(User::getNickName, keyword)
                    .or()
                    .like(User::getPhone, keyword)
            );
        }

        // 按创建时间倒序
        queryWrapper.orderByDesc(User::getCreateTime);

        // 分页查询
        Page<User> userPage = userService.page(new Page<>(page, size), queryWrapper);

        return Result.ok(userPage);
    }

    /**
     * 查询用户详情
     */
    @GetMapping("/{id}")
    public Result getUserDetail(@PathVariable("id") Long userId) {
        User user = userService.getById(userId);
        if (user == null) {
            return Result.fail("用户不存在");
        }

        Map<String, Object> result = new HashMap<>();
        result.put("user", user);

        // 如果是商家，查询关联的店铺
        if (user.getRole() != null && user.getRole() >= 1) {
            List<Long> shopIds = shopOwnerService.getShopIdsByUserId(userId);
            result.put("shopIds", shopIds);
        }

        return Result.ok(result);
    }

    /**
     * 修改用户角色
     */
    @PutMapping("/{id}/role")
    public Result updateUserRole(
            @PathVariable("id") Long userId,
            @RequestParam("role") Integer role) {

        // 检查当前操作用户的权限
        UserDTO currentUser = UserHolder.getUser();
        if (currentUser == null || currentUser.getRole() == null || currentUser.getRole() < 2) {
            return Result.fail("无权操作");
        }

        // 不能修改比自己权限高的用户
        User targetUser = userService.getById(userId);
        if (targetUser == null) {
            return Result.fail("目标用户不存在");
        }

        // 检查权限：只有管理员能提升到管理员
        if (role >= 2 && (currentUser.getRole() < 2)) {
            return Result.fail("无权提升用户为管理员");
        }

        // 更新角色
        targetUser.setRole(role);
        targetUser.setUpdateTime(LocalDateTime.now());

        boolean success = userService.updateById(targetUser);
        if (!success) {
            return Result.fail("更新失败");
        }

        // 记录操作日志
        log.info("管理员 {} 将用户 {} 角色修改为 {}", currentUser.getId(), userId, role);

        return Result.ok();
    }

    /**
     * 为用户分配商铺
     */
    @PostMapping("/{userId}/shops/{shopId}")
    public Result assignShopToUser(
            @PathVariable("userId") Long userId,
            @PathVariable("shopId") Long shopId) {

        // 检查当前操作用户的权限
        UserDTO currentUser = UserHolder.getUser();
        if (currentUser == null || currentUser.getRole() == null || currentUser.getRole() < 2) {
            return Result.fail("无权操作");
        }

        boolean success = shopOwnerService.assignShopToUser(userId, shopId);
        if (!success) {
            return Result.fail("分配商铺失败");
        }

        log.info("管理员 {} 将商铺 {} 分配给用户 {}", currentUser.getId(), shopId, userId);
        return Result.ok();
    }

    /**
     * 移除用户的商铺
     */
    @DeleteMapping("/{userId}/shops/{shopId}")
    public Result removeShopFromUser(
            @PathVariable("userId") Long userId,
            @PathVariable("shopId") Long shopId) {

        // 检查当前操作用户的权限
        UserDTO currentUser = UserHolder.getUser();
        if (currentUser == null || currentUser.getRole() == null || currentUser.getRole() < 2) {
            return Result.fail("无权操作");
        }

        boolean success = shopOwnerService.removeShopFromUser(userId, shopId);
        if (!success) {
            return Result.fail("移除商铺失败");
        }

        log.info("管理员 {} 将商铺 {} 从用户 {} 移除", currentUser.getId(), shopId, userId);
        return Result.ok();
    }
}