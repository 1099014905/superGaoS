package com.supergaos.seckill.config;

import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String SECKILL_ORDER_QUEUE = "seckill.order.queue";
    public static final String SECKILL_ORDER_DLQ = "seckill.order.dlq";

    @Bean
    public Queue seckillOrderQueue() {
        return new Queue(SECKILL_ORDER_QUEUE, true);
    }

    @Bean
    public Queue seckillOrderDlq() {
        return new Queue(SECKILL_ORDER_DLQ, true);
    }
}
