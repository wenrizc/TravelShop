package com.hmdp.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.OrderStateChangeDTO;
import com.hmdp.entity.Order;
import com.hmdp.entity.OrderStatusHistory;
import com.hmdp.enums.OperatorType;
import com.hmdp.enums.OrderStatus;
import com.hmdp.mapper.OrderStatusHistoryMapper;
import com.hmdp.service.IOrderStateService;
import com.hmdp.service.impl.order.OrderStateMachine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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
    @Transactional(rollbackFor = Exception.class)
    public boolean deliverOrder(Long orderId, String operatorId, String logisticsCode) {
        log.info("订单发货: orderId={}, operatorId={}, logisticsCode={}", orderId, operatorId, logisticsCode);

        // 先更新物流信息
        Order updateOrder = new Order();
        updateOrder.setId(orderId);
        updateOrder.setStatus(OrderStatus.DELIVERED.getCode());
        // 然后变更状态
        OrderStateChangeDTO changeDTO = new OrderStateChangeDTO();
        changeDTO.setOrderId(orderId);
        changeDTO.setTargetStatus(OrderStatus.DELIVERED.getCode());
        changeDTO.setOperator(operatorId);
        changeDTO.setOperatorType(OperatorType.ADMIN);
        changeDTO.setRemark("物流单号: " + logisticsCode);

        return changeOrderState(changeDTO);
    }

    @Override
    public boolean confirmReceive(Long orderId, Long userId) {
        log.info("确认收货: orderId={}, userId={}", orderId, userId);

        OrderStateChangeDTO changeDTO = new OrderStateChangeDTO();
        changeDTO.setOrderId(orderId);
        changeDTO.setTargetStatus(OrderStatus.RECEIVED.getCode());
        changeDTO.setOperator(userId.toString());
        changeDTO.setOperatorType(OperatorType.USER);
        changeDTO.setReason("用户确认收货");

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

    /**
     * 获取订单状态历史
     * @param orderId 订单ID
     * @return 状态历史记录列表
     */
    public List<OrderStatusHistory> getHistoryByOrderId(Long orderId) {
        // 直接使用Mapper查询历史记录
        return baseMapper.findByOrderId(orderId);
    }
}