package com.travelshop.service.impl.order;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.travelshop.dto.OrderStateChangeDTO;
import com.travelshop.entity.Order;
import com.travelshop.enums.OperatorType;
import com.travelshop.enums.OrderStatus;
import com.travelshop.service.IOrderService;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderStateAutoTransferTask {

    private final IOrderService orderService;
    private final OrderStateMachine orderStateMachine;

    /**
     * 自动取消超时未支付订单
     * 注册为XXL-Job任务，支持分片
     */
    @XxlJob("autoCancelUnpaidOrdersJob")
    public void autoCancelUnpaidOrders() {
        log.info("开始执行超时未支付订单自动取消任务");
        // 获取分片参数
        int shardIndex = XxlJobHelper.getShardIndex();
        int shardTotal = XxlJobHelper.getShardTotal();
        log.info("分片参数: 当前分片={}, 总分片={}", shardIndex, shardTotal);

        // 设置支付超时时间（例如：30分钟）
        LocalDateTime deadline = LocalDateTime.now().minusMinutes(30);

        // 查询超时未支付的订单
        LambdaQueryWrapper<Order> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Order::getStatus, OrderStatus.WAIT_PAY.getCode())
                .lt(Order::getCreateTime, deadline)
                // 通过对订单号取模实现分片
                .apply(shardTotal > 0, "MOD(id, {0}) = {1}", shardTotal, shardIndex);

        List<Order> orders = orderService.list(queryWrapper);

        // 批量处理超时订单
        int successCount = 0;
        int failCount = 0;
        for (Order order : orders) {
            try {
                // 构建状态变更参数
                OrderStateChangeDTO changeDTO = new OrderStateChangeDTO();
                changeDTO.setOrderId(order.getId());
                changeDTO.setTargetStatus(OrderStatus.CANCELLED.getCode());
                changeDTO.setOperator("system");
                changeDTO.setOperatorType(OperatorType.SYSTEM);
                changeDTO.setReason("支付超时，系统自动取消");

                // 调用状态机进行状态变更
                orderStateMachine.changeState(changeDTO);
                log.info("订单[{}]超时未支付，已自动取消", order.getOrderNo());
                successCount++;
            } catch (Exception e) {
                log.error("取消超时订单[{}]失败", order.getOrderNo(), e);
                failCount++;
            }
        }

        // 记录处理结果到XXL-Job日志
        XxlJobHelper.log("超时未支付订单自动取消任务执行完成，共处理{}个订单，成功{}个，失败{}个",
                orders.size(), successCount, failCount);
    }


    /**
     * 自动关闭长期申请退款但未处理的订单
     * 注册为XXL-Job任务，支持分片
     */
    @XxlJob("autoCloseStaleRefundOrdersJob")
    public void autoCloseStaleRefundOrders() {
        log.info("开始执行长期退款申请订单自动关闭任务");
        // 获取分片参数
        int shardIndex = XxlJobHelper.getShardIndex();
        int shardTotal = XxlJobHelper.getShardTotal();
        log.info("分片参数: 当前分片={}, 总分片={}", shardIndex, shardTotal);

        // 设置超时时间（例如：申请退款后30天仍未处理）
        LocalDateTime deadline = LocalDateTime.now().minusDays(30);

        // 查询长期处于退款申请状态的订单
        LambdaQueryWrapper<Order> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Order::getStatus, OrderStatus.REFUND_APPLY.getCode())
                .lt(Order::getUpdateTime, deadline)
                // 通过对订单号取模实现分片
                .apply(shardTotal > 0, "MOD(id, {0}) = {1}", shardTotal, shardIndex);

        List<Order> orders = orderService.list(queryWrapper);

        // 批量处理超时订单
        int successCount = 0;
        int failCount = 0;
        for (Order order : orders) {
            try {
                // 构建状态变更参数
                OrderStateChangeDTO changeDTO = new OrderStateChangeDTO();
                changeDTO.setOrderId(order.getId());
                changeDTO.setTargetStatus(OrderStatus.CLOSED.getCode());
                changeDTO.setOperator("system");
                changeDTO.setOperatorType(OperatorType.SYSTEM);
                changeDTO.setReason("退款申请长期未处理，系统自动关闭订单");

                // 调用状态机进行状态变更
                orderStateMachine.changeState(changeDTO);
                log.info("订单[{}]退款申请长期未处理，已自动关闭", order.getOrderNo());
                successCount++;
            } catch (Exception e) {
                log.error("自动关闭长期退款申请订单[{}]失败", order.getOrderNo(), e);
                failCount++;
            }
        }

        // 记录处理结果到XXL-Job日志
        XxlJobHelper.log("长期退款申请订单自动关闭任务执行完成，共处理{}个订单，成功{}个，失败{}个",
                orders.size(), successCount, failCount);
    }
}