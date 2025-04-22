package com.travelshop.exception;

/**
 * 订单状态异常
 */
public class OrderStateException extends RuntimeException {

    public OrderStateException(String message) {
        super(message);
    }

    public OrderStateException(String message, Throwable cause) {
        super(message, cause);
    }
}