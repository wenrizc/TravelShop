package com.hmdp.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hmdp.dto.OrderStateChangeDTO;
import com.hmdp.entity.OrderStatusHistory;
import com.hmdp.enums.OrderStatus;

public interface IOrderStateService extends IService<OrderStatusHistory> {

    /**
     * 修改订单状态
     * @param changeDTO 状态变更参数
     * @return 是否成功
     */
    boolean changeOrderState(OrderStateChangeDTO changeDTO);

    /**
     * 取消订单
     * @param orderId 订单ID
     * @param reason 取消原因
     * @param operatorId 操作人ID
     * @return 是否成功
     */
    boolean cancelOrder(Long orderId, String reason, String operatorId);

    /**
     * 申请退款
     * @param orderId 订单ID
     * @param id 退款ID
     * @param reason 退款原因
     * @return 是否成功
     */
    boolean applyRefund(Long orderId, Long id, String reason);
}