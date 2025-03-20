package com.hmdp.service;

public abstract interface TicketStockService {
    boolean lockStock(Long skuId, Integer count);
}
