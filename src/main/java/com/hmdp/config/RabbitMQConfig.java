package com.hmdp.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // 定义交换机名称
    public static final String DB_CHANGE_EXCHANGE = "db.change.exchange";
    // 定义队列名称
    public static final String DB_CHANGE_QUEUE = "db.change.queue";
    // 定义路由键
    public static final String DB_CHANGE_ROUTING_KEY = "db.change";

    /**
     * 声明直连交换机
     */
    @Bean
    public DirectExchange dbChangeExchange() {
        return new DirectExchange(DB_CHANGE_EXCHANGE, true, false);
    }

    /**
     * 声明队列
     */
    @Bean
    public Queue dbChangeQueue() {
        return new Queue(DB_CHANGE_QUEUE, true, false, false);
    }

    /**
     * 将队列绑定到交换机
     */
    @Bean
    public Binding bindingDbChangeQueue() {
        return BindingBuilder.bind(dbChangeQueue())
                .to(dbChangeExchange())
                .with(DB_CHANGE_ROUTING_KEY);
    }

    /**
     * 配置消息转换器
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(new Jackson2JsonMessageConverter());
        return rabbitTemplate;
    }
}