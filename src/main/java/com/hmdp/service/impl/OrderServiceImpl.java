package com.hmdp.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.OrderCreateDTO;
import com.hmdp.dto.OrderQueryDTO;
import com.hmdp.dto.Result;
import com.hmdp.entity.*;
import com.hmdp.enums.OrderStatus;
import com.hmdp.enums.ProductType;
import com.hmdp.mapper.*;
import com.hmdp.service.*;
import com.hmdp.service.strategy.ProductTypeHandler;
import com.hmdp.service.strategy.ProductTypeHandlerFactory;
import com.hmdp.utils.UserHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl extends ServiceImpl<OrderMapper, Order> implements IOrderService {


    private final OrderItemMapper orderItemMapper;
    private final OrderMapper orderMapper;
    private final TicketUsageMapper ticketUsageMapper;
    private final VoucherOrderMapper voucherOrderMapper;
    private final IVoucherService voucherService;
    private final IVoucherOrderService voucherOrderService;
    private final ISeckillVoucherService seckillVoucherService;
    private final IOrderStateService orderStateService;
    private final ProductTypeHandlerFactory productTypeHandlerFactory;
    private final PaymentService paymentService;



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

        // 4. 查询商品信息、计算金额
        List<OrderCreateDTO.OrderItemDTO> itemDTOs = createDTO.getOrderItems();
        List<OrderItem> orderItems = new ArrayList<>(itemDTOs.size());

        // 处理不同的商品类型
        for (OrderCreateDTO.OrderItemDTO itemDTO : itemDTOs) {
            // 创建订单项
            OrderItem item = new OrderItem();
            item.setProductId(itemDTO.getProductId());
            item.setSkuId(itemDTO.getSkuId());
            item.setCount(itemDTO.getCount());
            item.setProductType(itemDTO.getProductType());

            // 获取对应的商品类型处理策略
            ProductTypeHandler handler = productTypeHandlerFactory.getHandler(itemDTO.getProductType());

            // 验证商品有效性
            handler.validateProduct(itemDTO);

            // 设置订单项信息
            handler.setupOrderItem(item, itemDTO);

            // 计算总价
            item.setTotalAmount(item.getPrice().multiply(new BigDecimal(itemDTO.getCount())));
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

        boolean itemsSaved = orderMapper.saveBatch(orderItems);
        if (!itemsSaved) {
            log.error("保存订单项失败");
            throw new RuntimeException("保存订单项失败");
        }

        // 9. 执行各商品类型的后续处理
        for (OrderItem item : orderItems) {
            ProductTypeHandler handler = productTypeHandlerFactory.getHandler(item.getProductType());
            handler.processAfterOrderCreation(order, item);
        }

        return order.getId();
    }
    // 生成唯一票码
    private String generateTicketCode() {
        // 生成唯一票码的逻辑，可以使用UUID等
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
    }


    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean cancelOrder(Long orderId, Long userId, String reason) {
        log.info("开始取消订单: orderId={}, userId={}, reason={}", orderId, userId, reason);

        // 1. 查询订单
        Order order = getById(orderId);
        if (order == null) {
            log.error("取消订单失败: 订单不存在, orderId={}", orderId);
            throw new RuntimeException("订单不存在");
        }

        // 2. 验证订单所有权
        if (!order.getUserId().equals(userId)) {
            log.error("取消订单失败: 订单不属于当前用户, orderId={}, userId={}", orderId, userId);
            throw new RuntimeException("无权操作此订单");
        }

        // 3. 验证订单状态是否可取消
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
        boolean updated = updateById(updateOrder);

        if (!updated) {
            log.error("取消订单失败: 更新订单状态失败, orderId={}", orderId);
            throw new RuntimeException("取消订单失败");
        }

        // 5. 恢复库存
        List<OrderItem> orderItems = orderItemMapper.selectByOrderId(orderId);
        for (OrderItem item : orderItems) {
            ProductTypeHandler handler = productTypeHandlerFactory.getHandler(item.getProductType());
            handler.processAfterCancellation(order, item);
        }

        // 6. 记录订单状态变更历史
        OrderStatusHistory history = new OrderStatusHistory();
        history.setOrderId(orderId);
        history.setOrderStatus(OrderStatus.CANCELLED.getCode());
        history.setRemark("用户取消订单: " + reason);
        history.setUpdateTime(LocalDateTime.now());
        orderStateService.save(history);

        log.info("取消订单成功: orderId={}", orderId);
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

        // 检查排序参数的有效性
        if (!queryDTO.isValidSortField() || !queryDTO.isValidSortDirection()) {
            throw new IllegalArgumentException("排序参数无效");
        }

        // 规范化状态过滤器
        queryDTO.normalizeStatusFilter();

        // 使用自定义查询
        List<Order> orders = orderMapper.queryOrdersByCondition(page, queryDTO);
        page.setRecords(orders);

        // 为每个订单加载订单项
        for (Order order : orders) {
            List<OrderItem> orderItems = orderItemMapper.selectByOrderId(order.getId());
            order.setOrderItems(orderItems);

            // 为每个订单项加载额外信息
            for (OrderItem item : orderItems) {
                if (item.getProductType() == ProductType.TICKET.getCode()) {
                    // 加载门票使用记录
                    TicketUsage usage = ticketUsageMapper.getByOrderItemId(item.getId());
                    // 可以将使用记录信息设置到自定义字段或扩展对象中
                    if (usage != null) {
                        item.setRemark("核销码: " + usage.getCode() +
                                ", 状态: " + getTicketStatusDesc(usage.getStatus()));
                    }
                } else if (item.getProductType() == ProductType.VOUCHER.getCode()) {
                    // 加载优惠券使用记录
                    VoucherOrder voucherOrder = voucherOrderMapper.getByOrderItemId(item.getId());
                    if (voucherOrder != null) {
                        item.setRemark("优惠券状态: " + getVoucherStatusDesc(voucherOrder.getStatus()));
                    }
                }
            }
        }

        return page;
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
        log.info("订单支付请求: orderId={}, payType={}", orderId, payType);

        // 委托给专门的支付服务处理
        Result result = paymentService.createPayment(orderId, payType);

        if (result.getSuccess()) {
            return result.getData();
        } else {
            throw new RuntimeException("支付订单失败: " + result.getErrorMsg());
        }
    }

    // 获取门票状态描述
    private String getTicketStatusDesc(Integer status) {
        switch (status) {
            case 1: return "未使用";
            case 2: return "已使用";
            case 3: return "已过期";
            case 4: return "已退款";
            default: return "未知状态";
        }
    }

    // 获取优惠券状态描述
    private String getVoucherStatusDesc(Integer status) {
        switch (status) {
            case 1: return "未使用";
            case 2: return "已使用";
            case 3: return "已过期";
            case 4: return "已退款";
            default: return "未知状态";
        }
    }

    @Override
    public Result seckillVoucher(long voucherId) {
        // 1. 查询优惠券信息
        Voucher voucher = voucherService.getById(voucherId);
        if (voucher == null) {
            return Result.fail("优惠券不存在");
        }

        // 2. 判断是否是秒杀券
        if (voucher.getType() != 2) {
            return Result.fail("不是秒杀券");
        }

        // 3. 查询秒杀券详情
        SeckillVoucher seckillVoucher = seckillVoucherService.getById(voucherId);
        if (seckillVoucher == null) {
            return Result.fail("秒杀券不存在");
        }

        // 4. 判断秒杀是否开始或已结束
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(seckillVoucher.getBeginTime())) {
            return Result.fail("秒杀未开始");
        }
        if (now.isAfter(seckillVoucher.getEndTime())) {
            return Result.fail("秒杀已结束");
        }

        // 5. 判断库存是否充足
        if (seckillVoucher.getStock() < 1) {
            return Result.fail("库存不足");
        }

        // 6. 扣减库存（注意：实际场景应考虑并发安全，可使用乐观锁或分布式锁）
        boolean success = seckillVoucherService.update()
                .setSql("stock = stock - 1")
                .eq("voucher_id", voucherId)
                .gt("stock", 0)
                .update();
        if (!success) {
            return Result.fail("库存不足");
        }

        // 7. 创建优惠券订单
        VoucherOrder voucherOrder = new VoucherOrder();
        voucherOrder.setUserId(UserHolder.getUser().getId());
        voucherOrder.setVoucherId(voucherId);
        voucherOrder.setPayType(1); // 默认支付类型
        voucherOrder.setStatus(1);  // 未使用状态
        voucherOrder.setCreateTime(now);
        voucherOrder.setUpdateTime(now);
        voucherOrderService.save(voucherOrder);

        // 8. 返回订单ID
        return Result.ok(voucherOrder.getId());
    }

}