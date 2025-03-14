package com.hmdp.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.OrderCreateDTO;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.SeckillVoucher;
import com.hmdp.entity.Voucher;
import com.hmdp.enums.ProductType;
import com.hmdp.mapper.SeckillVoucherMapper;
import com.hmdp.mapper.VoucherMapper;
import com.hmdp.service.IOrderService;
import com.hmdp.service.ISeckillVoucherService;
import com.hmdp.service.IVoucherService;
import com.hmdp.utils.UserHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.Collections;

/**
 * <p>
 * 秒杀优惠券表，与优惠券是一对一关系 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2022-01-04
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class SeckillVoucherServiceImpl extends ServiceImpl<SeckillVoucherMapper, SeckillVoucher> implements ISeckillVoucherService {

    private final StringRedisTemplate stringRedisTemplate;

    @Lazy
    private final IOrderService orderService;

    private final VoucherMapper voucherMapper;

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

    // 将秒杀功能改造成先生成订单，再支付的模式
    @Override
    public Result seckillVoucher(Long voucherId) {
        // 获取用户
        UserDTO user = UserHolder.getUser();

        // 执行Lua脚本，验证是否有购买资格
        // 注意：这里没有传入id参数，因为当前实现已改为创建订单而非直接创建优惠券订单
        Long res = stringRedisTemplate.execute(
                SECKILL_SCRIPT,
                Collections.emptyList(),
                voucherId.toString(),
                user.getId().toString());

        // 判断结果
        int r = res.intValue();
        if (r != 0) {
            // 不为0 没有购买资格
            return Result.fail(r == 1 ? "库存不足" : "禁止重复下单");
        }


        // 有购买资格，创建普通订单（不再直接创建优惠券订单）
        Voucher voucher = voucherMapper.selectById(voucherId);
        if (voucher == null) {
            return Result.fail("优惠券不存在");
        }

        // 创建订单
        OrderCreateDTO createDTO = new OrderCreateDTO();
        createDTO.setUserId(user.getId());

        // 创建订单项
        OrderCreateDTO.OrderItemDTO itemDTO = new OrderCreateDTO.OrderItemDTO();
        itemDTO.setProductId(voucherId);
        itemDTO.setProductType(ProductType.VOUCHER.getCode()); // 标记为优惠券类型
        itemDTO.setCount(1);
        itemDTO.setPrice(BigDecimal.valueOf(voucher.getPayValue()));

        createDTO.setOrderItems(Collections.singletonList(itemDTO));
        createDTO.setPayType(1); // 默认支付方式
        createDTO.setSource(3);  // 来源（如小程序）

        // 创建订单
        Long orderId = orderService.createOrder(createDTO);

        return Result.ok(orderId);
    }
}
