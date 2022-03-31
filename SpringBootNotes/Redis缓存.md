---
typora-copy-images-to: SpringBootNotesPictures
---

# SpringBoot 整合 Redis[^1]

## 实现步骤

### 依赖

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
```

完整依赖：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>org.example</groupId>
    <artifactId>SpringBootRedis</artifactId>
    <version>1.0-SNAPSHOT</version>

    <properties>
        <maven.compiler.source>8</maven.compiler.source>
        <maven.compiler.target>8</maven.compiler.target>
    </properties>

    <parent>
        <artifactId>spring-boot-starter-parent</artifactId>
        <groupId>org.springframework.boot</groupId>
        <version>2.6.4</version>
    </parent>

    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-redis</artifactId>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
        </dependency>

        <!--Jackson-->
        <dependency>
            <groupId>com.fasterxml.jackson.core</groupId>
            <artifactId>jackson-databind</artifactId>
        </dependency>
    </dependencies>

</project>
```

引入依赖后，可以查看 RedisAutoConfiguration 自动配置类：

```java
package org.springframework.boot.autoconfigure.data.redis;

@Configuration(
    proxyBeanMethods = false
)
@ConditionalOnClass({RedisOperations.class})
@EnableConfigurationProperties({RedisProperties.class})
@Import({LettuceConnectionConfiguration.class, JedisConnectionConfiguration.class})
public class RedisAutoConfiguration {
    public RedisAutoConfiguration() {
    }

    @Bean
    @ConditionalOnMissingBean(
        name = {"redisTemplate"}
    )
    @ConditionalOnSingleCandidate(RedisConnectionFactory.class)
    public RedisTemplate<Object, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<Object, Object> template = new RedisTemplate();
        template.setConnectionFactory(redisConnectionFactory);
        return template;
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnSingleCandidate(RedisConnectionFactory.class)
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory redisConnectionFactory) {
        return new StringRedisTemplate(redisConnectionFactory);
    }
}
```

### 添加配置

```properties
spring.application.name=springboot-redis

# Redis
## 服务器地址
spring.redis.host=localhost
## 服务器连接端口
spring.redis.port=6379
## 数据库索引（默认为0）
spring.redis.database=0
spring.redis.client-type=lettuce
```



### 自定义 RedisTemplate

默认情况下的模板只能支持 `RedisTemplate<String,String>`，只能存入字符串，很多时候，我们需要自定义 RedisTemplate ，==设置序列化器==，这样我们可以很方便的操作实例对象[^3]。

RedisTemplate 默认的序列化方式为 JdkSerializationRedisSerializer，会把对象序列化存储到Redis中（二进制形式），StringRedisTemplate 的默认序列化方式为 StringRedisSerializer。

绝大多数情况下，不推荐使用 JdkSerializationRedisSerializer 进行序列化，主要是不方便人工排查数据。所以我们需要切换序列化方式。

Spring Data底层为我们实现了七种不同的序列化方式：

![RedisSerializer](SpringBootNotesPictures/RedisSerializer.png)

以Jackson2JsonRedisSerializer为例，展示如何切换序列化方式：

```java
package com.example.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;

/**
 * @author chenzufeng
 * @date 2022/3/31
 */
@Configuration
public class RedisConfig {

    /**
     * 默认是JDK的序列化策略，这里配置redisTemplate采用的是Jackson2JsonRedisSerializer的序列化策略
     * @param redisConnectionFactory 连接工厂
     * @return redisTemplate
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
        // 使用Jackson2JsonRedisSerializer来序列化和反序列化redis的value值（默认使用JDK的序列化方式）
        Jackson2JsonRedisSerializer<Object> jackson2JsonRedisSerializer = new Jackson2JsonRedisSerializer<>(Object.class);

        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();

        // 配置连接工厂
        redisTemplate.setConnectionFactory(redisConnectionFactory);
        // 使用StringRedisSerializer来序列化和反序列化redis的key值
        redisTemplate.setKeySerializer(jackson2JsonRedisSerializer);
        // 值采用json序列化
        redisTemplate.setValueSerializer(jackson2JsonRedisSerializer);
        redisTemplate.setHashKeySerializer(jackson2JsonRedisSerializer);
        redisTemplate.setHashValueSerializer(jackson2JsonRedisSerializer);
        redisTemplate.afterPropertiesSet();

        return redisTemplate;
    }

    /**
     * StringRedisTemplate默认采用的是String的序列化策略
     * @param redisConnectionFactory 连接工厂
     * @return stringRedisTemplate
     */
    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory redisConnectionFactory) {
        StringRedisTemplate stringRedisTemplate = new StringRedisTemplate();
        stringRedisTemplate.setConnectionFactory(redisConnectionFactory);
        return stringRedisTemplate;
    }
}
```

### 启动类

```java
package com.example;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * @author chenzufeng
 * @date 2022/3/31
 */
@SpringBootApplication
public class RedisApplication {
    public static void main(String[] args) {
        SpringApplication.run(RedisApplication.class, args);
    }
}
```

### 测试类

```java
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
```



# SpringCache 整合 Redis[^3]

## 实现步骤

### config

```java
package com.example.config;

import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnSingleCandidate;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

/**
 * 缓存管理器：基于Lettuce操作redis的客户端
 * @date 2022/3/31
 */
@Configuration
@AutoConfigureAfter(RedisAutoConfiguration.class)
@EnableCaching
public class LettuceRedisConfig {
    /**
     * 缓存管理器
     * @param redisConnectionFactory redisConnectionFactory
     * @return cacheManager
     */
    @Bean
    @ConditionalOnSingleCandidate(RedisConnectionFactory.class)
    public CacheManager cacheManager(RedisConnectionFactory redisConnectionFactory) {
        RedisCacheConfiguration cacheConfiguration = RedisCacheConfiguration.defaultCacheConfig();
        RedisCacheConfiguration redisCacheConfiguration = cacheConfiguration
                // 设置缓存管理器管理的缓存的默认过期时间(1小时)
                .entryTtl(Duration.ofHours(1))
                // 设置key为String序列化
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                // 设置value为json序列化
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer()))
                // 不缓存空值
                .disableCachingNullValues();
        // 构造一个Redis缓存管理器
        return RedisCacheManager.builder(redisConnectionFactory)
                // 缓存配置
                .cacheDefaults(redisCacheConfiguration)
                .build();
    }

    /**
     * 自定义序列化模板
     * @param lettuceConnectionFactory lettuceConnectionFactory
     * @return redisTemplate
     */
    @Bean
    @ConditionalOnSingleCandidate(LettuceConnectionFactory.class)
    public RedisTemplate<String, String> redisTemplate(LettuceConnectionFactory lettuceConnectionFactory) {
        // 创建一个模板类
        RedisTemplate<String, String> redisTemplate = new RedisTemplate<>();
        // 设置value的序列化规则和key的序列化规则
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        // redis连接工厂，储存到模板类中
        redisTemplate.setConnectionFactory(lettuceConnectionFactory);
        return redisTemplate;
    }
}
```

### 依赖

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>org.example</groupId>
    <artifactId>SpringCacheRedis</artifactId>
    <version>1.0-SNAPSHOT</version>

    <parent>
        <artifactId>spring-boot-starter-parent</artifactId>
        <groupId>org.springframework.boot</groupId>
        <version>2.3.7.RELEASE</version>
    </parent>

    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-cache</artifactId>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-redis</artifactId>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
        </dependency>

        <!-- https://mvnrepository.com/artifact/io.swagger.core.v3/swagger-models -->
        <dependency>
            <groupId>io.springfox</groupId>
            <artifactId>springfox-swagger2</artifactId>
            <version>2.9.2</version>
        </dependency>
        <!-- https://mvnrepository.com/artifact/io.springfox/springfox-swagger-ui -->
        <dependency>
            <groupId>io.springfox</groupId>
            <artifactId>springfox-swagger-ui</artifactId>
            <version>2.9.2</version>
        </dependency>
        <!-- https://mvnrepository.com/artifact/com.github.xiaoymin/knife4j-spring-boot-starter -->
        <dependency>
            <groupId>com.github.xiaoymin</groupId>
            <artifactId>knife4j-spring-boot-starter</artifactId>
            <version>2.0.9</version>
        </dependency>
    </dependencies>

</project>
```

### controller

```java
package com.example.controller;

import com.example.service.UserService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @date 2022/3/31
 */
@RestController
@Api(tags = "SpringCacheRedis")
public class UserController {
    @Autowired
    private UserService userService;

    @ApiOperation("add")
    @GetMapping("/addUserInfo")
    public void addUserInfo(String id) {
        System.out.println(userService.addUserInfo(id));
    }

    @ApiOperation("get")
    @GetMapping("/getUserInfo")
    public void getUserInfo(String id) {
        System.out.println(userService.getUserInfo(id));
    }
}
```

### service

```java
package com.example.service;

import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

/**
 * @date 2022/3/31
 */
@Service
public class UserService {

    @CachePut(key = "#id", value = "userInfo")
    public String addUserInfo(String id) {
        System.out.println("addUserInfo没有走缓存");
        return "addUserInfo";
    }

    @Cacheable(key = "#id", value = "userInfo")
    public String getUserInfo(String id) {
        System.out.println("getUserInfo没有走缓存");
        return "getUserInfo";
    }
}
```

# 参考资料

[^1]: [SpringBoot整合Redis，一篇解决缓存的所有问题_程序猿小亮的博客-CSDN博客](https://xiaoliang.blog.csdn.net/article/details/118677483)

[SpringBoot整合Redis做缓存，实战分享](https://blog.csdn.net/singwhatiwanna/article/details/107194161)

[^2]: [SpringBoot整合Spring Cache，简化分布式缓存开发](https://blog.csdn.net/jiuqiyuliang/article/details/118794044)
[^3]:[SpringBoot学习(七):集成Redis并结合Spring Cache使用 | 猿码记 (liuqh.icu)](http://liuqh.icu/2020/09/17/springboot-7-redis/)
[^4]:[使用 Spring Cache + Redis 作为缓存 - 简书 (jianshu.com)](https://www.jianshu.com/p/931484bb3fdc)

[优雅的缓存解决方案--SpringCache和Redis集成(SpringBoot) - 掘金 (juejin.cn)](https://juejin.cn/post/6844903807646711821)

https://cloud.tencent.com/developer/article/1497594

