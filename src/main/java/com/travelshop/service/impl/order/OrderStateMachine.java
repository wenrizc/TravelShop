package com.travelshop.service.impl.order;

import com.travelshop.dto.OrderStateChangeDTO;
import com.travelshop.entity.Order;
import com.travelshop.enums.OrderStatus;
import com.travelshop.exception.OrderStateException;
import com.travelshop.mapper.OrderMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderStateMachine {


    private final OrderNotifier orderNotifier;
    private final OrderMapper orderMapper;

    /**
     * 执行状态转换
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean changeState(OrderStateChangeDTO changeDTO) {
        // 1. 获取订单
        Order order = orderMapper.selectById(changeDTO.getOrderId());
        if (order == null) {
            throw new OrderStateException("订单不存在");
        }

        // 2. 获取当前状态和目标状态
        OrderStatus currentStatus = OrderStatus.getByCode(order.getStatus());
        OrderStatus targetStatus = OrderStatus.getByCode(changeDTO.getTargetStatus());

        // 3. 验证状态转换是否合法
        if (!OrderStateTransition.canTransfer(currentStatus, targetStatus)) {
            log.error("状态转换不合法：从 {} 到 {}", currentStatus, targetStatus);
            throw new OrderStateException("订单状态无法从 " + currentStatus.getDesc() + " 变更为 " + targetStatus.getDesc());
        }

        // 4. 执行额外的业务规则验证
        validateBusinessRules(currentStatus, targetStatus, order, changeDTO);

        orderNotifier.notifyStatusChange(order, currentStatus, targetStatus, changeDTO);

        return true;
    }

    /**
     * 根据状态更新订单相关时间字段
     */
    private void updateOrderTimesByStatus(Order order, OrderStatus targetStatus) {
        LocalDateTime now = LocalDateTime.now();
        switch (targetStatus) {
            case PAID:
                order.setPayTime(now);
                break;
            case DELIVERED:
                order.setDeliveryTime(now);
                break;
            case RECEIVED:
                order.setReceiveTime(now);
                break;
            case COMPLETED:
                order.setFinishTime(now);
                break;
            case CANCELLED:
            case CLOSED:
                order.setCloseTime(now);
                break;
            default:
                // 其他状态不更新特定时间字段
                break;
        }
    }

    /**
     * 验证业务规则
     */
    private void validateBusinessRules(OrderStatus currentStatus, OrderStatus targetStatus,
                                       Order order, OrderStateChangeDTO changeDTO) {
        // 实现各种业务规则验证逻辑
        // 例如：检查是否超出退款时限、特殊商品类型的状态转换规则等

        // 这里仅做示例，实际应结合具体业务需求实现
        if (currentStatus == OrderStatus.RECEIVED &&
                targetStatus == OrderStatus.REFUND_APPLY &&
                order.getReceiveTime() != null) {

            // 检查收货后申请退款是否超时（例如7天内可退）
            LocalDateTime deadline = order.getReceiveTime().plusDays(7);
            if (LocalDateTime.now().isAfter(deadline)) {
                throw new OrderStateException("已超过退款时限（收货后7天），无法申请退款");
            }
        }
    }
}


