package com.travelshop.interceptor;

import cn.hutool.core.util.StrUtil;
import com.travelshop.dto.UserDTO;
import com.travelshop.utils.JwtUtils;
import com.travelshop.utils.RedisConstants;
import com.travelshop.utils.UserHolder;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class RefreshTokenInterceptor implements HandlerInterceptor {
    private final StringRedisTemplate stringRedisTemplate;
    private final JwtUtils jwtUtils = new JwtUtils();
    public RefreshTokenInterceptor(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 从请求头获取token
        String token = request.getHeader("authorization");
        if (StrUtil.isBlank(token)) {
            return true;
        }

        // 检查令牌是否在黑名单中
        Boolean isInBlacklist = stringRedisTemplate.hasKey(RedisConstants.TOKEN_BLACKLIST_KEY + token);
        if (Boolean.TRUE.equals(isInBlacklist)) {
            return true; // 令牌已失效，交给LoginInterceptor处理
        }

        // 验证JWT令牌
        if (!jwtUtils.validateToken(token)) {
            return true; // 令牌无效，交给LoginInterceptor处理
        }

        // 从JWT中获取用户信息
        UserDTO userDTO = jwtUtils.getUserFromToken(token);
        if (userDTO == null) {
            return true;
        }

        // 保存用户信息到ThreadLocal
        UserHolder.saveUser(userDTO);

        return true;
    }


    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        UserHolder.removeUser();
    }
}
