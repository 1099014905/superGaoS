package com.supergaos.seckill.mapper;

import com.supergaos.seckill.entity.SeckillActivity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface SeckillActivityMapper {
    List<SeckillActivity> findAll();

    SeckillActivity findById(@Param("id") Long id);

    void insert(SeckillActivity activity);

    int updateStock(@Param("id") Long id, @Param("amount") int amount);
}
