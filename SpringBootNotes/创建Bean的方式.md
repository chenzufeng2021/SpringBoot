---
typora-copy-images-to: SpringBootNotesPictures
---

# 引言

Spring是创建和管理bean的工厂，它提供了多种定义bean的方式。

# xml文件配置bean

`xml配置bean`是Spring最早支持的方式。随着`SpringBoot`的发展，该方法目前已经用得很少了。

## 构造器

使用无参构造器创建bean：

```xml
<bean id="personService" class="com.sue.cache.service.test7.PersonService"></bean>
```

使用有参的构造器创建bean，可以通过`<constructor-arg>`标签来完成配置：

```xml
<bean id="personService" class="com.sue.cache.service.test7.PersonService">
    <constructor-arg index="0" value="susan"></constructor-arg>
    <constructor-arg index="1" ref="baseInfo"></constructor-arg>
</bean>
```

其中：

- `index`表示下标，从0开始；
- `value`表示常量值；
- `ref`表示引用另一个bean。

## setter方法

通过setter方法设置bean所需参数，这种方式耦合性相对较低，比有参构造器使用更为广泛。

先定义Person实体：

```java
@Data
public class Person {
    private String name;
    private int age;
}
```

里面包含：成员变量name和age，getter/setter方法。

然后在`bean.xml`文件中配置bean时，加上`<property>`标签设置bean所需参数：

```xml
<bean id="person" class="com.sue.cache.service.test7.Person">
   <property name="name" value="susan"></constructor-arg>
   <property name="age" value="18"></constructor-arg>
</bean>
```

## 静态工厂

这种方式的关键是需要==定义一个工厂类==，它里面包含一个==创建bean的静态方法==：

```java
public class SusanBeanFactory {
    // 静态工厂方法
    public static Person createPerson(String name, int age) {
        return new Person(name, age);
    }
}
```

接下来定义Person类：

```java
@AllArgsConstructor
@NoArgsConstructor
@Data
public class Person {
    private String name;
    private int age;
}
```

里面包含：成员变量name和age，getter/setter方法，无参构造器和全参构造器。

然后在`bean.xml`文件中配置bean时，通过`factory-method`参数==指定静态工厂方法==，同时通过`<constructor-arg>`设置相关参数：

```xml
<bean class="com.sue.cache.service.test7.SusanBeanFactory" factory-method="createPerson">
   <constructor-arg index="0" value="susan"></constructor-arg>
   <constructor-arg index="1" value="18"></constructor-arg>
</bean>
```

## 实例工厂方法

这种方式也需要定义一个工厂类，但里面包含==非静态的创建bean==的方法：

```java
public class SusanBeanFactory {
    public Person createPerson(String name, int age) {
        return new Person(name, age);
    }
}
```

接下来定义Person类：

```java
@AllArgsConstructor
@NoArgsConstructor
@Data
public class Person {
    private String name;
    private int age;
}
```

里面包含：成员变量name和age，getter/setter方法，无参构造器和全参构造器。

然后`bean.xml`文件中配置bean时，需要==先配置工厂bean==。接着，在==配置实例bean==时，通过`factory-bean`参数指定该工厂bean的引用：

```xml
<bean id="susanBeanFactory" class="com.sue.cache.service.test7.SusanBeanFactory"></bean>

<bean factory-bean="susanBeanFactory" factory-method="createPerson">
   <constructor-arg index="0" value="susan"></constructor-arg>
   <constructor-arg index="1" value="18"></constructor-arg>
</bean>
```

## FactoryBean

上面的实例工厂方法<font color=red>每次都需要创建一个工厂类，不方面统一管理</font>。

这时可以使用`FactoryBean`接口：

```java
public class UserFactoryBean implements FactoryBean<User> {
    @Override
    public User getObject() throws Exception {
        return new User();
    }

    @Override
    public Class<?> getObjectType() {
        return User.class;
    }
}
```

在它的`getObject`方法中可以实现我们自己的逻辑创建对象，并且在`getObjectType`方法中我们可以定义对象的类型。

然后在`bean.xml`文件中配置bean时，只需像普通的bean一样配置即可：

```xml
<bean id="userFactoryBean" class="com.sue.async.service.UserFactoryBean">
</bean>
```

> 注意：
>
> - `getBean("userFactoryBean");`获取的是getObject方法中返回的对象。
> - 而`getBean("&userFactoryBean");`获取的才是真正的UserFactoryBean对象。

## 总结

通过上面五种方式，<font color=red>在bean.xml文件中把bean配置好之后，Spring就会自动扫描和解析相应的标签，并且创建和实例化bean，然后放入spring容器中</font>。

基于xml文件的方式配置bean，简单而且非常灵活，比较适合一些小项目。如果遇到比较复杂的项目，则需要配置大量的bean，而且bean之间的关系错综复杂，这样久而久之会导致xml文件迅速膨胀，非常不利于bean的管理。

# Component注解

为了解决bean太多时，xml文件过大，从而导致膨胀不好维护的问题。在Spring 2.5中开始支持：`@Component`、`@Repository`、`@Service`、`@Controller`等注解定义bean。从注解的源码可知，后三种注解也是`@Component`。

`@Component`系列注解的出现，使得我们不需要像以前那样在bean.xml文件中配置bean了。现在只用<font color=red>在类上加Component、Repository、Service、Controller，这四种注解中的任意一种</font>，就能轻松完成==bean的定义==：

```java
@Service
public class PersonService {
    public String get() {
        return "data";
    }
}
```

这四种注解在功能上没有特别的区别：

- Controller 一般用在控制层；
- Service 一般用在业务层；
- Repository 一般用在数据层；
- Component 一般用在==公共组件==上。

通过这种`@Component`扫描注解的方式定义bean的前提是：**需要先<font color=red>配置扫描路径</font>**。

目前常用的配置扫描路径的方式如下：

1. 在`applicationContext.xml`文件中使用`<context:component-scan>`标签：

```xml
<context:component-scan base-package="com.sue.cache" />
```



2. 在Springboot的启动类上加上`@ComponentScan`注解：

```java
@ComponentScan(basePackages = "com.sue.cache")
@SpringBootApplication
public class Application {
    public static void main(String[] args) {
        new SpringApplicationBuilder(Application.class).web(WebApplicationType.SERVLET).run(args);
    }
}
```



3. 直接在`SpringBootApplication`注解上加，它支持`ComponentScan`功能：

```java
@SpringBootApplication(scanBasePackages = "com.sue.cache")
public class Application {
    public static void main(String[] args) {
        new SpringApplicationBuilder(Application.class).web(WebApplicationType.SERVLET).run(args);
    }
}
```

如果<font color=red>==需要扫描的类==与Springboot的==入口类==，在==同一级或者子级==的包下面，无需指定`scanBasePackages`参数，Spring默认会从入口类的同一级或者子级的包去找</font>：

```java
@SpringBootApplication
public class Application {
    public static void main(String[] args) {
        new SpringApplicationBuilder(Application.class).web(WebApplicationType.SERVLET).run(args);
    }
}
```

除了上述四种`@Component`注解之外，Springboot还增加了`@RestController`注解，它是一种特殊的`@Controller`注解，所以也是`@Component`注解。

`@RestController`还支持`@ResponseBody`注解的功能，即<font color=red>将接口响应数据的格式自动转换成json</font>。



# JavaConfig

`@Component`系列注解虽说使用起来非常方便，但是<font color=red>bean的创建过程完全交给Spring容器来完成</font>，我们没办法自己控制。

Spring从3.0以后，开始支持JavaConfig的方式定义bean。它可以==看作Spring的配置文件==，但并非真正的配置文件，我们需要通过==编码==的方式创建bean：

```java
@Configuration
public class MyConfiguration {
    @Bean
    public Person person() {
        return new Person();
    }
}
```

在JavaConfig==类==上加`@Configuration`注解，相当于配置了`<beans>`标签。而在==方法==上加`@Bean`注解，相当于配置了`<bean>`标签。

此外，Springboot还引入了一些列的`@Conditional`注解，用来控制bean的创建：

```java
@Configuration
public class MyConfiguration {
    @ConditionalOnClass(Country.class)
    @Bean
    public Person person() {
        return new Person();
    }
}
```

`@ConditionalOnClass`注解的功能是<font color=red>当项目中存在Country类时，才实例化Person类</font>。换句话说就是，如果项目中不存在Country类，就不实例化Person类。

这个功能非常有用，<font color=red>相当于一个开关控制着Person类，只有满足一定条件才能实例化</font>。

Spring中使用比较多的Conditional还有：

- ConditionalOnBean
- ConditionalOnProperty
- ConditionalOnMissingClass
- ConditionalOnMissingBean
- ConditionalOnWebApplication

下面用一张图整体认识一下@Conditional家族:

<img src="SpringBootNotesPictures/创建bean的方式-Conditional.webp" alt="图片" style="zoom:50%;" />

# Import注解

通过前面介绍的@Configuration和@Bean相结合的方式，我们可以通过代码定义bean。

但这种方式有一定的局限性，它<font color=red>只能创建该类中定义的bean实例，不能创建其他类的bean实例</font>，如果我们想创建其他类的bean实例该怎么办呢？

这时可以使用`@Import`注解导入。

## 普通类

Spring4.2之后`@Import`注解可以实例化==普通类的bean实例==。

先定义Role类：

```java
@Data
public class Role {
    private Long id;
    private String name;
}
```

接下来使用@Import注解导入Role类：

```java
@Import(Role.class)
@Configuration
public class MyConfig {
}
```

然后在调用的地方通过`@Autowired`注解注入所需的bean：

```java
@RequestMapping("/")
@RestController
public class TestController {

    @Autowired
    private Role role;

    @GetMapping("/test")
    public String test() {
        System.out.println(role);
        return "test";
    }
}
```



# 参考资料

[1] [Spring中竟然有12种定义Bean的方法](https://mp.weixin.qq.com/s/YZT7NURQsNBSoSNsWBciQg)