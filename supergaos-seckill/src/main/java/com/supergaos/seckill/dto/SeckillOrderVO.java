package com.supergaos.seckill.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class SeckillOrderVO {
    private Long id;
    private Long activityId;
    private String activityTitle;
    private BigDecimal amount;
    private Integer status;
    private LocalDateTime createTime;
}
