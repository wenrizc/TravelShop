package com.travelshop.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.travelshop.entity.SeckillVoucher;
import com.travelshop.mapper.SeckillVoucherMapper;
import com.travelshop.service.ISeckillVoucherService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;


@Service
@Slf4j
@RequiredArgsConstructor
public class SeckillVoucherServiceImpl extends ServiceImpl<SeckillVoucherMapper, SeckillVoucher> implements ISeckillVoucherService {

    private final StringRedisTemplate stringRedisTemplate;

    // 定义脚本变量
    private static final DefaultRedisScript<Long> SECKILL_SCRIPT;

    // 静态代码块，在类加载时初始化脚本
    static {
        SECKILL_SCRIPT = new DefaultRedisScript<>();
        // 设置脚本路径
        SECKILL_SCRIPT.setLocation(new ClassPathResource("seckill.lua"));
        // 设置返回值类型
        SECKILL_SCRIPT.setResultType(Long.class);
    }


}
