package com.hmdp.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.OrderCreateDTO;
import com.hmdp.dto.OrderQueryDTO;
import com.hmdp.entity.Order;
import com.hmdp.entity.OrderItem;
import com.hmdp.entity.OrderStatusHistory;
import com.hmdp.enums.OrderStatus;
import com.hmdp.mapper.OrderMapper;
import com.hmdp.service.IOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl extends ServiceImpl<OrderMapper, Order> implements IOrderService {

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createOrder(OrderCreateDTO createDTO) {
        log.info("开始创建订单: {}", createDTO);

        // 1. 参数校验
        if (createDTO == null || !createDTO.isValid()) {
            throw new IllegalArgumentException("订单参数无效");
        }

        // 2. 构建订单对象
        Order order = new Order();
        // 生成订单编号 (实际项目中可能使用更复杂的订单号生成策略)
        String orderNo = "ORDER" + System.currentTimeMillis() + createDTO.getUserId() % 1000;
        order.setOrderNo(orderNo);
        order.setUserId(createDTO.getUserId());
        order.setAddressId(createDTO.getAddressId());
        order.setStatus(OrderStatus.WAIT_PAY.getCode()); // 初始状态为待支付
        order.setPayType(createDTO.getPayType());
        order.setSource(createDTO.getSource());
        order.setRemark(createDTO.getRemark());

        // 3. 计算订单金额
        BigDecimal totalAmount = BigDecimal.ZERO;
        BigDecimal discountAmount = BigDecimal.ZERO;
        BigDecimal deliveryFee = new BigDecimal("8.00"); // 这里应该根据实际情况计算配送费

        // 4. 查询商品信息、计算金额
        List<OrderCreateDTO.OrderItemDTO> itemDTOs = createDTO.getOrderItems();
        List<OrderItem> orderItems = new ArrayList<>(itemDTOs.size());

        for (OrderCreateDTO.OrderItemDTO itemDTO : itemDTOs) {
            // 实际项目中应该从数据库查询商品信息，这里简化处理
            // ProductInfo product = productService.getById(itemDTO.getProductId());
            // if (product == null) throw new BusinessException("商品不存在");
            // SKU sku = skuService.getById(itemDTO.getSkuId());
            // if (sku == null) throw new BusinessException("商品规格不存在");

            // 这里应检查库存
            // if (sku.getStock() < itemDTO.getCount()) throw new BusinessException("商品库存不足");

            // 创建订单项
            OrderItem item = new OrderItem();
            item.setProductId(itemDTO.getProductId());
            item.setSkuId(itemDTO.getSkuId());
            item.setCount(itemDTO.getCount());
            // item.setProductName(product.getName());
            // item.setProductImg(product.getImage());
            // item.setSkuName(sku.getName());
            // item.setPrice(sku.getPrice());

            // 这里用前端传来的价格做示例，实际应该使用后端查询的价格
            item.setPrice(itemDTO.getPrice());
            item.setTotalAmount(itemDTO.getPrice().multiply(new BigDecimal(itemDTO.getCount())));

            orderItems.add(item);
            totalAmount = totalAmount.add(item.getTotalAmount());
        }

        // 5. 处理优惠券
        if (createDTO.getCouponId() != null) {
            // 实际项目中应该查询优惠券并计算折扣
            // Coupon coupon = couponService.getById(createDTO.getCouponId());
            // discountAmount = couponService.calculateDiscount(coupon, totalAmount);
            // couponService.useCoupon(createDTO.getUserId(), createDTO.getCouponId());
        }

        // 6. 设置订单金额
        order.setTotalAmount(totalAmount);
        order.setDiscountAmount(discountAmount);
        order.setDeliveryFee(deliveryFee);
        order.setPayAmount(totalAmount.subtract(discountAmount).add(deliveryFee));

        // 7. 设置订单时间
        LocalDateTime now = LocalDateTime.now();
        order.setCreateTime(now);
        order.setUpdateTime(now);
        order.setIsDeleted(0); // 未删除

        // 8. 保存订单和订单项
        boolean saved = this.save(order);
        if (!saved) {
            log.error("保存订单失败: {}", order);
            throw new RuntimeException("保存订单失败");
        }

        // 保存订单项
        for (OrderItem item : orderItems) {
            item.setOrderId(order.getId());
            item.setCreateTime(now);
            item.setUpdateTime(now);
        }

        // 批量保存订单项
        // orderItemService.saveBatch(orderItems);
        // 简化处理，实际项目中应该使用订单项服务保存
        log.info("保存订单项: {}", orderItems);

        // 9. 扣减库存
        // 实际项目中应该调用商品服务扣减库存
        // for (OrderItemDTO item : itemDTOs) {
        //    skuService.decreaseStock(item.getSkuId(), item.getCount());
        // }

        log.info("订单创建成功: {}, 订单ID: {}", orderNo, order.getId());
        return order.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean cancelOrder(Long orderId, Long userId, String reason) {
        log.info("开始取消订单: orderId={}, userId={}, reason={}", orderId, userId, reason);

        // 1. 查询订单
        Order order = this.getById(orderId);
        if (order == null) {
            log.error("取消订单失败: 订单不存在, orderId={}", orderId);
            throw new RuntimeException("订单不存在");
        }

        // 2. 验证订单所有权
        if (!order.getUserId().equals(userId)) {
            log.error("取消订单失败: 订单不属于当前用户, orderId={}, userId={}", orderId, userId);
            throw new RuntimeException("无权操作此订单");
        }

        // 3. 验证订单状态是否可取消（通常只有未支付或待发货状态可以取消）
        Integer status = order.getStatus();
        if (!OrderStatus.WAIT_PAY.getCode().equals(status) && !OrderStatus.WAIT_DELIVER.getCode().equals(status)) {
            log.error("取消订单失败: 订单状态不允许取消, orderId={}, status={}", orderId, status);
            throw new RuntimeException("当前订单状态不可取消");
        }

        // 4. 更新订单状态
        Order updateOrder = new Order();
        updateOrder.setId(orderId);
        updateOrder.setStatus(OrderStatus.CANCELLED.getCode());
        updateOrder.setUpdateTime(LocalDateTime.now());
        updateOrder.setCancelReason(reason);
        boolean updated = this.updateById(updateOrder);

        if (!updated) {
            log.error("取消订单失败: 更新订单状态失败, orderId={}", orderId);
            throw new RuntimeException("取消订单失败");
        }
        //TODO
        // 5. 恢复库存
        List<OrderItem> orderItems = this.getByOrderId(orderId);
        // 实际项目中应调用商品服务恢复库存
        // for (OrderItem item : orderItems) {
        //     productService.increaseStock(item.getSkuId(), item.getCount());
        // }

        // 6. 记录订单状态变更历史
        OrderStatusHistory history = new OrderStatusHistory();
        history.setOrderId(orderId);
        history.setOrderStatus(OrderStatus.CANCELLED.getCode());
        history.setRemark("用户取消订单: " + reason);
        history.setUpdateTime(LocalDateTime.now());
        this.save(history);

        log.info("订单取消成功: orderId={}", orderId);
        return true;
    }

    @Override
    public Order getOrderDetail(Long id) {
        log.info("查询订单详情: id={}", id);

        // 查询订单基本信息
        Order order = this.getById(id);
        if (order == null) {
            log.error("查询订单详情失败: 订单不存在, id={}", id);
            return null;
        }

        // 查询订单项
        List<OrderItem> orderItems = this.getByOrderId(id);
        order.setOrderItems(orderItems);

        log.info("查询订单详情成功: id={}", id);
        return order;
    }

    @Override
    public Page<Order> queryOrders(OrderQueryDTO queryDTO) {
        log.info("查询订单列表: queryDTO={}", queryDTO);

        // 创建分页对象
        Page<Order> page = new Page<>(queryDTO.getPage(), queryDTO.getSize());

        // 构建查询条件
        LambdaQueryWrapper<Order> queryWrapper = new LambdaQueryWrapper<>();

        // 按订单号查询
        if (StringUtils.isNotBlank(queryDTO.getOrderNo())) {
            queryWrapper.eq(Order::getOrderNo, queryDTO.getOrderNo());
        }

        // 按用户ID查询
        if (queryDTO.getUserId() != null) {
            queryWrapper.eq(Order::getUserId, queryDTO.getUserId());
        }

        // 按订单状态查询
        if (queryDTO.getStatus() != null) {
            queryWrapper.eq(Order::getStatus, queryDTO.getStatus());
        }

        // 按下单时间范围查询
        if (queryDTO.getStartTime() != null && queryDTO.getEndTime() != null) {
            queryWrapper.between(Order::getCreateTime, queryDTO.getStartTime(), queryDTO.getEndTime());
        }

        // 按未删除查询
        queryWrapper.eq(Order::getIsDeleted, 0);

        // 按创建时间倒序排序
        queryWrapper.orderByDesc(Order::getCreateTime);

        // 执行查询
        Page<Order> resultPage = this.page(page, queryWrapper);

        log.info("查询订单列表成功: total={}", resultPage.getTotal());
        return resultPage;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean confirmReceive(Long orderId, Long userId) {
        log.info("确认收货: orderId={}, userId={}", orderId, userId);

        // 1. 查询订单
        Order order = this.getById(orderId);
        if (order == null) {
            log.error("确认收货失败: 订单不存在, orderId={}", orderId);
            throw new RuntimeException("订单不存在");
        }

        // 2. 验证订单所有权
        if (!order.getUserId().equals(userId)) {
            log.error("确认收货失败: 订单不属于当前用户, orderId={}, userId={}", orderId, userId);
            throw new RuntimeException("无权操作此订单");
        }

        // 3. 验证订单状态是否为配送中
        if (!OrderStatus.DELIVERED.getCode().equals(order.getStatus())) {
            log.error("确认收货失败: 订单状态错误, orderId={}, status={}", orderId, order.getStatus());
            throw new RuntimeException("当前订单状态不可确认收货");
        }

        // 4. 更新订单状态
        Order updateOrder = new Order();
        updateOrder.setId(orderId);
        updateOrder.setStatus(OrderStatus.COMPLETED.getCode());
        updateOrder.setUpdateTime(LocalDateTime.now());
        updateOrder.setFinishTime(LocalDateTime.now());
        boolean updated = this.updateById(updateOrder);

        if (!updated) {
            log.error("确认收货失败: 更新订单状态失败, orderId={}", orderId);
            throw new RuntimeException("确认收货失败");
        }

        // 5. 记录订单状态变更历史
        OrderStatusHistory history = new OrderStatusHistory();
        history.setOrderId(orderId);
        history.setOrderStatus(OrderStatus.COMPLETED.getCode());
        history.setRemark("用户确认收货");
        history.setUpdateTime(LocalDateTime.now());
        this.save(history);

        log.info("确认收货成功: orderId={}", orderId);
        return true;
    }

    @Override
    public boolean deleteOrder(Long orderId, Long userId) {
        log.info("删除订单: orderId={}, userId={}", orderId, userId);

        // 1. 查询订单
        Order order = this.getById(orderId);
        if (order == null) {
            log.error("删除订单失败: 订单不存在, orderId={}", orderId);
            throw new RuntimeException("订单不存在");
        }

        // 2. 验证订单所有权
        if (!order.getUserId().equals(userId)) {
            log.error("删除订单失败: 订单不属于当前用户, orderId={}, userId={}", orderId, userId);
            throw new RuntimeException("无权操作此订单");
        }

        // 3. 验证订单状态（通常只有已完成、已取消的订单可删除）
        Integer status = order.getStatus();
        if (!OrderStatus.COMPLETED.getCode().equals(status) && !OrderStatus.CANCELLED.getCode().equals(status)) {
            log.error("删除订单失败: 当前状态不允许删除, orderId={}, status={}", orderId, status);
            throw new RuntimeException("当前订单状态不可删除");
        }

        // 4. 逻辑删除订单
        Order updateOrder = new Order();
        updateOrder.setId(orderId);
        updateOrder.setIsDeleted(1); // 标记为已删除
        updateOrder.setUpdateTime(LocalDateTime.now());
        boolean updated = this.updateById(updateOrder);

        log.info("删除订单{}: orderId={}", updated ? "成功" : "失败", orderId);
        return updated;
    }

    @Override
    public List<OrderItem> getByOrderId(Long orderId) {
        log.info("查询订单项: orderId={}", orderId);
        //TODO
        // 这里应该使用OrderItemMapper查询，需要注入OrderItemService或OrderItemMapper
        // 简化处理，返回空列表
        log.warn("getByOrderId方法需要实现，目前返回空列表");
        return new ArrayList<>();
    }

    @Override
    public Object queryUserOrders(Long userId, OrderQueryDTO queryDTO) {
        log.info("查询用户订单: userId={}, queryDTO={}", userId, queryDTO);

        // 设置查询条件
        queryDTO.setUserId(userId);

        // 调用通用查询方法
        Page<Order> orderPage = this.queryOrders(queryDTO);

        // 可以在这里对结果进行特殊处理，如加载订单项信息等
        List<Order> orders = orderPage.getRecords();
        for (Order order : orders) {
            List<OrderItem> orderItems = this.getByOrderId(order.getId());
            order.setOrderItems(orderItems);
        }

        log.info("查询用户订单成功: userId={}, total={}", userId, orderPage.getTotal());
        return orderPage;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Object payOrder(Long orderId, Integer payType) {
        log.info("支付订单: orderId={}, payType={}", orderId, payType);

        // 1. 查询订单
        Order order = this.getById(orderId);
        if (order == null) {
            log.error("支付订单失败: 订单不存在, orderId={}", orderId);
            throw new RuntimeException("订单不存在");
        }

        // 2. 验证订单状态
        if (!OrderStatus.WAIT_PAY.getCode().equals(order.getStatus())) {
            log.error("支付订单失败: 订单状态错误, orderId={}, status={}", orderId, order.getStatus());
            throw new RuntimeException("当前订单状态不可支付");
        }
        //TODO
        // 3. 调用支付接口（模拟）
        // PayResult payResult = paymentService.pay(order.getOrderNo(), order.getPayAmount(), payType);
        // if (!payResult.isSuccess()) {
        //     log.error("支付订单失败: 支付接口调用失败, orderId={}, reason={}", orderId, payResult.getMessage());
        //     throw new RuntimeException("支付失败: " + payResult.getMessage());
        // }

        // 4. 更新订单状态
        Order updateOrder = new Order();
        updateOrder.setId(orderId);
        updateOrder.setStatus(OrderStatus.WAIT_DELIVER.getCode());
        updateOrder.setPayType(payType);
        updateOrder.setPayTime(LocalDateTime.now());
        updateOrder.setUpdateTime(LocalDateTime.now());
        boolean updated = this.updateById(updateOrder);

        if (!updated) {
            log.error("支付订单失败: 更新订单状态失败, orderId={}", orderId);
            throw new RuntimeException("支付订单失败");
        }

        // 5. 记录订单状态变更历史
        OrderStatusHistory history = new OrderStatusHistory();
        history.setOrderId(orderId);
        history.setOrderStatus(OrderStatus.WAIT_DELIVER.getCode());
        history.setRemark("用户完成支付，等待发货");
        history.setUpdateTime(LocalDateTime.now());
        this.save(history);

        log.info("支付订单成功: orderId={}", orderId);
        return "支付成功";
    }

    @Override
    public List<OrderStatusHistory> getHistoryByOrderId(Long orderId) {
        log.info("查询订单状态历史: orderId={}", orderId);
        //TODO
        // 这里应该使用OrderStatusHistoryMapper查询
        // 简化处理，返回空列表
        log.warn("getHistoryByOrderId方法需要实现，目前返回空列表");
        return new ArrayList<>();
    }
    @Override
    public void save(OrderStatusHistory history) {
        log.info("保存订单状态历史: orderStatusHistory={}", history);
        //TODO
        // 这里应该使用OrderStatusHistoryMapper保存
        // 实际实现需要注入OrderStatusHistoryMapper或其Service
        log.warn("save(OrderStatusHistory)方法需要实现");
    }
}