package com.travelshop.utils;

import com.travelshop.service.impl.TicketStockServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 系统启动时预热热门门票数据
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TicketPreloadRunner implements ApplicationRunner {

    private final TicketHeatManager heatManager;
    private final TicketStockServiceImpl ticketStockService;

    @Override
    public void run(ApplicationArguments args) {
        log.info("系统启动，开始预热热门门票数据...");

        try {
            // 获取所有热门门票ID
            List<Long> hotTicketIds = heatManager.getAllHotTicketIds();
            log.info("发现{}个热门门票需要预热", hotTicketIds.size());

            if (!hotTicketIds.isEmpty()) {
                // 预热门票库存
                ticketStockService.preloadTicketsStock(hotTicketIds);
            }

            log.info("热门门票数据预热完成");
        } catch (Exception e) {
            log.error("热门门票数据预热失败", e);
        }
    }
}