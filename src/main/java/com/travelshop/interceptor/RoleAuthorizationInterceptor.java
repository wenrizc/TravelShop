package com.travelshop.interceptor;

import com.travelshop.dto.UserDTO;
import com.travelshop.service.IShopOwnerService;
import com.travelshop.utils.UserHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@RequiredArgsConstructor
public class RoleAuthorizationInterceptor implements HandlerInterceptor {

    private final IShopOwnerService shopOwnerService;

    // 提取路径中的shopId参数的正则表达式
    private static final Pattern SHOP_ID_PATTERN = Pattern.compile("/shopManage/(\\d+)");

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        UserDTO user = UserHolder.getUser();
        if (user == null) {
            response.setStatus(401);
            return false;
        }

        // 获取当前请求路径
        String uri = request.getRequestURI();
        Integer role = user.getRole();

        // 默认普通用户
        if (role == null) {
            role = 0;
        }

        // 检查路径权限
        // 商家接口：需要商家或管理员权限(>=1)
        if (uri.startsWith("/merchant/") || uri.startsWith("/shopManage/")) {
            if (role < 1) {
                log.warn("用户{}权限不足，无法访问商家接口: {}", user.getId(), uri);
                response.setStatus(403);
                return false;
            }

            // 特殊处理：如果是针对特定商铺的操作，需要校验是否为该商铺的所有者
            if (uri.startsWith("/shopManage/")) {
                Matcher matcher = SHOP_ID_PATTERN.matcher(uri);
                if (matcher.find()) {
                    String shopIdStr = matcher.group(1);
                    Long shopId = Long.parseLong(shopIdStr);

                    // 管理员可操作任意商铺，商家只能操作自己的商铺
                    if (role == 1 && !shopOwnerService.checkOwnership(user.getId(), shopId)) {
                        log.warn("商家用户{}尝试访问非自有商铺{}", user.getId(), shopId);
                        response.setStatus(403);
                        return false;
                    }
                }
            }
        }

        // 管理员接口：仅限管理员访问(>=2)
        if (uri.startsWith("/admin/") || uri.startsWith("/system/")) {
            if (role < 2) {
                log.warn("用户{}权限不足，无法访问管理员接口: {}", user.getId(), uri);
                response.setStatus(403);
                return false;
            }
        }

        return true;
    }
}