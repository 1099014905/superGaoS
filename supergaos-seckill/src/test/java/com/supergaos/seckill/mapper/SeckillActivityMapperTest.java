package com.supergaos.seckill.mapper;

import com.supergaos.seckill.entity.SeckillActivity;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

@MybatisTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class SeckillActivityMapperTest {

    @Autowired
    private SeckillActivityMapper activityMapper;

    @Test
    void insertAndFindById() {
        SeckillActivity activity = new SeckillActivity();
        activity.setTitle("测试秒杀");
        activity.setPrice(new BigDecimal("0.01"));
        activity.setStock(100);
        activity.setStartTime(LocalDateTime.now());
        activity.setEndTime(LocalDateTime.now().plusHours(1));
        activity.setStatus(0);
        activityMapper.insert(activity);
        assertNotNull(activity.getId());

        SeckillActivity found = activityMapper.findById(activity.getId());
        assertEquals("测试秒杀", found.getTitle());
        assertEquals(100, found.getStock());
    }

    @Test
    void findAll_returnsNonEmptyList() {
        SeckillActivity activity = new SeckillActivity();
        activity.setTitle("列表测试");
        activity.setPrice(new BigDecimal("1.00"));
        activity.setStock(50);
        activity.setStartTime(LocalDateTime.now());
        activity.setEndTime(LocalDateTime.now().plusHours(2));
        activity.setStatus(0);
        activityMapper.insert(activity);

        List<SeckillActivity> list = activityMapper.findAll();
        assertFalse(list.isEmpty());
    }
}
