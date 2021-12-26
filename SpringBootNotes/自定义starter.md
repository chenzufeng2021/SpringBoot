# 前言

## SpringBoot starter 机制

SpringBoot 中的 starter 是一种非常重要的机制，能够抛弃以前繁杂的配置，将其统一集成进 starter，应用者只需要<font color=red>在 maven 中引入 starter 依赖</font>，SpringBoot 就能自动扫描到要加载的信息并启动相应的默认配置。

starter 让我们摆脱了各种依赖库的处理，需要配置各种信息的困扰。<font color=red>SpringBoot 会自动通过 classpath 路径下的类发现需要的 Bean，并注册进 IOC 容器</font>。

SpringBoot 提供了针对日常企业应用研发各种场景的 spring-boot-starter 依赖模块。所有这些依赖模块都遵循着约定成俗的默认配置，并允许我们调整这些配置，即遵循“约定大于配置”的理念。

## 为什么要自定义 starter

在我们的日常开发工作中，经常会有一些==独立于业务之外的配置模块==，我们经常将其放到一个特定的包下，然后如果另一个工程需要复用这块功能的时候，需要将代码硬拷贝到另一个工程，重新集成一遍，麻烦至极。

如果我们<font color=red>将这些可独立于业务代码之外的功配置模块封装成一个个 starter，复用的时候只需要将其在 pom 中引用依赖即可</font>，SpringBoot 为我们完成自动装配。

# 参考资料

[1] [SpringBoot应用篇（一）：自定义starter ](https://www.cnblogs.com/hello-shf/p/10864977.html)