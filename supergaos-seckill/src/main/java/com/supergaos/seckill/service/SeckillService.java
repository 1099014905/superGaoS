package com.supergaos.seckill.service;

import com.supergaos.common.exception.BusinessException;
import com.supergaos.seckill.config.RabbitMQConfig;
import com.supergaos.seckill.entity.SeckillActivity;
import com.supergaos.seckill.entity.SeckillOrder;
import com.supergaos.seckill.mapper.SeckillActivityMapper;
import com.supergaos.seckill.mapper.SeckillOrderMapper;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class SeckillService {

    private final SeckillActivityMapper activityMapper;
    private final SeckillOrderMapper orderMapper;
    private final StringRedisTemplate redisTemplate;
    private final DefaultRedisScript<Long> seckillScript;
    private final RabbitTemplate rabbitTemplate;

    public SeckillService(SeckillActivityMapper activityMapper,
                           SeckillOrderMapper orderMapper,
                           StringRedisTemplate redisTemplate,
                           DefaultRedisScript<Long> seckillScript,
                           RabbitTemplate rabbitTemplate) {
        this.activityMapper = activityMapper;
        this.orderMapper = orderMapper;
        this.redisTemplate = redisTemplate;
        this.seckillScript = seckillScript;
        this.rabbitTemplate = rabbitTemplate;
    }

    @PostConstruct
    public void initStock() {
        List<SeckillActivity> activities = activityMapper.findAll();
        for (SeckillActivity a : activities) {
            if (a.getStatus() == 1) { // 进行中
                preloadStock(a.getId(), a.getStock());
            }
        }
    }

    public List<SeckillActivity> listActivities() {
        return activityMapper.findAll();
    }

    public SeckillActivity getActivity(Long id) {
        SeckillActivity activity = activityMapper.findById(id);
        if (activity == null) {
            throw new BusinessException(6001, "活动不存在");
        }
        return activity;
    }

    public String grab(Long activityId, Long userId) {
        // 1. Validate activity
        SeckillActivity activity = activityMapper.findById(activityId);
        if (activity == null) {
            throw new BusinessException(6001, "活动不存在");
        }

        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(activity.getStartTime())) {
            throw new BusinessException(6002, "活动未开始");
        }
        if (now.isAfter(activity.getEndTime())) {
            throw new BusinessException(6003, "活动已结束");
        }

        // 2. Execute Redis Lua script
        String stockKey = "seckill:stock:" + activityId;
        String usersKey = "seckill:users:" + activityId;
        Long result = redisTemplate.execute(seckillScript,
                List.of(stockKey, usersKey), String.valueOf(userId));

        // 3. Handle result
        if (result == -1) {
            throw new BusinessException(6004, "您已参与过该秒杀活动");
        }
        if (result == -2) {
            throw new BusinessException(6005, "已售罄");
        }

        // 4. Send MQ message for async order creation
        Map<String, Object> msg = Map.of(
                "activityId", activityId,
                "userId", userId,
                "amount", activity.getPrice().toString(),
                "timestamp", System.currentTimeMillis()
        );
        rabbitTemplate.convertAndSend(RabbitMQConfig.SECKILL_ORDER_QUEUE, msg);

        return "抢购成功，订单处理中";
    }

    public List<SeckillOrder> getOrders(Long userId) {
        return orderMapper.findByUserId(userId);
    }

    public void preloadStock(Long activityId, int stock) {
        redisTemplate.opsForValue().set("seckill:stock:" + activityId, String.valueOf(stock));
    }

    public void clearUsers(Long activityId) {
        redisTemplate.delete("seckill:users:" + activityId);
    }
}
