---
typora-copy-images-to: SpringBootNotesPictures
---

# @RestController 和 @Controller 区别

`@RestController = @Controller + @ResponseBody`

- 使用 @Controller 注解的 Controller 类中的函数可以==返回具体的页面==。
    - 比如直接返回的 String 类型的 JSP、HTML页面名字，或者通过 `ModelAndView.setViewName()` 来指定页面名字。
    - 但==如果需要返回 Json 等类型的数据，则需要在函数上面再添加一个注解 @ResponseBody==。


- 通过 @RestController 注解的类，其中的函数不可以返回页面路径，==只可以返回具体的结果值==。比如查询完的对象、对象列表，最终呈现出来就是常用的 ==Json 等类型的值==。
    - 通过 @RestController 注解的类，返回得到值后，未加处理，总是得到 Json 类型的值。
    - 如果使用 @RestController 注解的类，再想返回页面路径，得到的值则为 null。

## 示例

```java
@Controller
public class HelloController {

    @RequestMapping(value="/hello", method= RequestMethod.GET)
    public String sayHello() {
        return "hello";
    }
}
```

如果直接使用 @Controller 这个注解，当运行该 SpringBoot 项目后，在浏览器中会得到错误提示。

出现这种情况的原因在于：没有使用模版。即==用 @Controller 来响应页面，必须配合模版来使用==。

<font color=red>返回 Json 需要 @ResponseBody 和 @Controller 配合</font>：

```java
@Controller
@ResponseBody
public class HelloController {

    @RequestMapping(value="/hello", method= RequestMethod.GET)
    public String sayHello() {
        return "hello";
    }
}
```

@RestController 是 Spring4 之后新加入的注解：

```java
@RestController
public class HelloController {

    @RequestMapping(value="/hello", method= RequestMethod.GET)
    public String sayHello() {
        return "hello";
    }
}
```

即 @RestController 是 @ResponseBody 和 @Controller 的组合注解。

## @RestController 源码

```java
package org.springframework.web.bind.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.stereotype.Controller;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented

@Controller
@ResponseBody
public @interface RestController {
    String value() default "";
}
```

可以看到，它被加上了 @Controller 和 @ResponseBody 注解。

```java
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface Controller {
    String value() default "";
}

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ResponseBody {
}
```

# @RequestMapping 配置 url 映射

@RequestMapping 此注解既可以作用在控制器的==某个方法==上，也可以作用在此控制器==类==上。

- 当控制器在类级别上添加 @RequestMapping 注解时，这个注解会==应用到控制器的所有处理器方法==上。
- 处理器方法上的 @RequestMapping 注解会对类级别上的 @RequestMapping 的声明进行补充。

## @RequestMapping 仅作用在处理器方法上

```java
@RestController
public class HelloController {

    @RequestMapping(value="/hello", method= RequestMethod.GET)
    public String sayHello() {
        return "hello";
    }
}
```

sayHello 所响应的 url 为 `localhost:8080/hello`。

## @RequestMapping 仅作用在类级别上
```java
@RestController
@RequestMapping("/hello")
public class HelloController {

    @RequestMapping(method= RequestMethod.GET)
    public String sayHello() {
        return "hello";
    }
}
```

sayHello 所响应的 url 为 `localhost:8080/hello`。

## @RequestMapping 作用在类级别和处理器方法上

```java
@RestController
@RequestMapping("/hello")
public class HelloController {

    @RequestMapping(value="/sayHello", method= RequestMethod.GET)
    public String sayHello() {
        return "hello";
    }
    @RequestMapping(value="/sayHi", method= RequestMethod.GET)
    public String sayHi() {
        return "hi";
    }
}
```

sayHello 所响应的 url 为 `localhost:8080/hello/sayHello`；
sayHi 所响应的 url 为 `localhost:8080/hello/sayHi`。

# 接收参数注解

## 请求路径参数

### @PathVaribale 获取 url 中的数据

如果需要获取 url 为 `localhost:8080/hello/id` 中的 id 值（==路径中的参数==，即`url/{id}`这种形式），实现代码如下：
```java
@RestController
public class HelloController {

    @RequestMapping(value="/hello/{id}", method= RequestMethod.GET)
    public String sayHello(@PathVariable("id") Integer id) {
        return "id:" + id;
    }
}
```

如果需要需要获取 url 中==多个参数==：
```java
@RestController
public class HelloController {

    @RequestMapping(value="/hello/{id}/{name}", method= RequestMethod.GET)
    public String sayHello(@PathVariable("id") Integer id, @PathVariable("name") String name) {
        return "id: " + id + " name: " + name;
    }
}
```

### @RequestParam 获取请求参数的值

获取 `localhost:8080/hello?id=98` 中 id 值（即`url?name=`这种形式）：

```java
@RestController
public class HelloController {

    @RequestMapping(value="/hello", method= RequestMethod.GET)
    public String sayHello(@RequestParam("id") Integer id) {
        return "id: " + id;
    }
}
```

如果 url 中没有 id 值：
```java
@RestController
public class HelloController {
    @RequestMapping(value="/hello", method= RequestMethod.GET)
    // required=false 表示 url 中可以不穿入 id 参数，此时就使用默认参数
    public String sayHello(@RequestParam(value="id", required = false, defaultValue = "1") Integer id) {
        return "id: " + id;
    }
}
```

## Body参数

### @RequestBody

Postman设置：

![image-20211223230859544](SpringBootNotesPictures/@RequestBody.png)

对应的Java代码：

```java
@PostMapping(path = "/demo")
public void demo(@RequestBody Person person) {
    System.out.println(person.toString());
}
```

### 无注解

Postman设置：

<img src="SpringBootNotesPictures/无注解.png" alt="image-20211223232437646" style="zoom:45%;" />

对应的Java代码：

```java
@PostMapping("/demo")
public void demo(Person person) {
    System.out.println(person.toString());
}
```

### @RequestBody接收多个对象

https://blog.csdn.net/hunt_er/article/details/109678025

## @GetMapping 组合注解

@GetMapping 是 `@RequestMapping(method = RequestMethod.GET)` 的缩写。该注解将 HTTP Get 映射到特定的处理方法上。

即可以使用 `@GetMapping(value = “/hello”)` 来代替 `@RequestMapping(value=”/hello”, method= RequestMethod.GET)`：
```java
@RestController
public class HelloController {
    // @RequestMapping(value="/hello", method= RequestMethod.GET)
    @GetMapping(value = "/hello")
    // required=false 表示 url 中可以不穿入 id 参数，此时就使用默认参数
    public String sayHello(@RequestParam(value="id", required = false, defaultValue = "1") Integer id) {
        return "id: " + id;
    }
}
```

## @PostMapping

`@PostMapping`注释将 HTTP POST 请求映射到特定的处理程序方法。 它是一个组合的注释，用作`@RequestMapping(method = RequestMethod.POST)`的快捷方式。



# 参数绑定注解

https://blog.csdn.net/aliyacl/article/details/85089035
https://blog.csdn.net/walkerjong/article/details/7946109/


# 参考资料

https://mp.weixin.qq.com/s?__biz=MzU4Njc5MjE4Mw==&mid=2247484427&idx=1&sn=9514b1767beaa1bb5f15cbaa5c176c9c&chksm=fdf4a993ca832085e565d4d4493791556f5c3cedfc1ff2dda23172a64dbbfaae15eb8b86ef71&token=2141916767&lang=zh_CN#rd

https://blog.csdn.net/u010412719/article/details/69710480?utm_medium=distribute.pc_relevant.none-task-blog-2%7Edefault%7ECTRLIST%7Edefault-1.no_search_link&depth_1-utm_source=distribute.pc_relevant.none-task-blog-2%7Edefault%7ECTRLIST%7Edefault-1.no_search_link

https://blog.csdn.net/u010412719/article/details/69788227