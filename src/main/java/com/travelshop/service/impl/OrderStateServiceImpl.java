package com.travelshop.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.travelshop.dto.OrderStateChangeDTO;
import com.travelshop.entity.OrderStatusHistory;
import com.travelshop.enums.OperatorType;
import com.travelshop.enums.OrderStatus;
import com.travelshop.mapper.OrderStatusHistoryMapper;
import com.travelshop.service.IOrderStateService;
import com.travelshop.service.impl.order.OrderStateMachine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderStateServiceImpl extends ServiceImpl<OrderStatusHistoryMapper, OrderStatusHistory> implements IOrderStateService {
    private final OrderStateMachine orderStateMachine;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean changeOrderState(OrderStateChangeDTO changeDTO) {
        log.info("修改订单状态: {}", changeDTO);

        // 参数校验
        if (changeDTO == null || changeDTO.getOrderId() == null || changeDTO.getTargetStatus() == null) {
            log.error("参数不完整，无法修改订单状态");
            return false;
        }

        try {
            // 直接委托给订单状态机处理
            return orderStateMachine.changeState(changeDTO);
        } catch (Exception e) {
            log.error("订单状态修改失败: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean cancelOrder(Long orderId, String reason, String operatorId) {
        log.info("管理员取消订单: orderId={}, reason={}, operatorId={}", orderId, reason, operatorId);

        OrderStateChangeDTO changeDTO = new OrderStateChangeDTO();
        changeDTO.setOrderId(orderId);
        changeDTO.setTargetStatus(OrderStatus.CANCELLED.getCode());
        changeDTO.setOperator(operatorId);
        changeDTO.setOperatorType(OperatorType.ADMIN);
        changeDTO.setReason(reason);

        return changeOrderState(changeDTO);
    }

    @Override
    public boolean applyRefund(Long orderId, Long userId, String reason) {
        log.info("申请退款: orderId={}, userId={}, reason={}", orderId, userId, reason);

        OrderStateChangeDTO changeDTO = new OrderStateChangeDTO();
        changeDTO.setOrderId(orderId);
        changeDTO.setTargetStatus(OrderStatus.REFUND_APPLY.getCode());
        changeDTO.setOperator(userId.toString());
        changeDTO.setOperatorType(OperatorType.USER);
        changeDTO.setReason(reason);

        return changeOrderState(changeDTO);
    }
}