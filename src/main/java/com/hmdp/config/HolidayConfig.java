package com.hmdp.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 节假日规则配置
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "order.holiday")
public class HolidayConfig {

    /**
     * 延长收货确认时间的节假日列表
     */
    private List<HolidayPeriod> extendedReceivePeriods = new ArrayList<>();

    /**
     * 延长退款申请时间的节假日列表
     */
    private List<HolidayPeriod> extendedRefundPeriods = new ArrayList<>();

    /**
     * 检查当前日期是否在延长收货确认时间的节假日期间
     */
    public boolean isInExtendedReceivePeriod(LocalDate date) {
        return isInPeriods(date, extendedReceivePeriods);
    }

    /**
     * 检查当前日期是否在延长退款申请时间的节假日期间
     */
    public boolean isInExtendedRefundPeriod(LocalDate date) {
        return isInPeriods(date, extendedRefundPeriods);
    }

    /**
     * 判断日期是否在时间段内
     */
    private boolean isInPeriods(LocalDate date, List<HolidayPeriod> periods) {
        if (date == null || periods == null) {
            return false;
        }

        for (HolidayPeriod period : periods) {
            if ((date.isEqual(period.getStartDate()) || date.isAfter(period.getStartDate())) &&
                    (date.isEqual(period.getEndDate()) || date.isBefore(period.getEndDate()))) {
                return true;
            }
        }

        return false;
    }

    /**
     * 节假日时间段
     */
    @Data
    public static class HolidayPeriod {
        /**
         * 节假日名称
         */
        private String name;

        /**
         * 开始日期
         */
        private LocalDate startDate;

        /**
         * 结束日期
         */
        private LocalDate endDate;
    }
}