package com.hmdp.service.strategy.impl;

import com.hmdp.dto.OrderCreateDTO;
import com.hmdp.entity.Order;
import com.hmdp.entity.OrderItem;
import com.hmdp.entity.Ticket;
import com.hmdp.entity.TicketSku;
import com.hmdp.entity.TicketUsage;
import com.hmdp.enums.ProductType;
import com.hmdp.service.ITicketService;
import com.hmdp.service.ITicketSkuService;
import com.hmdp.service.ITicketUsageService;
import com.hmdp.service.strategy.ProductTypeHandler;
import com.hmdp.mapper.TicketSkuMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 门票商品处理策略实现
 */
@Component
@RequiredArgsConstructor
public class TicketProductHandler implements ProductTypeHandler {

    private final ITicketService ticketService;
    private final ITicketSkuService ticketSkuService;
    private final ITicketUsageService ticketUsageService;
    private final TicketSkuMapper ticketSkuMapper;

    @Override
    public ProductType getProductType() {
        return ProductType.TICKET;
    }

    @Override
    public void validateProduct(OrderCreateDTO.OrderItemDTO itemDTO) {
        // 验证门票有效性和库存
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
    }

    @Override
    public void setupOrderItem(OrderItem item, OrderCreateDTO.OrderItemDTO itemDTO) {
        // 获取门票和规格信息
        Ticket ticket = ticketService.getById(itemDTO.getProductId());
        TicketSku ticketSku = ticketSkuService.getById(itemDTO.getSkuId());

        // 设置门票商品信息
        item.setProductName(ticket.getName());
        item.setProductImg(ticket.getImages().split(",")[0]); // 取第一张图片
        item.setSkuName(ticketSku.getName());
        item.setPrice(ticketSku.getPrice());
    }

    @Override
    public void processAfterOrderCreation(Order order, OrderItem item) {
        // 创建订单时不需要处理门票业务，支付后再处理
    }

    @Override
    public void processAfterPayment(Order order, OrderItem item) {
        // 支付成功后，减少门票库存，增加销量
        ticketSkuMapper.decreaseStock(item.getSkuId(), item.getCount());

        // 生成门票使用凭证
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
    }

    @Override
    public void processAfterCancellation(Order order, OrderItem item) {
        // 恢复门票库存
        ticketSkuMapper.increaseStock(item.getSkuId(), item.getCount());
        // 更新门票使用记录状态
        ticketUsageService.markAsRefunded(order.getId());
    }

    /**
     * 生成唯一票码
     */
    private String generateTicketCode() {
        // 生成唯一票码的逻辑，可以使用UUID等
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
    }
}