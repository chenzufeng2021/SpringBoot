package com.example;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author chenzufeng
 * @date 2022/4/14
 */
@RestController
@RequestMapping("/redisson")
public class RedissonController {
    @GetMapping("test")
    @CacheEvict(value = "redisson", key = "#id")
    public void test(Integer id) {
        System.out.println("删除缓存！");
    }
}
