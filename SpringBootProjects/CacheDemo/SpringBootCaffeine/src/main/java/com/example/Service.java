package com.example;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author chenzufeng
 * @date 2022/4/16
 */
@RestController
public class Service {
    @GetMapping("/test")
    @Cacheable(value = "test", key = "'caffeine'")
    public void test() {
        System.out.println("没有走缓存!");
    }

    @GetMapping("delete")
    @CacheEvict(value = "test", key = "'caffeine'")
    public void delete() {
        System.out.println("删除缓存！");
    }
}
