---
typora-copy-images-to: SpringBootNotesPictures
---

# SpringBoot 的 starter 简介

> Starters are a set of convenient dependency descriptors that you can include in your application. You get a one-stop shop for all the Spring and related technologies that you need without having to hunt through sample code and copy-paste loads of dependency descriptors. For example, if you want to get started using Spring and JPA for database access, include the spring-boot-starter-data-jpa dependency in your project.<sup><a href="#ref2">[2]</a></sup>

在SpringBoot出现之前，如果想使用 SpringMVC 来构建 web 项目，必须要做的几件事情如下：

- 首先项目中需要引入 SpringMVC 的依赖；

- 在 web.xml 中注册 SpringMVC 的 DispatcherServlet，并配置 url 映射；

- 编写 springmcv-servlet.xml，在其中配置 SpringMVC 中几个重要的组件，处理映射器（HandlerMapping）、处理适配器（HandlerAdapter）、视图解析器（ViewResolver）；

- 在 applicationcontext.xml 文件中引入 springmvc-servlet.xml 文件；

- …

以上这几步只是配置好了 SpringMVC，如果我们还需要与数据库进行交互，就要在 application.xml 中配置数据库连接池 DataSource；如果需要数据库事务，还需要配置 TransactionManager……

这就是使用 Spring 框架开发项目带来的一些的问题：

- ==依赖导入==问题： 每个项目都需要来单独维护自己所依赖的 jar 包，在项目中使用到什么功能就需要引入什么样的依赖。手动导入依赖容易出错，且无法统一集中管理。

- ==配置繁琐==：在引入依赖之后需要做繁杂的配置，并且这些配置是每个项目来说都是必要的，例如 web.xml 配置（Listener 配置、Filter 配置、Servlet 配置）、log4j 配置、数据库连接池配置等等。这些配置重复且繁杂，在不同的项目中需要进行多次重复开发，这在很大程度上降低了我们的开发效率

而在 SpringBoot 出现之后，它为我们提供了一个强大的功能来解决上述的两个痛点，这就是 SpringBoot 的 starters（==场景启动器==，它包含了一系列可以集成到应用里面的==依赖包==）。

Spring Boot 通过将常用的功能场景抽取出来，做成的一系列场景启动器，这些启动器导入实现各个功能所需要依赖的全部组件，我们<font color=red>只需要在项目中引入这些 starters，相关场景的所有依赖就会全部被导入进来</font>，并且我们可以抛弃繁杂的配置，仅需要通过配置文件来进行少量的配置就可以使用相应的功能。

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-thymeleaf</artifactId>
</dependency>

<dependency>
    <groupId>org.mybatis.spring.boot</groupId>
    <artifactId>mybatis-spring-boot-starter</artifactId>
</dependency>
```

在Spring Boot项目POM文件中总会看到这两种依赖：`spring-boot-starter-xxx` 和 `xxx-spring-boot-starter`。

这就是Spring Boot的四大组件之一的==starter==。

**两种starter的区别是 **

- ==官方==提供的starter：==spring-boot-starter-xxx==
- ==非官方==的starter：==xxx-spring-boot-starter==

其中xxx就是我们想要依赖的组件或者jar包。上例就是我们Spring Boot用来引入thymeleaf引擎和mybatis框架所配置的==依赖==。引入之后通过简单的约定==配置==就可以正常使用：

```yaml
mybatis:
  # 注意：一定要对应mapper映射xml文件的所在路径
  mapper-locations: classpath:mapper/*.xml
  # 注意：对应实体类的路径
  type-aliases-package: com.hi.ld.vo.system  
  configuration:
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
```

## starters定义

启动场景 starter 做好了各种开源组件、自定义组件的封装，引入依赖即可使用。

## starters分类

### Spring Boot应用类启动器

| 启动器名称              | 功能描述                                                     |
| :---------------------- | :----------------------------------------------------------- |
| spring-boot-starter     | 包含自动配置、日志、YAML的支持。                             |
| spring-boot-starter-web | 使用Spring MVC构建web 工程，包含restful，默认使用Tomcat容器。 |
| ...                     | ...                                                          |

### Spring Boot生产启动器

| 启动器名称                   | 功能描述                               |
| :--------------------------- | :------------------------------------- |
| spring-boot-starter-actuator | 提供生产环境特性，能==监控管理应用==。 |

### Spring Boot技术类启动器

| 启动器名称                  | 功能描述                            |
| :-------------------------- | :---------------------------------- |
| spring-boot-starter-json    | 提供对JSON的读写支持。              |
| spring-boot-starter-logging | 默认的日志启动器，默认使用Logback。 |
| ...                         | ...                                 |



# SpringBoot 场景启动器的原理

在导入 starter 之后，SpringBoot 主要帮我们完成了两件事情：

- 相关组件的自动导入
- 相关组件的自动配置

这两件事情统一称为 ==SpringBoot 的自动配置==。



# 自定义场景启动器

- starter 开发步骤

以 RedisTemplate 自动配置为例，<font color=red>RedisAutoConfiguration 类读取 application.yml 中的相关配置，将这些配置设置到 RedisTemplate 对象中，然后再将 RedisTemplate 对象注入到 Spring 容器</font>。需要用的时候，直接从 Spring 容器中拿就可以了。

① 读取 application.yml 中的相关配置 $\rightarrow$ ② 配置设置到 RedisTemplate 对象中 $\rightarrow$ ③ 将 RedisTemplate 对象注入到 Spring 容器

- 自动装配三板斧

① 获取各组件种`META-INF/spring.factories`文件；

② 根据其中value值（其value为类名），通过反射方式创建实体类；

③ 将实体类注入到IOC容器中，待后续使用。

## starter 的命名规范

官方命名空间

- 前缀：spring-boot-starter-
- 模式：spring-boot-starter-模块名
- 举例：spring-boot-starter-web、spring-boot-starter-jdbc

自定义命名空间

- 后缀：-spring-boot-starter
- 模式：模块-spring-boot-starter
- 举例：mybatis-spring-boot-starter



## starter 模块整体结构

通过上边的介绍，可以总结 starter 的整体实现逻辑主要由两个基本部分组成：

`xxxAutoConfiguration`：==自动配置类==，对某个场景下需要使用到的一些组件进行自动注入，并利用xxxProperties类来进行组件相关配置；

`xxxProperties`：某个场景下所有==可配置属性的集成==，在配置文件中配置可以进行属性值的覆盖。

按照 SpringBoot 官方的定义，Starer 的作用就是==依赖聚合==，因此直接在 starter 内部去进行代码实现是不符合规定的，<font color=red>starter 应该只起到依赖导入的作用，而具体的代码实现应该去交给其他模块来实现，然后在 starter 中去引用该模块即可</font>，因此整体的 starter 的构成应该如下图所示：

![自定义starter](SpringBootNotesPictures/自定义starter.png)

可见 starter 模块依赖了两部分，一部分是一些==常用依赖==；另一部分就是对==自动配置模块==的依赖，而`xxxAutoConfiguration`与`xxxProperties`的具体实现，都封装在自动配置模块中，starter 实际是通过该模块来对外提供相应的功能。



## 依赖引入

首先所有的自动配置模块都要引入两个jar包依赖：

```xml
<dependencies>
     <dependency>
         <groupId>org.springframework.boot</groupId>
         <!-- 包含很多与自动配置相关的注解的定义，必须要引入 -->
         <artifactId>spring-boot-autoconfigure</artifactId>
     </dependency>
    
     <dependency>
         <groupId>org.springframework.boot</groupId>
         <!-- 非必须的，引入后可以在【配置文件】中输入我们自定义配置的时候有相应的提示，
	也可以通过其他.properties文件为相关类进行属性映射（SpringBoot默认使用application.yml)-->
         <artifactId>spring-boot-configuration-processor</artifactId>
         <optional>true</optional>
     </dependency>
 <dependencies>
```

## xxxAutoConfiguration实现

autoconfigure模块中最重要的就是==自动配置类==的编写，它为我们实现组件的自动配置与自动注入。

在编写自动配置类的时候，我们应该要考虑向容器中注入什么组件，如何去配置它：

```java
@Configuration 
// 限定自动配置类生效的一些条件
@ConditionalOnxxx
@EnableConfigurationProperties(xxxProperties.class) 
public class xxxAutoConfiguration {
    @Autowired
    private xxxProperties properties;
    
    @Bean
    public static BeanYouNeed beanYouNeed() {
        BeanYouNeed bean = new BeanYouNeed()
        bean.setField(properties.get(field));
        bean.setField(properties.get(field));
        bean.setField(properties.get(field));
        ......
        return bean;
    }
}
```

## xxxProperties的实现

这是跟配置文件相绑定的类，里边的属性就是我们可以在配置文件中配置的内容，然后通过`@ConfigurationProperties`将其与配置文件绑定：

```java
// 使用 @ConfigurationProperties 注解绑定配置文件
// 读取配置文件中的配置设置到被此注解标注的类属性
@ConfigurationProperties(prefix = "your properties prefix")
public class xxxProperties {

    private boolean enabled = true;

    private String clientId;

    private String beanName;

    private String scanBasePackage;

    private String path;

    private String token;
    
    // Getter & Setter
}
```

## 让 starter 生效<sup><a href="#ref2">[2]</a></sup>

`starter`集成应用有两种方式：

- 自动装载：通过`SpringBoot`的`SPI`的机制来去加载 starter

在`resource`目录下新建`META-INF`文件夹，在文件夹下新建`spring.factories`文件，并添加写好的`xxxAutoConfiguration`类：

```xml
org.springframework.boot.autoconfigure.EnableAutoConfiguration=\
com.meituan.xframe.boot.mcc.autoconfigure.xxxAutoConfiguration
```



- `@EnableXxx`注解：在`starter`组件集成到SpringBoot应用时需要主动声明启用该`starter`才生效

自定义一个`@Enable`注解，把自动配置类通过`Import`注解引入进来：

```java
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented

@Import({SmsAutoConfiguration.class})
public @interface EnableSms {......}
```

使用的时候需要在启动类上面开启这个注解（使用Springfox的Swagger组件，会引入`@EnableSwagger2`注解）：

```java
@SpringBootApplication
@EnableSms
public class xxxDemo{......}
```



## 使用 starter 模块

starter模块中只进行==依赖导入==，在pom文件中添加对autoconfigure模块的依赖，并添加一些其他必要的依赖项：

```xml
<dependencies>
 ================================================================
 <!--添加了对autoconfigure模块的引用-->
 <!--自定义starter-->
     <dependency>
         <groupId>com.test.starter</groupId>
         <artifactId>xxx-spring-boot-autoconfigure</artifactId>
     </dependency>
 ===============================================================
 <!--其他的一些必要依赖项-->
     <dependency>
         <groupId>commons-collections</groupId>
         <artifactId>commons-collections</artifactId>
     </dependency>
 </dependencies>
```

这两个模块都开发完成之后，通过`mvn install`命令或者`deploy`命令将包发布到==本地==或者==中央仓库==，即可直接在其他项目中引用我们自定义的starter模块了。



由于引入了依赖`spring-boot-configuration-processor`，在配置文件中配置相关属性值时，将会自动提示哪些属性可以配置，以及每个属性的注释：

![spring-boot-configuration-processor作用](SpringBootNotesPictures/spring-boot-configuration-processor作用.png)



# 参考资料

[1] [SpringBoot场景启动器（starter）原理及实践](https://blog.csdn.net/qq_21310939/article/details/107401400)

<span name="ref2">[2] [手把手教你实现自定义 Spring Boot 的 Starter](https://xie.infoq.cn/article/68621c5f5c1dc16312e0a52e4)</span>

[Spring Boot自定义starter必知必会条件](https://blog.csdn.net/u010192145/article/details/110950963)

[SpringBoot使用AutoConfiguration自定义Starter](https://cloud.tencent.com/developer/article/1152814)

 [SpringBoot应用篇（一）：自定义starter ](https://www.cnblogs.com/hello-shf/p/10864977.html)

