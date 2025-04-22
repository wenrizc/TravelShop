package com.travelshop.config;

import com.alibaba.csp.sentinel.adapter.spring.webmvc.callback.BlockExceptionHandler;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.travelshop.dto.Result;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Configuration
public class SentinelDashboardConfig {

    /**
     * 自定义限流异常处理
     */
    @Bean
    public BlockExceptionHandler blockExceptionHandler() {
        return new BlockExceptionHandler() {
            @Override
            public void handle(HttpServletRequest request, HttpServletResponse response, BlockException e) throws Exception {
                // 设置响应类型
                response.setStatus(429);
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.setCharacterEncoding("UTF-8");

                // 构建响应内容
                String requestURI = request.getRequestURI();
                Result result = Result.fail("系统繁忙，请稍后重试");

                // 写入响应
                new ObjectMapper().writeValue(response.getWriter(), result);
            }
        };
    }
}