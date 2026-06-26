package com.supergaos.seckill.consumer;

import com.supergaos.seckill.entity.SeckillOrder;
import com.supergaos.seckill.mapper.SeckillActivityMapper;
import com.supergaos.seckill.mapper.SeckillOrderMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.Map;

@Component
public class OrderConsumer {
    private static final Logger log = LoggerFactory.getLogger(OrderConsumer.class);
    private final SeckillOrderMapper orderMapper;
    private final SeckillActivityMapper activityMapper;

    public OrderConsumer(SeckillOrderMapper orderMapper, SeckillActivityMapper activityMapper) {
        this.orderMapper = orderMapper;
        this.activityMapper = activityMapper;
    }

    @RabbitListener(queues = "seckill.order.queue")
    @Transactional
    public void handleOrder(Map<String, Object> msg) {
        Long activityId = Long.valueOf(msg.get("activityId").toString());
        Long userId = Long.valueOf(msg.get("userId").toString());
        BigDecimal amount = new BigDecimal(msg.get("amount").toString());
        activityMapper.updateStock(activityId, -1);
        SeckillOrder order = new SeckillOrder();
        order.setActivityId(activityId);
        order.setUserId(userId);
        order.setAmount(amount);
        order.setStatus(0);
        orderMapper.insert(order);
        log.info("秒杀订单创建成功: activityId={}, userId={}, orderId={}", activityId, userId, order.getId());
    }
}
