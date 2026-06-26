package com.supergaos.seckill.entity;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class SeckillOrder {
    private Long id;
    private Long activityId;
    private Long userId;
    private BigDecimal amount;
    private Integer status;
    private LocalDateTime createTime;
}
