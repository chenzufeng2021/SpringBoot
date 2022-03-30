package com.example;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

/**
 * @author chenzufeng
 * @date 2022/3/31
 */
@SpringBootTest
public class RedisApplicationTest {
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Test
    void testRedis() {
        ValueOperations<String, Object> stringObjectValueOperations = redisTemplate.opsForValue();
        stringObjectValueOperations.set("Hello", "Redis");

        System.out.println(stringObjectValueOperations.get("Hello"));
    }

    @Test
    void testString() {
        stringRedisTemplate.opsForValue().set("StringKey", "Redis");
        System.out.println(stringRedisTemplate.opsForValue().get("StringKey"));
    }
}
