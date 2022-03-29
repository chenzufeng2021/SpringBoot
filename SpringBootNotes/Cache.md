# 参考资料

## 多级缓存

SpringBoot+SpringCache实现两级缓存(Redis+Caffeine)：https://www.cnblogs.com/cnndevelop/p/13429660.html

基于Spring Cache实现二级缓存(Caffeine+Redis)：https://www.cnblogs.com/qdhxhz/p/16029596.html——业界已经存在封装了 Caffeine 和 Redis 的二级缓存组件

品味Spring Cache设计之美：https://zhuanlan.zhihu.com/p/444504022——Spring Cache不是一个具体的缓存实现方案，而是一个对缓存使用的抽象；Spring Cache并没有二级缓存的实现



Spring Boot缓存实战 Redis + Caffeine 实现多级缓存：https://www.jianshu.com/p/ef9042c068fd（重要）！！！



# Spring Cache

SpringBoot整合Spring Cache，简化分布式缓存开发：https://xiaoliang.blog.csdn.net/article/details/118794044——**缓存问题**：缓存穿透、击穿、雪崩

缓存使用的思考：https://juejin.cn/post/6844904016615309326——遇到一些稍微复杂的需求，仅仅依靠 Spring Cache 的注解是远远不够的，我们需要自己去操作 cache 对象。如果使用原生 API 就非常简单了，能应对不同的需求。







## Redis

SpringBoot整合Spring Cache，简化分布式缓存开发：https://xiaoliang.blog.csdn.net/article/details/118794044

SpringBoot整合Redis，一篇解决缓存的所有问题：https://xiaoliang.blog.csdn.net/article/details/118677483



SpringBoot整合Redis做缓存，实战分享：https://blog.csdn.net/singwhatiwanna/article/details/107194161



# Caffine

Caffeine 详解 —— Caffeine 使用：https://zhuanlan.zhihu.com/p/329684099——参数

Caffeine缓存：https://www.jianshu.com/p/9a80c662dac4——策略等





## Spring缓存注解@Cacheable,@CachePut , @CacheEvict使用

[SpringBoot如何集成Caffeine？ - SegmentFault 思否](https://segmentfault.com/a/1190000040912996)

[Spring Boot缓存注解@Cacheable、@CacheEvict、@CachePut使用_Dreamhai的博客-CSDN博客](https://blog.csdn.net/dreamhai/article/details/80642010)



[^1]: dii



