package com.example;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author chenzufeng
 * @date 2022/4/16
 */
@RestController
public class Service1 {
    @GetMapping("/test")
    @CacheEvict(value = "test", key = "'caffeine'")
    public void test() {
        System.out.println("删除缓存!");
    }
}
