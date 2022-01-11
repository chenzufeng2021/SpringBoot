# 依赖注入

什么是依赖注入？

以前使用一个对象的时候需要==new一个对象==出来，而且对象之间存在依赖关系，B类的对象可能是A类的属性，在A类中new出B对象，增加了类之间的耦合性。于是，就有了控制反转（IoC）和依赖注入（DI）的概念。

==控制反转==就是<font color=red>将对象的实例化过程交给Spring框架来做</font>；==依赖注入==就是<font color=red>将A对象所依赖的B对象以配置文件或注解的形式传递给A对象</font>。

在Spring中，你不需要自己创建对象，你只需要告诉Spring，哪些类我需要创建出对象，然后在启动项目的时候Spring就会自动帮你创建出该对象。

在SpringBoot中使用依赖注入的方式很简单，只需要添加相应的注解即可。

# 注解分类

一类是==使用Bean==，即Bean拿来用，==完成属性、方法的组装=。比如`@Autowired` , `@Resource`，可以通过`byTYPE`（@Autowired）、`byNAME`（@Resource）的方式获取Bean。一般用来修饰字段，构造函数，或者设置方法，并做注入。

一类是==注册Bean==，`@Component`、`@Repository`、`@Controller`、`@Service`、`@Configration`，一般修饰类，这些注解都是<font color=red>把你要实例化的对象转化成一个Bean，放在IoC容器中，等你要用的时候，它会和@Autowired、@Resource配合到一起，把对象、属性、方法完美组装</font>。

| 注解                     | 作用                                                         |
| ------------------------ | ------------------------------------------------------------ |
| @Service                 | 标注业务层组件                                               |
| @Controller              | 标注控制层组件                                               |
| @RestController          | Spring4之后新加入的注解，原来返回json需要@ResponseBody和@Controller配合，将调用的结果直接返回给调用者。 |
| @Repository              | 标注数据库访问Dao组件。需要在Spring中配置扫描地址，然后生成Dao层的Bean才能被注入到Service层中 |
| @Mapper                  | 标注数据库访问Dao组件。不需要配置扫描地址，通过xml里面的namespace里面的接口地址，生成了Bean后注入到Service层中。 |
| @Component               | 泛指组件，当组件不好归类的时候，我们可以使用这个注解进行标注 |
| @Autowired               | 自动注入，自动从Spring的上下文找到合适的bean来注入           |
| @Value                   | 注入application.properties配置的属性的值。                   |
| @ComponentScan           | 组件扫描，发现和组装一些Bean                                 |
| @EnableAutoConfiguration | 自动配置                                                     |
| @SpringBootApplication   | 申明让SpringBoot自动给程序进行必要的配置，这个配置等同于：@Configuration ，@EnableAutoConfiguration 和 @ComponentScan 三个配置。 |
| @Import                  | 用来导入其他配置类                                           |
| @ImportResource          | 用来加载xml配置文件                                          |
| @Bean                    | 放在==方法==的上面，而不是类。意思是产生一个bean，并交给Spring管理 |
| @Inject                  | 等价于默认的@Autowired，只是没有required属性                 |





# 参考资料

[SpringBoot注入依赖及注解](https://www.jianshu.com/p/686ecfc1a4b8)