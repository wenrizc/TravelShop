package com.travelshop.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.travelshop.entity.TicketUsage;

import java.util.List;

public interface ITicketUsageService extends IService<TicketUsage> {
    /**
     * 根据核销码查询门票使用记录
     * @param code 核销码
     * @return 门票使用记录
     */
    TicketUsage getByCode(String code);

    /**
     * 使用门票
     * @param code 核销码
     * @return 是否成功
     */
    boolean useTicket(String code);

    /**
     * 根据订单项ID查询门票使用记录
     * @param orderItemId 订单项ID
     * @return 门票使用记录
     */
    TicketUsage getByOrderItemId(Long orderItemId);

    /**
     * 标记为已过期
     * @param id 使用记录ID
     * @return 是否成功
     */
    boolean markAsExpired(Long id);

    /**
     * 标记为已退款
     * @param orderId 订单ID
     * @return 是否成功
     */
    boolean markAsRefunded(Long orderId);

    /**
     * 获取用户的门票列表
     * @param userId 用户ID
     * @return 门票使用记录列表
     */
    List<TicketUsage> getUserTickets(Long userId);

    /**
     * 检查订单项下的所有门票是否都已核销
     * @param orderItemId 订单项ID
     * @return 如果都已核销返回true，否则返回false
     */
    boolean checkAllTicketsUsed(Long orderItemId);
}