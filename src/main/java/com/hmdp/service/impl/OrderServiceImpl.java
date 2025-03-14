package com.hmdp.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.OrderCreateDTO;
import com.hmdp.dto.OrderQueryDTO;
import com.hmdp.entity.*;
import com.hmdp.enums.OrderStatus;
import com.hmdp.enums.ProductType;
import com.hmdp.mapper.*;
import com.hmdp.service.*;
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
    private final TicketSkuMapper ticketSkuMapper;
    private final ITicketService ticketService;
    private final ITicketUsageService ticketUsageService;
    private final IVoucherService voucherService;
    private final IVoucherOrderService voucherOrderService;
    private final ISeckillVoucherService seckillVoucherService;
    private final IOrderStateService orderStateService;
    private final ITicketSkuService ticketSkuService;




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

        // 根据商品类型处理不同的商品
        for (OrderCreateDTO.OrderItemDTO itemDTO : itemDTOs) {
            // 创建订单项
            OrderItem item = new OrderItem();
            item.setProductId(itemDTO.getProductId());
            item.setSkuId(itemDTO.getSkuId());
            item.setCount(itemDTO.getCount());
            item.setProductType(itemDTO.getProductType()); // 设置商品类型
            ProductType productType = ProductType.getByCode(itemDTO.getProductType());
            if (productType == null) {
                throw new RuntimeException("未知商品类型");
            }
            // 根据商品类型获取商品信息
            switch (productType) {
                case TICKET:
                    // 处理门票商品
                    TicketSku ticketSku = ticketSkuService.getById(itemDTO.getSkuId());
                    if (ticketSku == null) {
                        throw new RuntimeException("门票规格不存在");
                    }

                    // 验证库存
                    if (ticketSku.getStock() < itemDTO.getCount()) {
                        throw new RuntimeException("门票库存不足");
                    }

                    Ticket ticket = ticketService.getById(itemDTO.getProductId());
                    if (ticket == null) {
                        throw new RuntimeException("门票不存在");
                    }

                    // 设置门票商品信息
                    item.setProductName(ticket.getName());
                    item.setProductImg(ticket.getImages().split(",")[0]); // 取第一张图片
                    item.setSkuName(ticketSku.getName());
                    item.setPrice(ticketSku.getPrice());
                    break;

                case VOUCHER:
                    // 处理优惠券商品
                    Voucher voucher = voucherService.getById(itemDTO.getProductId());
                    if (voucher == null) {
                        throw new RuntimeException("优惠券不存在");
                    }

                    // 验证库存（秒杀券需要验证）
                    if (voucher.getType() == 2) { // 假设2表示秒杀券
                        SeckillVoucher seckillVoucher = seckillVoucherService.getById(itemDTO.getProductId());
                        if (seckillVoucher == null || seckillVoucher.getStock() < itemDTO.getCount()) {
                            throw new RuntimeException("优惠券库存不足");
                        }
                    }

                    // 设置优惠券商品信息
                    item.setProductName(voucher.getTitle());
                    item.setSkuName("标准券");
                    item.setPrice(BigDecimal.valueOf(voucher.getPayValue()));
                    break;
                default:
                    throw new RuntimeException("未知商品类型");
            }

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

        for (OrderItem item : orderItems) {
            // 根据商品类型进行后续处理
            if (item.getProductType() == ProductType.TICKET.getCode()) {
                // 生成门票使用记录
                for (int i = 0; i < item.getCount(); i++) {
                    TicketUsage usage = new TicketUsage();
                    usage.setOrderId(order.getId());
                    usage.setOrderItemId(item.getId());
                    usage.setTicketId(item.getProductId());
                    usage.setTicketSkuId(item.getSkuId());
                    usage.setUserId(order.getUserId());
                    usage.setCode(generateTicketCode()); // 生成唯一核销码
                    usage.setStatus(1); // 未使用状态

                    // 计算过期时间
                    Ticket ticket = ticketService.getById(item.getProductId());
                    if (ticket.getIsTimeLimited() && ticket.getEffectiveDays() != null) {
                        usage.setExpireTime(LocalDateTime.now().plusDays(ticket.getEffectiveDays()));
                    }

                    usage.setCreateTime(LocalDateTime.now());
                    usage.setUpdateTime(LocalDateTime.now());
                    ticketUsageService.save(usage);
                }
            } else if (item.getProductType() == ProductType.VOUCHER.getCode()) {
                // 处理优惠券核销逻辑
                // 创建优惠券订单记录
                VoucherOrder voucherOrder = new VoucherOrder();
                voucherOrder.setUserId(order.getUserId());
                voucherOrder.setVoucherId(item.getProductId());
                voucherOrder.setPayType(order.getPayType());
                voucherOrder.setStatus(1); // 未使用状态
                voucherOrder.setCreateTime(LocalDateTime.now());
                voucherOrder.setUpdateTime(LocalDateTime.now());
                voucherOrderService.save(voucherOrder);
            }
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
            if (item.getProductType() == ProductType.TICKET.getCode()) {
                // 恢复门票库存
                ticketSkuMapper.increaseStock(item.getSkuId(), item.getCount());
                // 更新门票使用记录状态
                ticketUsageService.markAsRefunded(orderId);
            } else if (item.getProductType() == ProductType.VOUCHER.getCode()) {
                // 恢复优惠券库存
                seckillVoucherService.update()
                        .setSql("stock = stock + " + item.getCount())
                        .eq("voucher_id", item.getProductId())
                        .update();

                // 更新优惠券订单状态
                VoucherOrder voucherOrder = voucherOrderMapper.getByOrderItemId(item.getId());
                if (voucherOrder != null) {
                    voucherOrderService.refundVoucher(voucherOrder.getId());
                }
            }
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

        // 3. 更新订单状态
        Order updateOrder = new Order();
        updateOrder.setId(orderId);
        updateOrder.setStatus(OrderStatus.PAID.getCode());
        updateOrder.setUpdateTime(LocalDateTime.now());
        updateOrder.setPayTime(LocalDateTime.now());
        updateOrder.setPayType(payType);
        boolean updated = this.updateById(updateOrder);

        if (!updated) {
            log.error("支付订单失败: 更新订单状态失败, orderId={}", orderId);
            throw new RuntimeException("支付订单失败");
        }

        // 4. 处理订单项相关业务
        List<OrderItem> orderItems = this.getByOrderId(orderId);
        for (OrderItem item : orderItems) {
            if (item.getProductType() == ProductType.TICKET.getCode()) {
                // 减少门票库存，增加销量
                ticketSkuMapper.decreaseStock(item.getSkuId(), item.getCount());

                // 生成门票使用凭证
                for (int i = 0; i < item.getCount(); i++) {
                    TicketUsage usage = new TicketUsage();
                    usage.setOrderId(orderId);
                    usage.setOrderItemId(item.getId());
                    usage.setTicketId(item.getProductId());
                    usage.setTicketSkuId(item.getSkuId());
                    usage.setUserId(order.getUserId());
                    usage.setCode(generateTicketCode()); // 生成唯一核销码
                    usage.setStatus(1); // 未使用状态

                    // 计算过期时间
                    Ticket ticket = ticketService.getById(item.getProductId());
                    if (ticket.getIsTimeLimited() && ticket.getEffectiveDays() != null) {
                        usage.setExpireTime(LocalDateTime.now().plusDays(ticket.getEffectiveDays()));
                    }

                    usage.setCreateTime(LocalDateTime.now());
                    usage.setUpdateTime(LocalDateTime.now());
                    ticketUsageService.save(usage);
                }
            } else if (item.getProductType() == ProductType.VOUCHER.getCode()) {
                // 更新优惠券订单状态
                voucherOrderMapper.updateStatusToPaid(item.getId(), LocalDateTime.now());
            }
        }


        // 5. 记录订单状态变更历史
        OrderStatusHistory history = new OrderStatusHistory();
        history.setOrderId(orderId);
        history.setOrderStatus(OrderStatus.WAIT_DELIVER.getCode());
        history.setRemark("用户完成支付，等待发货");
        history.setUpdateTime(LocalDateTime.now());
        orderStateService.save(history);

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
}