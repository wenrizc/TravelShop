package com.travelshop.service;

public abstract interface TicketStockService {
    boolean lockStock(Long skuId, Integer count);
}
