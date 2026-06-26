package com.supergaos.seckill.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@DisabledIfEnvironmentVariable(named = "SKIP_REDIS_TESTS", matches = "true")
class RedisLuaTest {

    @Autowired
    private StringRedisTemplate redisTemplate;

    private final DefaultRedisScript<Long> script = new DefaultRedisScript<>();

    {
        script.setScriptSource(new ResourceScriptSource(new ClassPathResource("lua/seckill.lua")));
        script.setResultType(Long.class);
    }

    @Test
    void luaScript_shouldDeductStockAndRecordUser() {
        String stockKey = "seckill:stock:test:1";
        String usersKey = "seckill:users:test:1";
        redisTemplate.opsForValue().set(stockKey, "10");

        Long result = redisTemplate.execute(script, List.of(stockKey, usersKey), "1001");
        assertEquals(1L, result);

        String remaining = redisTemplate.opsForValue().get(stockKey);
        assertEquals("9", remaining);

        redisTemplate.delete(stockKey);
        redisTemplate.delete(usersKey);
    }

    @Test
    void luaScript_shouldRejectDuplicateUser() {
        String stockKey = "seckill:stock:test:2";
        String usersKey = "seckill:users:test:2";
        redisTemplate.opsForValue().set(stockKey, "10");

        redisTemplate.execute(script, List.of(stockKey, usersKey), "2001");
        Long result = redisTemplate.execute(script, List.of(stockKey, usersKey), "2001");
        assertEquals(-1L, result);

        redisTemplate.delete(stockKey);
        redisTemplate.delete(usersKey);
    }

    @Test
    void luaScript_shouldRejectWhenSoldOut() {
        String stockKey = "seckill:stock:test:3";
        String usersKey = "seckill:users:test:3";
        redisTemplate.opsForValue().set(stockKey, "0");

        Long result = redisTemplate.execute(script, List.of(stockKey, usersKey), "3001");
        assertEquals(-2L, result);

        redisTemplate.delete(stockKey);
        redisTemplate.delete(usersKey);
    }
}
