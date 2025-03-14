package com.hmdp.service.impl.order;

import com.hmdp.entity.*;
import com.hmdp.enums.OrderStatus;
import com.hmdp.enums.ProductType;
import com.hmdp.order;
import com.hmdp.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

/**
 * 商品类型特殊规则处理器
 */
@Component
@RequiredArgsConstructor
public class ProductTypeRuleHandler {

    private final IOrderService orderService;
    private final ITicketService ticketService;
    private final ITicketUsageService ticketUsageService;
    private final IVoucherService voucherService;
    private final IVoucherOrderService voucherOrderService;


    /**
     * 各商品类型的退款时限规则（返回允许退款的最长时间，单位天）
     */
    private static final Map<Integer, BiFunction<Order, OrderItem, Integer>> REFUND_TIME_RULES = new HashMap<>();


    @PostConstruct
    public void init() {
        // 普通商品，收货后7天内可退
        REFUND_TIME_RULES.put(1, (order, item) -> 7);

        // 生鲜商品，收货后1天内可退
        REFUND_TIME_RULES.put(2, (order, item) -> 1);

        // 数码商品，收货后15天内可退
        REFUND_TIME_RULES.put(3, (order, item) -> 15);

        // 服装商品，收货后7天内可退（未穿着/吊牌未剪）
        REFUND_TIME_RULES.put(4, (order, item) -> 7);

        // 虚拟商品，未使用可退，已使用不可退
        REFUND_TIME_RULES.put(5, (order, item) -> {
            // 虚拟商品特殊处理，需要查询是否已使用
            // 这里简化处理，统一返回1天
            return 1;
        });

        // 更新门票退款规则
        REFUND_TIME_RULES.put(ProductType.TICKET.getCode(), (order, item) -> {
            // 检查门票是否已使用
            TicketUsage usage = ticketUsageService.getByOrderItemId(item.getId());
            if (usage != null) {
                if (usage.getStatus() == 2) { // 已使用
                    return 0; // 不可退款
                } else if (usage.getStatus() == 3) { // 已过期
                    return 0; // 不可退款
                }
            }

            // 获取门票
            Ticket ticket = ticketService.getById(item.getProductId());
            if (ticket != null) {
                // 如果是时间限制门票且有效期不足3天，则退款期限缩短
                if (ticket.getIsTimeLimited() && ticket.getEffectiveDays() != null) {
                    if (ticket.getEffectiveDays() <= 3) {
                        return 1; // 只有1天可退
                    }
                }
            }

            return 7; // 默认7天内可退
        });

        // 更新优惠券退款规则
        REFUND_TIME_RULES.put(ProductType.VOUCHER.getCode(), (order, item) -> {
            // 检查优惠券是否已使用
            VoucherOrder voucherOrder = voucherOrderService.getByOrderItemId(item.getId());
            if (voucherOrder != null && voucherOrder.getStatus() == 3) { // 已使用
                return 0; // 不可退款
            }

            // 检查是否是秒杀券
            Voucher voucher = voucherService.getById(item.getProductId());
            if (voucher != null && voucher.getType() == 2) { // 秒杀券
                return 3; // 秒杀券3天内可退
            }

            return 30; // 普通优惠券30天内可退
        });
    }

    /**
     * 检查订单是否符合退款时限
     * @param order 订单对象
     * @return 是否可以申请退款
     */
    public boolean checkRefundTimeLimit(Order order) {
        // 只有已付款、已发货、已签收状态的订单才需要检查退款时限
        OrderStatus status = OrderStatus.getByCode(order.getStatus());
        if (status != OrderStatus.PAID && status != OrderStatus.DELIVERED && status != OrderStatus.RECEIVED) {
            return false;
        }

        // 未发货商品可直接退款
        if (status == OrderStatus.PAID) {
            return true;
        }

        // 已收货订单，需要检查时限
        if (status == OrderStatus.RECEIVED && order.getReceiveTime() != null) {
            // 获取订单中的商品
            List<OrderItem> items = orderService.getByOrderId(order.getId());
            if (items.isEmpty()) {
                return false;
            }

            // 获取所有商品中限制最严格的退款时限
            int minRefundDays = Integer.MAX_VALUE;

            for (OrderItem item : items) {
                // 获取商品类型（这里假设ProductType字段存在，实际可能需要关联查询）
                Integer productType = item.getProductType();
                if (productType == null) {
                    productType = 1; // 默认按普通商品处理
                }

                // 获取该类型商品的退款时限
                BiFunction<Order, OrderItem, Integer> rule = REFUND_TIME_RULES.getOrDefault(productType,
                        (o, i) -> 7); // 默认7天

                int refundDays = rule.apply(order, item);
                if (refundDays < minRefundDays) {
                    minRefundDays = refundDays;
                }
            }

            // 计算退款截止时间
            LocalDateTime deadline = order.getReceiveTime().plusDays(minRefundDays);

            // 判断是否在可退款时间范围内
            return LocalDateTime.now().isBefore(deadline);
        }

        return false;
    }
}