package com.hmdp.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.hmdp.dto.OrderCreateDTO;
import com.hmdp.dto.OrderQueryDTO;
import com.hmdp.entity.Order;
import com.hmdp.entity.OrderItem;
import com.hmdp.entity.OrderStatusHistory;
import org.aspectj.weaver.ast.Or;

import java.util.List;

/**
 * 订单服务接口
 */
public interface IOrderService extends IService<Order> {

    /**
     * 创建订单
     * @param createDTO 订单创建参数
     * @return 订单ID
     */
    Long createOrder(OrderCreateDTO createDTO);

    /**
     * 取消订单
     * @param orderId 订单ID
     * @param userId 用户ID
     * @param reason 取消原因
     * @return 是否成功
     */
    boolean cancelOrder(Long orderId, Long userId, String reason);

    /**
     * 获取订单详情
     *
     * @param id 订单ID
     * @return 订单详情
     */
    Order getOrderDetail(Long id);

    /**
     * 分页查询订单列表
     * @param queryDTO 查询条件
     * @return 订单分页结果
     */
    Page<Order> queryOrders(OrderQueryDTO queryDTO);

    /**
     * 用户确认收货
     * @param orderId 订单ID
     * @param userId 用户ID
     * @return 是否成功
     */
    boolean confirmReceive(Long orderId, Long userId);

    /**
     * 删除订单（逻辑删除）
     * @param orderId 订单ID
     * @param userId 用户ID
     * @return 是否成功
     */
    boolean deleteOrder(Long orderId, Long userId);

    /**
     * 根据订单ID获取
     * @param orderId 订单ID
     * @return 订单信息
     */
    List<OrderItem>  getByOrderId(Long orderId);

    Object queryUserOrders(Long id, OrderQueryDTO queryDTO);

    Object payOrder(Long orderId, Integer payType);

    List<OrderStatusHistory> getHistoryByOrderId(Long orderId);

    void save(OrderStatusHistory history);

}