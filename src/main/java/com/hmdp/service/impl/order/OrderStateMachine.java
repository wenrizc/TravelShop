package com.hmdp.service.impl.order;

import com.hmdp.dto.OrderStateChangeDTO;
import com.hmdp.entity.Order;
import com.hmdp.entity.OrderStatusHistory;
import com.hmdp.enums.OrderStatus;
import com.hmdp.exception.OrderStateException;
import com.hmdp.service.IOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderStateMachine {


    private final IOrderService orderService;
    private final OrderNotifier orderNotifier;

    /**
     * 执行状态转换
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean changeState(OrderStateChangeDTO changeDTO) {
        // 1. 获取订单
        Order order = orderService.getById(changeDTO.getOrderId());
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

        // 5. 更新订单状态
        order.setStatus(targetStatus.getCode());
        updateOrderTimesByStatus(order, targetStatus);
        order.setUpdateTime(LocalDateTime.now());
        boolean updated = orderService.updateById(order);

        if (!updated) {
            throw new OrderStateException("更新订单状态失败");
        }

        // 6. 记录状态变更历史
        OrderStatusHistory history = new OrderStatusHistory();
        history.setOrderId(order.getId());
        history.setPreviousStatus(currentStatus.getCode());
        history.setCurrentStatus(targetStatus.getCode());
        history.setOperator(changeDTO.getOperator());
        history.setOperatorType(changeDTO.getOperatorType().getCode());
        history.setReason(changeDTO.getReason());
        history.setRemark(changeDTO.getRemark());
        history.setCreateTime(LocalDateTime.now());
        orderService.save(history);

        // 7. 发送状态变更通知
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


