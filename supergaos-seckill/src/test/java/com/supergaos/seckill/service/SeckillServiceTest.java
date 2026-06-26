package com.supergaos.seckill.service;

import com.supergaos.common.exception.BusinessException;
import com.supergaos.seckill.entity.SeckillActivity;
import com.supergaos.seckill.entity.SeckillOrder;
import com.supergaos.seckill.mapper.SeckillActivityMapper;
import com.supergaos.seckill.mapper.SeckillOrderMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SeckillServiceTest {

    @Mock
    private SeckillActivityMapper activityMapper;
    @Mock
    private SeckillOrderMapper orderMapper;
    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private DefaultRedisScript<Long> seckillScript;
    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private SeckillService seckillService;

    @Test
    void grab_whenActivityNotFound_shouldThrow() {
        when(activityMapper.findById(1L)).thenReturn(null);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> seckillService.grab(1L, 100L));
        assertTrue(ex.getMessage().contains("活动不存在"));
    }

    @Test
    void grab_whenActivityNotStarted_shouldThrow() {
        SeckillActivity activity = new SeckillActivity();
        activity.setStartTime(LocalDateTime.now().plusHours(2));
        activity.setEndTime(LocalDateTime.now().plusHours(3));
        when(activityMapper.findById(1L)).thenReturn(activity);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> seckillService.grab(1L, 100L));
        assertTrue(ex.getMessage().contains("未开始"));
    }

    @Test
    void grab_whenActivityEnded_shouldThrow() {
        SeckillActivity activity = new SeckillActivity();
        activity.setStartTime(LocalDateTime.now().minusHours(3));
        activity.setEndTime(LocalDateTime.now().minusHours(2));
        when(activityMapper.findById(1L)).thenReturn(activity);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> seckillService.grab(1L, 100L));
        assertTrue(ex.getMessage().contains("已结束"));
    }

    @Test
    void grab_whenLuaReturnsSuccess_shouldSendMQ() {
        SeckillActivity activity = new SeckillActivity();
        activity.setId(1L);
        activity.setPrice(new BigDecimal("0.01"));
        activity.setTitle("测试商品");
        activity.setStock(100);
        activity.setStartTime(LocalDateTime.now().minusMinutes(10));
        activity.setEndTime(LocalDateTime.now().plusHours(1));
        when(activityMapper.findById(1L)).thenReturn(activity);
        when(redisTemplate.execute(eq(seckillScript), anyList(), anyString()))
                .thenReturn(1L);

        String result = seckillService.grab(1L, 100L);

        assertEquals("抢购成功，订单处理中", result);
        verify(rabbitTemplate).convertAndSend(anyString(), isA(Map.class));
    }

    @Test
    void grab_whenLuaReturnsDuplicate_shouldThrow() {
        SeckillActivity activity = new SeckillActivity();
        activity.setId(1L);
        activity.setTitle("测试商品");
        activity.setStock(100);
        activity.setStartTime(LocalDateTime.now().minusMinutes(10));
        activity.setEndTime(LocalDateTime.now().plusHours(1));
        when(activityMapper.findById(1L)).thenReturn(activity);
        when(redisTemplate.execute(eq(seckillScript), anyList(), anyString()))
                .thenReturn(-1L);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> seckillService.grab(1L, 100L));
        assertTrue(ex.getMessage().contains("已参与过"));
    }

    @Test
    void grab_whenLuaReturnsSoldOut_shouldThrow() {
        SeckillActivity activity = new SeckillActivity();
        activity.setId(1L);
        activity.setTitle("测试商品");
        activity.setStock(0);
        activity.setStartTime(LocalDateTime.now().minusMinutes(10));
        activity.setEndTime(LocalDateTime.now().plusHours(1));
        when(activityMapper.findById(1L)).thenReturn(activity);
        when(redisTemplate.execute(eq(seckillScript), anyList(), anyString()))
                .thenReturn(-2L);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> seckillService.grab(1L, 100L));
        assertTrue(ex.getMessage().contains("已售罄"));
    }
}
