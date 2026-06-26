package com.supergaos.seckill.consumer;

import com.supergaos.seckill.entity.SeckillOrder;
import com.supergaos.seckill.mapper.SeckillActivityMapper;
import com.supergaos.seckill.mapper.SeckillOrderMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderConsumerTest {

    @Mock
    private SeckillOrderMapper orderMapper;
    @Mock
    private SeckillActivityMapper activityMapper;

    @InjectMocks
    private OrderConsumer orderConsumer;

    @Test
    void consume_shouldCreateOrder() {
        Map<String, Object> msg = Map.of(
                "activityId", 1L,
                "userId", 100L,
                "amount", "0.01",
                "timestamp", System.currentTimeMillis()
        );

        orderConsumer.handleOrder(msg);

        ArgumentCaptor<SeckillOrder> captor = ArgumentCaptor.forClass(SeckillOrder.class);
        verify(orderMapper).insert(captor.capture());
        SeckillOrder order = captor.getValue();
        assertEquals(1L, order.getActivityId());
        assertEquals(100L, order.getUserId());
        assertEquals(0, new BigDecimal("0.01").compareTo(order.getAmount()));
        assertEquals(0, order.getStatus());
    }

    @Test
    void consume_shouldUpdateStockInDb() {
        Map<String, Object> msg = Map.of(
                "activityId", 1L,
                "userId", 200L,
                "amount", "0.01",
                "timestamp", System.currentTimeMillis()
        );

        orderConsumer.handleOrder(msg);

        verify(activityMapper).updateStock(1L, -1);
    }
}
