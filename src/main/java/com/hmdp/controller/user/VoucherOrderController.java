package com.hmdp.controller.user;


import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.service.IOrderService;
import com.hmdp.service.IVoucherOrderService;
import com.hmdp.service.IVoucherService;
import com.hmdp.utils.UserHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 优惠券订单控制器
 */
@RestController
@RequestMapping("/voucher-order")
@RequiredArgsConstructor
public class VoucherOrderController {
    private final IOrderService orderService;
    private final IVoucherOrderService voucherOrderService;
    private final IVoucherService voucherService;

    @PostMapping("seckill/{id}")
    public Result seckillVoucher(@PathVariable("id") Long voucherId) {
        return orderService.seckillVoucher(voucherId);
    }

    /**
     * 优惠券核销
     */
    @PostMapping("/voucher/use/{id}")
    public Result useVoucher(@PathVariable("id") Long id) {
        UserDTO user = UserHolder.getUser();

        // 验证优惠券所有权
        VoucherOrder voucherOrder = voucherOrderService.getById(id);
        if (voucherOrder == null) {
            return Result.fail("优惠券不存在");
        }

        if (!voucherOrder.getUserId().equals(user.getId())) {
            return Result.fail("无权使用此优惠券");
        }

        // 检查优惠券状态
        if (voucherOrder.getStatus() != 2) { // 假设2是已支付待使用状态
            String statusMsg = "";
            switch (voucherOrder.getStatus()) {
                case 1: statusMsg = "优惠券未支付"; break;
                case 3: statusMsg = "优惠券已使用"; break;
                case 4: statusMsg = "优惠券已过期"; break;
                case 5: statusMsg = "优惠券已退款"; break;
                default: statusMsg = "优惠券状态异常";
            }
            return Result.fail(statusMsg);
        }

        // 核销优惠券
        boolean success = voucherOrderService.useVoucher(id);
        return success ? Result.ok("核销成功") : Result.fail("核销失败");
    }

    /**
     * 查询我的优惠券列表
     */
    @GetMapping("/vouchers")
    public Result myVouchers() {
        UserDTO user = UserHolder.getUser();
        List<VoucherOrder> vouchers = voucherOrderService.getUserVouchers(user.getId());
        return Result.ok(vouchers);
    }

    /**
     * 查询店铺的优惠券列表
     * @param shopId 店铺id
     * @return 优惠券列表
     */
    @GetMapping("/list/{shopId}")
    public Result queryVoucherOfShop(@PathVariable("shopId") Long shopId) {
        return voucherService.queryVoucherOfShop(shopId);
    }
}
