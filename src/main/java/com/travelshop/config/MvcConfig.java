package com.travelshop.config;

import com.travelshop.interceptor.LoginInterceptor;
import com.travelshop.interceptor.RefreshTokenInterceptor;
import com.travelshop.interceptor.RoleAuthorizationInterceptor;
import com.travelshop.service.IShopOwnerService;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.annotation.Resource;

/**
 * MVC配置
 */
@Configuration
public class MvcConfig implements WebMvcConfigurer {
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private IShopOwnerService shopOwnerService;


    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // Token解析拦截器 - 优先级最高
        registry
                .addInterceptor(new RefreshTokenInterceptor(stringRedisTemplate))
                .addPathPatterns("/**")
                .order(0);

        // 登录拦截器 - 中等优先级
        registry
                .addInterceptor(new LoginInterceptor())
                .excludePathPatterns(
                        "/user/code",
                        "/user/login",
                        "/user/refresh-token",
                        "/blog/hot",
                        "/shop/**",
                        "/shop-type/**",
                        "/upload/**",
                        "/voucher/**"
                )
                .order(1);

        // 角色权限拦截器 - 最低优先级
        registry
                .addInterceptor(new RoleAuthorizationInterceptor(shopOwnerService))
                .addPathPatterns("/merchant/**", "/shopManage/**", "/admin/**", "/system/**")
                .order(2);
    }
}