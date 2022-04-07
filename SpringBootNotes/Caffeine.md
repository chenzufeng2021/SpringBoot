---
typora-copy-images-to: SpringBootNotesPictures
---

# 本地缓存组件 Caffeine

Redis 这种 NoSql 作为分布式缓存组件，能提供多个服务间的缓存，但是 Redis 需要网络开销，增加时耗。本地缓存是直接从本地内存中读取，没有网络开销，例如秒杀系统或者数据量小的缓存等，比远程缓存更合适。

在下面缓存组件中 Caffeine 性能是其中最好的：

![Caffeine性能](SpringBootNotesPictures/Caffeine性能.png)

## Caffeine 参数配置

| 参数              |   类型   | 描述                                                         |
| ----------------- | :------: | ------------------------------------------------------------ |
| initialCapacity   | integer  | 初始的缓存空间大小                                           |
| maximumSize       |   long   | 缓存的最大条数                                               |
| maximumWeight     |   long   | 缓存的最大权重                                               |
| expireAfterAccess | duration | 最后一次写入或访问后，指定经过多长的时间过期                 |
| expireAfterWrite  | duration | 最后一次写入后，指定经过多长的时间缓存过期                   |
| refreshAfterWrite | duration | 创建缓存或者最近一次更新缓存后，经过指定的时间间隔后刷新缓存 |
| weakKeys          | boolean  | 打开 key 的弱引用                                            |
| weakValues        | boolean  | 打开 value 的弱引用                                          |
| softValues        | boolean  | 打开 value 的软引用                                          |
| recordStats       |    -     | 开发统计功能                                                 |

**注意：**

- `weakValues` 和 `softValues` 不可以同时使用。
- `maximumSize` 和 `maximumWeight` 不可以同时使用。
- `expireAfterWrite` 和 `expireAfterAccess` 同时存在时，以 `expireAfterWrite` 为准。

## 写入缓存策略

Caffeine有三种缓存写入策略：`手动`、`同步加载`和`异步加载`。

## 缓存值的清理策略

Caffeine有三种缓存值的清理策略：`基于大小`、`基于时间`和`基于引用`。

`基于容量`：当缓存大小超过配置的大小限制时会发生回收。

`基于时间`：

1. 写入后到期策略。
2. 访问后过期策略。
3. 到期时间由 Expiry 实现独自计算。

`基于引用`：启用基于缓存键值的垃圾回收。

- Java有四种引用：强引用，软引用，弱引用和虚引用，caffeine可以将值封装成弱引用或软引用。
- 软引用：如果一个对象只具有软引用，则内存空间足够，垃圾回收器就不会回收它；如果内存空间不足了，就会回收这些对象的内存。
- 弱引用：在垃圾回收器线程扫描它所管辖的内存区域的过程中，一旦发现了只具有弱引用的对象，不管当前内存空间足够与否，都会回收它的内存。

```java
// 软引用
Caffeine.newBuilder().softValues().build();

// 弱引用
Caffeine.newBuilder().weakKeys().weakValues().build();
```

## 缓存淘汰算法

缓存淘汰算法的作用是在有限的资源内，尽可能识别出哪些数据在短时间会被重复利用，从而提高缓存的命中率。常用的缓存淘汰算法有LRU、LFU、FIFO等。

FIFO：先进先出。选择最先进入的数据优先淘汰。

LRU：最近最少使用。选择最近最少使用的数据优先淘汰。

- LRU（Least Recently Used）算法认为最近访问过的数据将来被访问的几率也更高。

- LRU通常使用链表来实现，如果数据添加或者被访问到则把数据移动到链表的头部，链表的头部为热数据，链表的尾部如冷数据，当数据满时，淘汰尾部的数据。

LFU：最不经常使用。选择在一段时间内被使用次数最少的数据优先淘汰。

- LFU（Least Frequently Used）算法根据数据的历史访问频率来淘汰数据，其核心思想是“如果数据过去被访问多次，那么将来被访问的频率也更高”。根据LFU的思想，如果想要实现这个算法，需要额外的一套存储用来存每个元素的访问次数，会造成内存资源的浪费。

Caffeine采用了一种结合LRU、LFU优点的算法：`W-TinyLFU`，其特点：高命中率、低内存占用。

# 使用 Caffeine 方法实现缓存

直接引入 Caffeine 依赖，然后使用 Caffeine 方法实现缓存。

## 实现步骤

### 依赖

```xml
<dependency>
    <groupId>com.github.ben-manes.caffeine</groupId>
    <artifactId>caffeine</artifactId>
</dependency>
```

完整依赖：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>2.3.7.RELEASE</version>
        <relativePath/> <!-- lookup parent from repository -->
    </parent>

    <groupId>org.example</groupId>
    <artifactId>CaffineCache</artifactId>
    <version>1.0-SNAPSHOT</version>

    <properties>
        <java.version>1.8</java.version>
    </properties>
    
    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <dependency>
            <groupId>com.github.ben-manes.caffeine</groupId>
            <artifactId>caffeine</artifactId>
        </dependency>

        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
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
    
    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>

</project>
```

### 缓存配置类

可以直接在配置文件中进行配置，也可以使用`JavaConfig`的配置方式来代替配置文件：

```java
package com.example.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
public class CaffeineCacheConfig {
    @Bean
    public Cache<String, Object> caffeineCache() {
        // 对Caffeine缓存特性进行设置
        return Caffeine.newBuilder()
                // 设置最后一次写入或访问后经过固定时间过期
                .expireAfterWrite(60, TimeUnit.SECONDS)
                // 初始的缓存空间大小
                .initialCapacity(100)
                // 缓存的最大条数
                .maximumSize(100)
                .build();
    }
}
```

### 实体对象和服务接口

实体对象：

```java
package com.example.entity;

import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class UserInfo {
    private Integer id;
    private String name;
    private String sexual;
    private Integer age;
}
```

服务接口：

```java
package com.example.service;

import com.example.entity.UserInfo;

public interface UserInfoService {
    void addUserInfo(UserInfo userInfo);

    UserInfo getUserInfoById(Integer id);

    UserInfo updateUserInfo(UserInfo userInfo);

    void deleteUserInfoById(Integer id);
}
```

### 服务接口实现类

```java
package com.example.service.impl;

import com.example.entity.UserInfo;
import com.example.service.UserInfoService;
import com.github.benmanes.caffeine.cache.Cache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;

@Slf4j
@Service
public class UserInfoServiceImpl implements UserInfoService {

    /**
     * 模拟数据库存储数据
     */
    private HashMap<Integer, UserInfo> userInfoHashMap = new HashMap<>();

    /**
     * 使用 caffeineCache bean
     */
    @Autowired
    private Cache<String, Object> caffeineCache;

    @Override
    public void addUserInfo(UserInfo userInfo) {
        log.info("addUserInfo");
        userInfoHashMap.put(userInfo.getId(), userInfo);
        // 加入缓存中
        caffeineCache.put(String.valueOf(userInfo.getId()), userInfo);
    }

    @Override
    public UserInfo getUserInfoById(Integer id) {
        log.info("试图先从缓存中读取");
        // Object ifPresent = caffeineCache.getIfPresent(String.valueOf(id));
        UserInfo userInfo = (UserInfo) caffeineCache.asMap().get(String.valueOf(id));
        if (userInfo != null) {
            return userInfo;
        }

        // 如果缓存不存在，则从数据库中获取
        log.info("缓存中没有，则从数据库中获取");
        userInfo = userInfoHashMap.get(id);
        // 加入缓存中
        if (userInfo != null) {
            caffeineCache.put(String.valueOf(userInfo.getId()), userInfo);
        }
        return userInfo;
    }

    @Override
    public UserInfo updateUserInfo(UserInfo userInfo) {
        log.info("updateUserInfo");
        if (userInfoHashMap.containsKey(userInfo.getId())) {
            return null;
        }

        UserInfo oldUserInfo = userInfoHashMap.get(userInfo.getId());
        oldUserInfo.setAge(userInfo.getAge());
        oldUserInfo.setName(userInfo.getName());
        oldUserInfo.setSexual(userInfo.getSexual());
        userInfoHashMap.put(oldUserInfo.getId(), oldUserInfo);
        caffeineCache.put(String.valueOf(oldUserInfo.getId()), oldUserInfo);
        return oldUserInfo;
    }

    @Override
    public void deleteUserInfoById(Integer id) {
        log.info("deleteUserInfoById");
        if (caffeineCache.getIfPresent(String.valueOf(id)) != null) {
            userInfoHashMap.remove(id);
            // 从缓存中删除
            caffeineCache.asMap().remove(String.valueOf(id));
        }
    }
}
```

### Controller 和启动类

Controller 类：

```java
package com.example.controller;

import com.example.entity.UserInfo;
import com.example.service.UserInfoService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Api(tags = "caffeine controller")
@RestController
@RequestMapping("/caffeine")
public class UserInfoController {
    @Autowired
    private UserInfoService userInfoService;

    @ApiOperation("get")
    @GetMapping("/userInfo")
    public UserInfo getUserInfoById(@RequestParam Integer id) {
        UserInfo userInfo = userInfoService.getUserInfoById(id);
        return userInfo;
    }

    @ApiOperation("add")
    @PostMapping("/add")
    public String addUserInfo(@RequestBody UserInfo userInfo) {
        userInfoService.addUserInfo(userInfo);
        return "Success";
    }

    @ApiOperation("update")
    @PostMapping("/update")
    public UserInfo updateUserInfo(@RequestBody UserInfo userInfo) {
        return userInfoService.updateUserInfo(userInfo);
    }

    @ApiOperation("delete")
    @GetMapping("/delete")
    public String deleteUserInfoById(@RequestParam Integer id) {
        userInfoService.deleteUserInfoById(id);
        return "SUCCESS";
    }
}
```

启动类：

```java
package com.example;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class CaffeineApplication {
    public static void main(String[] args) {
        SpringApplication.run(CaffeineApplication.class, args);
    }
}
```

# 使用 SpringCache 注解实现缓存

## 实现步骤

### 依赖

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-cache</artifactId>
</dependency>

<dependency>
    <groupId>com.github.ben-manes.caffeine</groupId>
    <artifactId>caffeine</artifactId>
</dependency>
```

完整依赖：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>2.3.7.RELEASE</version>
        <relativePath/> <!-- lookup parent from repository -->
    </parent>

    <groupId>org.example</groupId>
    <artifactId>SpringCaffeineCache</artifactId>
    <version>1.0-SNAPSHOT</version>
    
    <properties>
        <java.version>1.8</java.version>
    </properties>

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
            <groupId>com.github.ben-manes.caffeine</groupId>
            <artifactId>caffeine</artifactId>
        </dependency>

        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
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
    
    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>

</project>
```

### 缓存配置类

可以直接在配置文件中进行配置，也可以使用`JavaConfig`的配置方式来代替配置文件：

```java
package com.example.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
// @EnableCaching 注解用于开启 Springboot 的缓存功能，可以放在此处，也可以放在 application 启动类的头上。
public class CaffeineCacheConfig {
    @Bean("caffeineCacheManager")
    public CacheManager caffeineCacheManager() {
        CaffeineCacheManager caffeineCacheManager = new CaffeineCacheManager();
        // 【定制化缓存Cache
        caffeineCacheManager.setCaffeine(Caffeine.newBuilder()
                // 设置最后一次写入或访问后经过固定时间过期
                .expireAfterAccess(60, TimeUnit.SECONDS)
                // 初始的缓存空间大小
                .initialCapacity(100)
                // 缓存的最大条数
                .maximumSize(100));
        return caffeineCacheManager;
    }
}
```

### 实体对象和服务接口

实体对象：

```java
package com.example.entity;

import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class UserInfo {
    private Integer id;
    private String name;
    private String sexual;
    private Integer age;
}
```

服务接口：

```java
package com.example.service;

import com.example.entity.UserInfo;

public interface UserInfoService {
    UserInfo addUserInfo(UserInfo userInfo);

    UserInfo getUserInfoById(Integer id);

    UserInfo updateUserInfo(UserInfo userInfo);

    void deleteUserInfoById(Integer id);
}
```

### 接口实现类：注解使用

```java
package com.example.service.impl;

import com.example.entity.UserInfo;
import com.example.service.UserInfoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.HashMap;

@Slf4j
@Service
public class UserInfoServiceImpl implements UserInfoService {

    /**
     * 模拟数据库存储数据
     */
    private HashMap<Integer, UserInfo> userInfoHashMap = new HashMap<>();


    @Override
    @CachePut(key = "#userInfo.id", value = "userInfo")
    public UserInfo addUserInfo(UserInfo userInfo) {
        log.info("addUserInfo");
        userInfoHashMap.put(userInfo.getId(), userInfo);
        return userInfo;
    }

    @Override
    @Cacheable(key = "#id", value = "userInfo")
    public UserInfo getUserInfoById(Integer id) {
        log.info("缓存中没有，则从数据库中获取");
        return userInfoHashMap.get(id);

    }

    @Override
    @CachePut(key = "#userInfo.id", value = "userInfo")
    public UserInfo updateUserInfo(UserInfo userInfo) {
        log.info("updateUserInfo");
        if (userInfoHashMap.containsKey(userInfo.getId())) {
            return null;
        }

        UserInfo oldUserInfo = userInfoHashMap.get(userInfo.getId());
        oldUserInfo.setAge(userInfo.getAge());
        oldUserInfo.setName(userInfo.getName());
        oldUserInfo.setSexual(userInfo.getSexual());
        userInfoHashMap.put(oldUserInfo.getId(), oldUserInfo);
        return oldUserInfo;
    }

    @Override
    @CacheEvict(key = "#id", value = "userInfo")
    public void deleteUserInfoById(Integer id) {
        log.info("deleteUserInfoById");
        userInfoHashMap.remove(id);
    }
}
```

### controller 层

```java
package com.example.controller;

import com.example.entity.UserInfo;
import com.example.service.UserInfoService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Api(tags = "SpringCacheCaffeine controller")
@RestController
@RequestMapping("/springCacheCaffeine")
public class UserInfoController {
    @Autowired
    private UserInfoService userInfoService;

    @ApiOperation("get")
    @GetMapping("/userInfo")
    public UserInfo getUserInfoById(@RequestParam Integer id) {
        UserInfo userInfo = userInfoService.getUserInfoById(id);
        return userInfo;
    }

    @ApiOperation("add")
    @PostMapping("/add")
    public UserInfo addUserInfo(@RequestBody UserInfo userInfo) {
        return userInfoService.addUserInfo(userInfo);
    }

    @ApiOperation("update")
    @PostMapping("/update")
    public UserInfo updateUserInfo(@RequestBody UserInfo userInfo) {
        return userInfoService.updateUserInfo(userInfo);
    }

    @ApiOperation("delete")
    @GetMapping("/delete")
    public String deleteUserInfoById(@RequestParam Integer id) {
        userInfoService.deleteUserInfoById(id);
        return "SUCCESS";
    }
}
```

### 启动类

使用`@EnableCaching`注解让 Spring Boot 开启对缓存的支持：

```java
package com.example;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
// 开启缓存，需要显示的指定
@EnableCaching
public class SpringCacheCaffeineApplication {
    public static void main(String[] args) {
        SpringApplication.run(SpringCacheCaffeineApplication.class, args);
    }
}
```



## CaffeineCacheManager[^2]

在 Springboot 中<font color=red>使用 CaffeineCacheManager 管理器管理 Caffeine 类型的缓存，Caffeine 类似 Cache 缓存的工厂， 可以生产很多个 Cache 实例，Caffeine 可以设置各种缓存属性，这些 Cache 实例都共享 Caffeine 的缓存属性</font>[^3]。

`spring-context-support`提供的`CaffeineCacheManager`实现：

```java
package org.springframework.cache.caffeine;

public class CaffeineCacheManager implements CacheManager {
    // Caffeine 的各种属性都使用默认的属性，因为 Caffeine.newBuilder() 没有给 Caffeine 设置任何属性
    // 可通过setCaffeine来自定这个cacheBuilder
    private Caffeine<Object, Object> cacheBuilder = Caffeine.newBuilder();
    @Nullable
    private CacheLoader<Object, Object> cacheLoader;
    private boolean allowNullValues = true;
    // 默认能动态生成Cache：当根据名字来获取某个缓存时，如果缓存不存在，那么是否自动创建一个缓存
    private boolean dynamic = true;
    // 保存各个缓存 Cache，key 是缓存的名字，value 就是对应的缓存对象
    private final Map<String, Cache> cacheMap = new ConcurrentHashMap(16);
    private final Collection<String> customCacheNames = new CopyOnWriteArrayList();

    public CaffeineCacheManager() {
    }

    public CaffeineCacheManager(String... cacheNames) {
        this.setCacheNames(Arrays.asList(cacheNames));
    }

    ......

    @Nullable
    public Cache getCache(String name) {
        return (Cache)this.cacheMap.computeIfAbsent(name, (cacheName) -> {
            return this.dynamic ? this.createCaffeineCache(cacheName) : null;
        });
    }

    ......

    // CaffeineCache实现了org.springframework.cache.Cache接口
    // 内部实现都是委托给com.github.benmanes.caffeine.cache.Cache<Object, Object>来做
    protected Cache adaptCaffeineCache(String name, com.github.benmanes.caffeine.cache.Cache<Object, Object> cache) {
        return new CaffeineCache(name, cache, this.isAllowNullValues());
    }

    protected Cache createCaffeineCache(String name) {
        return this.adaptCaffeineCache(name, this.createNativeCaffeineCache(name));
    }
......
}
```



# 参考资料

[SpringBoot 使用 Caffeine 本地缓存](http://www.mydlq.club/article/56/)

[^2]:[玩转Spring Cache --- 整合进程缓存之王Caffeine Cache](https://fangshixiang.blog.csdn.net/article/details/94982916)
[^3]:[Springboot Caffeine 详解（一篇就明白）](https://blog.csdn.net/dgh112233/article/details/119009366)



[Caffeine高性能设计剖析](https://albenw.github.io/posts/a4ae1aa2/)

[Caffeine本地缓存详解](https://blog.csdn.net/w727655308/article/details/121623776)

[Caffeine本地缓存详解（一篇就明白）](https://blog.csdn.net/dgh112233/article/details/118915259)：属性

[Caffeine Cache-高性能Java本地缓存组件](https://www.cnblogs.com/rickiyang/p/11074158.html)：原理

Caffeine 详解 —— Caffeine 使用：https://zhuanlan.zhihu.com/p/329684099——参数

Caffeine缓存：https://www.jianshu.com/p/9a80c662dac4——策略等
