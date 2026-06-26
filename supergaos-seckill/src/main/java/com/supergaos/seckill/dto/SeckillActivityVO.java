package com.supergaos.seckill.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class SeckillActivityVO {
    private Long id;
    private String title;
    private BigDecimal price;
    private Integer stock;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer status;
    private LocalDateTime createTime;
}
