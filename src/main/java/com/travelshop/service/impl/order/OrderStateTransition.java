package com.travelshop.service.impl.order;

import com.travelshop.enums.OrderStatus;

import java.util.*;

/**
 * 订单状态转换规则
 */
public class OrderStateTransition {

    /**
     * 存储每个状态允许的后续状态
     */
    private static final Map<OrderStatus, Set<OrderStatus>> ALLOWED_TRANSITIONS = new HashMap<>();

    static {
        // 待付款状态允许转换为：已付款、已取消
        ALLOWED_TRANSITIONS.put(OrderStatus.WAIT_PAY, new HashSet<>(Arrays.asList(
                OrderStatus.PAID,
                OrderStatus.CANCELLED
        )));

        // 已付款状态允许转换为：已发货、申请退款
        ALLOWED_TRANSITIONS.put(OrderStatus.PAID, new HashSet<>(Arrays.asList(
                OrderStatus.DELIVERED,
                OrderStatus.REFUND_APPLY
        )));

        // 已发货状态允许转换为：已签收、申请退款
        ALLOWED_TRANSITIONS.put(OrderStatus.DELIVERED, new HashSet<>(Arrays.asList(
                OrderStatus.RECEIVED,
                OrderStatus.REFUND_APPLY
        )));

        // 已签收状态允许转换为：已完成、申请退款
        ALLOWED_TRANSITIONS.put(OrderStatus.RECEIVED, new HashSet<>(Arrays.asList(
                OrderStatus.COMPLETED,
                OrderStatus.REFUND_APPLY
        )));

        // 已完成状态为最终状态，不允许转换
        ALLOWED_TRANSITIONS.put(OrderStatus.COMPLETED, new HashSet<>());

        // 已取消状态为最终状态，不允许转换
        ALLOWED_TRANSITIONS.put(OrderStatus.CANCELLED, new HashSet<>());

        // 申请退款状态允许转换为：已付款(拒绝退款)、退款成功
        ALLOWED_TRANSITIONS.put(OrderStatus.REFUND_APPLY, new HashSet<>(Arrays.asList(
                OrderStatus.PAID, // 拒绝退款则回到已付款状态
                OrderStatus.REFUNDED
        )));

        // 退款成功状态为最终状态，不允许转换
        ALLOWED_TRANSITIONS.put(OrderStatus.REFUNDED, new HashSet<>());

        // 交易关闭状态为最终状态，不允许转换
        ALLOWED_TRANSITIONS.put(OrderStatus.CLOSED, new HashSet<>());
    }

    /**
     * 检查状态转换是否有效
     * @param current 当前状态
     * @param target 目标状态
     * @return 是否允许转换
     */
    public static boolean canTransfer(OrderStatus current, OrderStatus target) {
        if (current == null || target == null) {
            return false;
        }

        Set<OrderStatus> allowedStatus = ALLOWED_TRANSITIONS.get(current);
        return allowedStatus != null && allowedStatus.contains(target);
    }
}