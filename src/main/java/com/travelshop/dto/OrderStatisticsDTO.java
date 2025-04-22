package com.travelshop.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.travelshop.enums.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单统计DTO
 * 用于订单数量和金额等统计数据传输
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OrderStatisticsDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 订单状态
     */
    private Integer status;

    /**
     * 状态名称
     */
    private String statusName;

    /**
     * 该状态的订单数量
     */
    private Integer count;

    /**
     * 该状态的订单总金额
     */
    private BigDecimal totalAmount;

    /**
     * 统计开始时间
     */
    private LocalDateTime startTime;

    /**
     * 统计结束时间
     */
    private LocalDateTime endTime;

    /**
     * 日期/日期区间描述(如: "今日"、"本周"、"本月")
     */
    private String timePeriod;

    /**
     * 环比上期增长率(%)
     */
    private BigDecimal growthRate;

    /**
     * 用户ID(用于用户个人订单统计)
     */
    private Long userId;

    /**
     * 商户ID(用于商户订单统计)
     */
    private Long merchantId;

    /**
     * 支付方式(1-支付宝 2-微信支付 3-银联)
     */
    private Integer payType;

    /**
     * 支付方式名称
     */
    private String payTypeName;

    /**
     * 订单来源(1-APP 2-PC 3-小程序)
     */
    private Integer source;

    /**
     * 订单来源名称
     */
    private String sourceName;

    /**
     * 设置状态和状态名称
     */
    public OrderStatisticsDTO setStatusWithName(Integer status) {
        this.status = status;
        OrderStatus orderStatus = OrderStatus.getByCode(status);
        this.statusName = orderStatus != null ? orderStatus.getDesc() : "未知状态";
        return this;
    }

    /**
     * 设置支付方式名称
     */
    public OrderStatisticsDTO setPayTypeWithName(Integer payType) {
        this.payType = payType;
        switch (payType) {
            case 1:
                this.payTypeName = "支付宝";
                break;
            case 2:
                this.payTypeName = "微信支付";
                break;
            case 3:
                this.payTypeName = "银联";
                break;
            default:
                this.payTypeName = "其他";
        }
        return this;
    }

    /**
     * 设置订单来源名称
     */
    public OrderStatisticsDTO setSourceWithName(Integer source) {
        this.source = source;
        switch (source) {
            case 1:
                this.sourceName = "APP";
                break;
            case 2:
                this.sourceName = "PC";
                break;
            case 3:
                this.sourceName = "小程序";
                break;
            default:
                this.sourceName = "其他";
        }
        return this;
    }

    /**
     * 构建"今日"统计数据
     */
    public static OrderStatisticsDTO buildTodayStatistics(Integer count, BigDecimal totalAmount) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfDay = now.toLocalDate().atStartOfDay();

        return OrderStatisticsDTO.builder()
                .count(count)
                .totalAmount(totalAmount)
                .startTime(startOfDay)
                .endTime(now)
                .timePeriod("今日")
                .build();
    }

    /**
     * 构建"本周"统计数据
     */
    public static OrderStatisticsDTO buildThisWeekStatistics(Integer count, BigDecimal totalAmount) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfWeek = now.toLocalDate().minusDays(now.getDayOfWeek().getValue() - 1).atStartOfDay();

        return OrderStatisticsDTO.builder()
                .count(count)
                .totalAmount(totalAmount)
                .startTime(startOfWeek)
                .endTime(now)
                .timePeriod("本周")
                .build();
    }

    /**
     * 构建"本月"统计数据
     */
    public static OrderStatisticsDTO buildThisMonthStatistics(Integer count, BigDecimal totalAmount) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfMonth = now.toLocalDate().withDayOfMonth(1).atStartOfDay();

        return OrderStatisticsDTO.builder()
                .count(count)
                .totalAmount(totalAmount)
                .startTime(startOfMonth)
                .endTime(now)
                .timePeriod("本月")
                .build();
    }

    /**
     * 计算环比增长率
     * @param current 当前周期数值
     * @param previous 上一周期数值
     * @return 增长率(%)
     */
    public static BigDecimal calculateGrowthRate(BigDecimal current, BigDecimal previous) {
        if (previous == null || previous.compareTo(BigDecimal.ZERO) == 0) {
            return current != null && current.compareTo(BigDecimal.ZERO) > 0
                    ? new BigDecimal("100") // 从0增长到有值，定义为100%增长
                    : BigDecimal.ZERO;
        }

        if (current == null) {
            current = BigDecimal.ZERO;
        }

        // (current - previous) / previous * 100
        return current.subtract(previous)
                .divide(previous, 4, BigDecimal.ROUND_HALF_UP)
                .multiply(new BigDecimal("100"))
                .setScale(2, BigDecimal.ROUND_HALF_UP);
    }
}