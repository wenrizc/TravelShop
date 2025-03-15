package com.hmdp.controller;


import com.hmdp.dto.Result;
import com.hmdp.service.IOrderService;
import com.hmdp.service.IVoucherOrderService;
import com.hmdp.service.impl.OrderServiceImpl;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * 优惠券订单控制器
 */
@RestController
@RequestMapping("/voucher-order")
public class VoucherOrderController {
    @Resource
    private IOrderService orderService;
    @PostMapping("seckill/{id}")
    public Result seckillVoucher(@PathVariable("id") Long voucherId) {
        return orderService.seckillVoucher(voucherId);
    }
}
