package com.travelshop.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.travelshop.dto.OrderCreateDTO;
import com.travelshop.dto.OrderQueryDTO;
import com.travelshop.dto.Result;
import com.travelshop.entity.Order;
import com.travelshop.entity.OrderItem;

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
     * 删除订单
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

    /**
     * 查询用户订单
     * @param id 用户ID
     * @param queryDTO 查询条件
     * @return 订单列表
     */
    Object queryUserOrders(Long id, OrderQueryDTO queryDTO);

    /**
     * 支付订单
     * @param orderId 订单ID
     * @param payType 支付类型
     * @return 支付结果
     */
    Object payOrder(Long orderId, Integer payType);

    /**
     * 秒杀代金券
     * @param voucherId 代金券ID
     * @return 秒杀结果
     */
    public Result seckillVoucher(long voucherId);


}