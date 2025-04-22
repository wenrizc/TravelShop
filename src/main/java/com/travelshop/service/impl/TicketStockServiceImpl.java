package com.travelshop.service.impl;


import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.travelshop.entity.Ticket;
import com.travelshop.entity.TicketSku;
import com.travelshop.mapper.TicketMapper;
import com.travelshop.mapper.TicketSkuMapper;
import com.travelshop.service.TicketStockService;
import com.travelshop.utils.TicketHeatManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.Collections;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class TicketStockServiceImpl extends ServiceImpl<TicketMapper, Ticket> implements TicketStockService {

    private final TicketMapper ticketMapper;
    private final TicketSkuMapper ticketSkuMapper;
    private final TicketHeatManager heatManager;
    private final RedisTemplate redisTemplate;
    private DefaultRedisScript<Long> stockScript;

    @PostConstruct
    public void init() {
        stockScript = new DefaultRedisScript<>();
        stockScript.setLocation(new ClassPathResource("stock_check.lua"));
        stockScript.setResultType(Long.class);
    }

    // 需要添加的方法，用于批量预热门票库存
    public void preloadTicketsStock(List<Long> ticketIds) {
        if (ticketIds == null || ticketIds.isEmpty()) {
            return;
        }

        log.info("开始预热{}个热门门票的库存", ticketIds.size());

        for (Long ticketId : ticketIds) {
            // 获取门票下的所有SKU
            List<TicketSku> skuList = ticketSkuMapper.queryByTicketId(ticketId);
            for (TicketSku sku : skuList) {
                String stockKey = "ticket:stock:" + sku.getId();
                // 将库存加载到Redis
                redisTemplate.opsForValue().set(stockKey, sku.getStock().toString());
                log.info("预热门票[{}]的SKU[{}]库存: {}", ticketId, sku.getId(), sku.getStock());
            }
        }
    }

    @Override
    public boolean lockStock(Long skuId, Integer count) {
        // 根据skuId获取门票ID
        TicketSku sku = ticketSkuMapper.selectById(skuId);
        if (sku == null) return false;

        boolean isHot = heatManager.isHotTicket(sku.getTicketId());

        if (isHot) {
            // 热门门票使用Redis预减库存
            String stockKey = "ticket:stock:" + skuId;

            // 检查库存是否已预加载
            Boolean hasKey = redisTemplate.hasKey(stockKey);
            if (Boolean.FALSE.equals(hasKey)) {
                // 如未预加载则实时加载
                Integer stock = ticketSkuMapper.selectStockById(skuId);
                if (stock == null || stock < count) {
                    return false;
                }
                // 设置到Redis
                redisTemplate.opsForValue().set(stockKey, stock.toString());
            }

            // 执行库存校验和预减的Lua脚本
            Object result = redisTemplate.execute(
                    stockScript,
                    Collections.singletonList(stockKey),
                    count.toString()
            );

            if (result != null && (Long) result == 1) {
                // 异步更新数据库
                asyncUpdateStock(skuId, count);
                return true;
            }
            return false;
        } else {
            // 普通门票直接操作数据库
            return ticketSkuMapper.lockStock(skuId, count) > 0;
        }
    }

    private void asyncUpdateStock(Long skuId, Integer count) {
        // 异步更新数据库库存
        // 这里可以使用消息队列或线程池等方式异步更新数据库
        log.info("异步更新SKU[{}]库存: -{}", skuId, count);
    }
}