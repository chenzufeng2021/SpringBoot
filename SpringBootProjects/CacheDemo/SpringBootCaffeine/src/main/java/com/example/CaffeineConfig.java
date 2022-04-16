package com.example;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * @author chenzufeng
 * @date 2022/4/16
 */
@Configuration
@EnableCaching
public class CaffeineConfig {
    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager caffeineCacheManager = new CaffeineCacheManager();
        caffeineCacheManager.setCaffeine(Caffeine.newBuilder()
                .expireAfterAccess(60, TimeUnit.MINUTES)
                .initialCapacity(100)
                .maximumSize(1000));
        return caffeineCacheManager;
    }
}
