package com.supergaos.seckill.mapper;

import com.supergaos.seckill.entity.SeckillOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface SeckillOrderMapper {
    void insert(SeckillOrder order);

    SeckillOrder findById(@Param("id") Long id);

    List<SeckillOrder> findByUserId(@Param("userId") Long userId);

    SeckillOrder findByActivityAndUser(@Param("activityId") Long activityId, @Param("userId") Long userId);
}
