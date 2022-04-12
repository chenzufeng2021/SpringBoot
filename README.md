https://www.bookstack.cn/read/redisson-wiki-zh/spilt.9.14.-%E7%AC%AC%E4%B8%89%E6%96%B9%E6%A1%86%E6%9E%B6%E6%95%B4%E5%90%88.md
# 集成Redis

[浅析SpringBoot缓存原理探究、SpringCache常用注解介绍及如何集成Redis](https://itcn.blog/p/1648146775684444.html)
SpringBoot2.x系列教程之中利用Redis实现缓存功能详细教程：https://blog.csdn.net/GUDUzhongliang/article/details/122053095

# 博客

[Spring源码学习笔记汇总](https://czwer.github.io/2018/06/06/Spring%E6%BA%90%E7%A0%81%E5%AD%A6%E4%B9%A0%E7%AC%94%E8%AE%B0%E6%B1%87%E6%80%BB/)：重要
博客学习：https://xiaolyuh.blog.csdn.net/?type=blog（从2017.08.11开始看）-> https://github.com/wyh-spring-ecosystem-student/spring-boot-student/tree/releases
SpringBoot2.x架构教程：https://blog.csdn.net/thinkingcao/category_9281035.html


# 方向盘（重要）
[Spring Cache的缓存抽象与JSR107缓存抽象JCache，并使用API方式使用Spring Cache](https://fangshixiang.blog.csdn.net/article/details/94446903)

[开启基于注解的缓存功能@EnableCaching原理](https://fangshixiang.blog.csdn.net/article/details/94562018)

[@Cacheable/@CachePut/@CacheEvict缓存注解相关基础类](https://fangshixiang.blog.csdn.net/article/details/94603480)

[@Cacheable/@CachePut/@CacheEvict注解的原理深度剖析和使用](https://fangshixiang.blog.csdn.net/article/details/94570960)
	处理缓存注解的步骤总结

[整合进程缓存之王Caffeine Cache](https://fangshixiang.blog.csdn.net/article/details/94982916)

[整合分布式缓存Redis Cache（使用Lettuce、使用Spring Data Redis）](https://fangshixiang.blog.csdn.net/article/details/95047822)

[扩展缓存注解支持失效时间TTL](https://fangshixiang.blog.csdn.net/article/details/95234347)

# 二级缓存

[品味Spring Cache设计之美](https://mp.weixin.qq.com/s/o8RvO14LEzHCB7R44LLZmw)：集成Caffine、Redisson；自定义二级缓存
[Spring Cache(二) - 自定义两级缓存（Caffeine+Redis）](https://juejin.cn/post/6907242584738021384#heading-7)
Spring Boot缓存实战 Redis + Caffeine 实现多级缓存：https://xiaolyuh.blog.csdn.net/article/details/78866184（重要）
caffeine + redis自定义二级缓存：https://www.jianshu.com/p/d9358e7a6afc（重要）
基于Spring接口，集成Caffeine+Redis两级缓存：https://www.cnblogs.com/trunks2008/p/16105077.html（重点参考）
Redis+Caffeine两级缓存，让访问速度纵享丝滑：https://www.cnblogs.com/trunks2008/p/16065501.html
SpringBoot+SpringCache实现两级缓存(Redis+Caffeine)：https://www.cnblogs.com/cnndevelop/p/13429660.html

# AOP

Spring AOP实现原理简介：https://blog.csdn.net/wyl6019/article/details/80136000

# SpringCache

CacheAble、CachePut、CacheEvict的注解底层逻辑解析：https://blog.csdn.net/qq_43719932/article/details/112651226
Spring Cache(一) - Cache在springboot中的实现与原理：https://juejin.cn/post/6904553882861436936
Spring Cache，从入门到真香：https://juejin.cn/post/6882196005731696654#heading-14
SpringBoot2.x—SpringCache（3） CacheManager源码：https://www.jianshu.com/p/ef8fb285ed72（极为重要！！！）
SpringBoot中Cache缓存的使用：https://blog.csdn.net/weixin_36279318/article/details/82820880


# Caffeine

Caffeine 使用与原理：https://www.daimajiaoliu.com/daima/56a188d3c54cc06

# Git

## HTTPS/SSH

HTTPS：[https://github.com/chenzufeng2021/SpringBoot.git]()

SSH：[git@github.com:chenzufeng2021/SpringBoot.git]()

## Git 全局设置

```markdown
git config --global user.name "Chenzufeng"
git config --global user.email "chenzufeng@outlook.com"
```

## 创建 git 仓库

```markdown
echo "# SpringBoot" >> README.md
git init
git add README.md
git commit -m "first commit"
git branch -M main
git remote add origin git@github.com:chenzufeng2021/SpringBoot.git
git push -u origin main
```

## 已有仓库

```markdown
git remote add origin git@github.com:chenzufeng2021/SpringBoot.git
git branch -M main
git push -u origin main
```

# 待归档文档

[Spring Boot 2.X(七)：Spring Cache 使用 - 掘金 (juejin.cn)](https://juejin.cn/post/6844903966615011335)：完善基本的注解信息

[史上超详细的SpringBoot整合Cache使用教程 | Java入门教程_springboot基础教程_springcloud_jvm学习交流网站【Java实验室】 (javawu.com)](https://javawu.com/archives/1731)：涉及SpringCache浅层次的原理

[Spring cache原理详解 - 掘金 (juejin.cn)](https://juejin.cn/post/6959002694539444231#heading-8)

[(42条消息) SpringCache实现原理及核心业务逻辑（一）_不动明王1984的博客-CSDN博客_springcache](https://blog.csdn.net/m0_37962779/article/details/78671468)

[Spring -- Cache原理 - 云+社区 - 腾讯云 (tencent.com)](https://cloud.tencent.com/developer/article/1580633)

[玩转Spring Cache](https://fangshixiang.blog.csdn.net/category_7941357_3.html)：重要

https://blog.csdn.net/qq_43719932/article/details/112651226

AOP：

[(42条消息) Spring AOP实现原理简介_豹变的博客-CSDN博客_springaop实现原理](https://blog.csdn.net/wyl6019/article/details/80136000)

[(42条消息) 细说Spring——AOP详解（AOP概览）_Jivan2233的博客-CSDN博客_aop](https://blog.csdn.net/q982151756/article/details/80513340)

