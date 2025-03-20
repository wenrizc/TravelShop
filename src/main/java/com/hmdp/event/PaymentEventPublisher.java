package com.hmdp.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * 支付事件发布者
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventPublisher {

    private final ApplicationEventPublisher eventPublisher;

    /**
     * 发布支付事件
     *
     * @param event 支付事件
     */
    public void publishEvent(PaymentEvent event) {
        log.info("发布支付事件: {}", event);
        eventPublisher.publishEvent(event);
    }
}