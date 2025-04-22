package com.travelshop.utils;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 订单编号生成器
 */
@Component
public class OrderNoGenerator {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    /**
     * 生成订单编号
     * 规则：年月日时分秒 + 4位随机数
     */
    public String generate() {
        String dateTime = LocalDateTime.now().format(FORMATTER);
        String random = String.format("%04d", ThreadLocalRandom.current().nextInt(10000));
        return dateTime + random;
    }
}