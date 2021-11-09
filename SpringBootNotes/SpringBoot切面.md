---
typora-copy-images-to: SpringBootNotesPictures
---

# 基本介绍

## 什么是 AOP<sup><a href="#ref1">1</a></sup>

**AOP** 为 **Aspect Oriented Programming** 的缩写，意为：面向切面编程，通过==预编译==方式和==运行期动态代理==实现程序功能的统一维护的一种技术。

利用 **AOP** 可以==对业务逻辑的各个部分进行隔离==，从而使得业务逻辑各部分之间的耦合度降低，提高程序的可重用性，同时提高了开发的效率。

**一个 AOP 的使用场景：**

> 假设一个已经上线的系统运行出现问题，有时运行得很慢。为了检测出是哪个环节出现了问题，就需要监控每一个方法的执行时间，再根据执行时间进行分析判断。
>
> 由于整个系统里的方法数量十分庞大，如果一个个方法去修改工作量将会十分巨大，而且这些监控方法在分析完毕后还需要移除掉，所以这种方式并不合适。
>
> 如果能够在系统运行过程中动态添加代码，就能很好地解决这个需求。
>
> 这种==在系统运行时动态添加代码==的方式称为面向切面编程（**AOP**）

## AOP 相关概念介绍

- **Joinpoint**（连接点）：==类里面可以被增强的方法==即为连接点。例如，想要修改哪个方法的功能，那么该方法就是一个链接点。

- **Target**（目标对象）：==要增强的类==成为 **Target**。

- **Pointcut**（切入点）：==对 **Jointpoint** 进行拦截的定义==即为切入点。例如，拦截所有以 **insert** 开始的方法，这个定义即为切入点。

- **Advice**（通知）：==拦截到 **Jointpoint** 之后要做的事情==就是通知。通知分为前置通知、后置通知、异常通知、最终通知和环绕通知。例如，前面说到的打印日志监控就是通知。

- **Aspect**（切面）：即 **Pointcut** 和 **Advice** 的结合。



## 切面类注解

- **@Aspect** 注解：表明这是一个切面类。

- **@Pointcut** 注解：表明这是一个切入点（`@Pointcut("execution(* com.example.demo.service.*.*(..))")`）。**execution** 中

  - 第一个 ***** 表示==方法返回任意值==；

  - 第二个 ***** 表示 ==**service** 包下的任意类==；

  - 第三个 ***** 表示==类中的任意方法==，括号中的==两个点表示方法参数任意==，即这里描述的切入点为 **service** 包下所有类中的所有方法。

- **@Before** 注解：表示这是一个前置通知，该方法在目标方法之前执行。
  - 通过 **JoinPoint** 参数可以获取目标方法的方法名、修饰符等信息。

- **@After** 注解：表示这是一个后置通知，该方法在目标执行之后执行。

- **@AfterReturning** 注解：表示这是一个返回通知，在该方法中可以获取目标方法的返回值。

  - **returning** 参数是指返回值的变量名，对应方法的参数。

  - **注意**：本样例在方法参数中定义 **result** 的类型为 **Object**，表示目标方法的返回值可以是任意类型。若 **result** 参数的类型为 **Long**，则该方法只能处理目标方法返回值为 **Long** 的情况。

- **@AfterThrowing** 注解：表示这是一个异常通知，即当目标方法发生异常，该方法会被调用。

  - 样例中设置的异常类型为 **Exception** 表示所有的异常都会进入该方法中执行。

  - 若异常类型为 **ArithmeticException** 则表示只有目标方法抛出的 **ArithmeticException** 异常才会进入该方法的处理。

- **@Around** 注解：表示这是一个环绕通知。环绕通知是所有通知里功能最为强大的通知，可以实现前置通知、后置通知、异常通知以及返回通知的功能。
  - 目标方法进入环绕通知后，通过调用 **ProceedingJointPoint** 对象的 **proceed** 方法使目标方法继续执行，开发者可以在次修改目标方法的执行参数、返回值值，并且可以在此目标方法的异常。

## 依赖

**Spring Boot** 在 **Spring** 的基础上对 **AOP** 的配置提供了自动化配置解决方案，只需要修改 **pom.xml** 文件，添加 **spring-boot-starter-aop** 依赖即可：

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-aop</artifactId>
</dependency>
```



# 实例一

参考链接：

[https://blog.csdn.net/zhuzhezhuzhe1/article/details/80565067](https://blog.csdn.net/zhuzhezhuzhe1/article/details/80565067)

[https://cloud.tencent.com/developer/article/1457465](https://cloud.tencent.com/developer/article/1457465)

面向切面编程可以让开发更加低耦合，大大减少代码量，同时让程序员更专注于业务模块的开发，把那些与业务无关的东西提取出去，便于后期的维护和迭代。

## 搭建环境

![切面-搭建环境](SpringBootNotesPictures/切面-搭建环境.png)

## 引入依赖

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>

<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-aop</artifactId>
</dependency>
```

![切面-引入依赖](SpringBootNotesPictures/切面-引入依赖.png)

## 日志实体类、service、controller和日志注解

### 实体类

```java
package com.example.entity;

import lombok.Data;
controller
/**
 * @author chenzufeng
 * @date 2021/11/2
 * @usage SysLogBO
 */
@Data
public class SysLogBO {
    
    private String className;

    private String methodName;

    private String params;

    private Long executeTime;

    private String remark;

    private String createDate;
}
```

### service

```java
package com.example.service;

import com.example.entity.SysLogBO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * @author chenzufeng
 * @date 2021/11/2
 * @usage SysLogService
 * @Slf4j 相当于 private final Logger logger = LoggerFactory.getLogger(X.class);
 */
@Slf4j
@Service
public class SysLogService {
    public boolean save(SysLogBO sysLogBO) {
        log.info(sysLogBO.getParams());
        return true;
    }
}
```

### controller

```java
package com.example.controller;

import com.example.annotation.SysLog;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author chenzufeng
 * @date 2021/11/2
 * @usage SysLogAopController
 */
@RestController
@RequestMapping("/aop")
public class SysLogAopController {
    @SysLog("测试AOP")
    @GetMapping("/test")
    public String test(
            @RequestParam("name") String name, 
            @RequestParam("age") Integer age) {
        return name + " " + age;
    }
}
```



### 注解

```java
package com.example.annotation;

import java.lang.annotation.*;

/**
 * @author chenzufeng
 * @date 2021/11/2
 * @usage SysLog 定义系统日志注解
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface SysLog {
    String value() default "";
}
```

## 声明切面

```java
package com.example.aspect;

import com.example.annotation.SysLog;
import com.example.entity.SysLogBO;
import com.example.service.SysLogService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

/**
 * @author chenzufeng
 * @date 2021/11/2
 * @usage SysLogAspect 系统日志切面
 * 使用@Aspect注解声明一个切面
 */
@Aspect
@Component
public class SysLogAspect {

    @Autowired
    private SysLogService sysLogService;

    @Pointcut("@annotation(com.example.annotation.SysLog)")
    public void logPointCut() {}

    @Around("logPointCut()")
    public Object around(ProceedingJoinPoint proceedingJoinPoint) throws Throwable {
        long beginTime = System.currentTimeMillis();
        Object result = proceedingJoinPoint.proceed();
        long time = System.currentTimeMillis() - beginTime;
        saveLog(proceedingJoinPoint, time);
        return  result;
    }

    private void saveLog(ProceedingJoinPoint proceedingJoinPoint, long time) {
        MethodSignature signature = (MethodSignature) proceedingJoinPoint.getSignature();
        Method method = signature.getMethod();

        SysLogBO sysLogBO = new SysLogBO();

        sysLogBO.setExecuteTime(time);

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
        sysLogBO.setCreateDate(simpleDateFormat.format(new Date()));

        SysLog sysLog = method.getAnnotation(SysLog.class);
        if (sysLog != null) {
            sysLogBO.setRemark(sysLog.value());
        }

        String className = proceedingJoinPoint.getTarget().getClass().getName();
        String methodName = signature.getName();
        sysLogBO.setClassName(className);
        sysLogBO.setMethodName(methodName);

        Object[] args = proceedingJoinPoint.getArgs();
        ArrayList<String> list = new ArrayList<>();
        for (Object arg : args) {
            list.add(arg.toString());
        }
        sysLogBO.setParams(list.toString());

        sysLogService.save(sysLogBO);
    }
}
```

# 实例二<sup><a href="#ref1">1</a></sup>

## 搭建环境

![切面-搭建环境2](SpringBootNotesPictures/切面-搭建环境2.png)

## 引入依赖

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>

<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-aop</artifactId>
</dependency>

<dependency>
    <groupId>ch.qos.logback</groupId>
    <artifactId>logback-classic</artifactId>
    <version>1.2.3</version>
</dependency>
```

## 创建controller

```java
package com.example.controller;

import com.example.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author chenzufeng
 * @date 2021/11/7
 * @usage HelloController
 */
@RestController
@RequestMapping("/HelloController")
public class HelloController {
    @Autowired
    private UserService userService;
    
    @GetMapping("/getUserById")
    public String getUserById(Integer id) {
        return userService.getUserById(id);
    }
}
```



## 创建service

```java
package com.example.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * @author chenzufeng
 * @date 2021/11/7
 * @usage UserService
 */
@Service
public class UserService {
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    public String getUserById(Integer id) {
        logger.info("接口调用方法getUserById：{}", id);
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return "chenzufeng";
    }
}
```

## 创建切面

### 切面类

```java
package com.example.aspect;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * @author chenzufeng
 * @date 2021/11/7
 * @usage LogAspect
 */
@Aspect
@Component
public class LogAspect {
    private static final Logger logger = LoggerFactory.getLogger(LogAspect.class);

    /**
     * 方法返回任意值，service包下任意类、类中任意方法、任意参数
     */
    @Pointcut("execution(* com.example.service.*.*(..))")
    public void pointCut() {}

    /**
     * 前置通知
     * @param joinPoint joinPoint
     */
    @Before(value = "pointCut()")
    public void before(JoinPoint joinPoint) {
        String name = joinPoint.getSignature().getName();
        logger.info("{} 方法开始执行。。。", name);
    }

    /**
     * 后置通知
     * @param joinPoint joinPoint
     */
    @After(value = "pointCut()")
    public void after(JoinPoint joinPoint) {
        String name = joinPoint.getSignature().getName();
        logger.info("{} 方法执行结束！", name);
    }

    /**
     * 返回通知
     * @param joinPoint joinPoint
     * @param result 方法返回值
     */
    @AfterReturning(value = "pointCut()", returning = "result")
    public void afterReturning(JoinPoint joinPoint, Object result) {
        String name = joinPoint.getSignature().getName();
        logger.info("{} 方法返回值为 {}", name, result);
    }

    /**
     * 异常通知
     * @param joinPoint joinPoint
     * @param exception 异常
     */
    @AfterThrowing(value = "pointCut()", throwing = "exception")
    public void afterThrowing(JoinPoint joinPoint, Exception exception) {
        String name = joinPoint.getSignature().getName();
        logger.info("{} 方法抛出 {} 异常！", name, exception);
    }

    /**
     * 环绕通知
     * @param proceedingJoinPoint proceedingJoinPoint
     */
    @Around(value = "pointCut()")
    public Object afterThrowing(ProceedingJoinPoint proceedingJoinPoint) throws Throwable {
        String name = proceedingJoinPoint.getSignature().getName();
        // 统计方法执行时间
        Long startTime = System.currentTimeMillis();
        Object result = proceedingJoinPoint.proceed();
        Long endTime = System.currentTimeMillis();
        logger.info("{} 方法执行时间为 {} ms！", name, endTime - startTime);
        return result;
    }
}
```

注意1：第80行（`return result`），相当于，<font color=red>切面对返回结果进行处理后，将原结果放行</font>！

注意2：`@Pointcut("execution(* com.example.service.*.*(..))")`是对`service`中方法调用进行处理：

```java
@RestController
@RequestMapping("/HelloController")
public class HelloController {
    @Autowired
    private UserService userService;

    @GetMapping("/getUserById")
    public Integer getUserById(Integer id) {
        return id;
    }
}
```

上述方法没有调用`service`中的方法，此时切面对其不起作用！



### 切面类注解详解

- **@Aspect** 注解：表明这是一个切面类。

- **@Pointcut** 注解：表明这是一个切入点（`@Pointcut("execution(* com.example.demo.service.*.*(..))")`）。**execution** 中

  - 第一个 ***** 表示==方法返回任意值==；

  - 第二个 ***** 表示 ==**service** 包下的任意类==；

  - 第三个 ***** 表示==类中的任意方法==，括号中的==两个点表示方法参数任意==，即这里描述的切入点为 **service** 包下所有类中的所有方法。

- **@Before** 注解：表示这是一个前置通知，该方法在目标方法之前执行。
  - 通过 **JoinPoint** 参数可以获取目标方法的方法名、修饰符等信息。

- **@After** 注解：表示这是一个后置通知，该方法在目标执行之后执行。

- **@AfterReturning** 注解：表示这是一个返回通知，在该方法中可以获取目标方法的返回值。

  - **returning** 参数是指返回值的变量名，对应方法的参数。

  - **注意**：本样例在方法参数中定义 **result** 的类型为 **Object**，表示目标方法的返回值可以是任意类型。若 **result** 参数的类型为 **Long**，则该方法只能处理目标方法返回值为 **Long** 的情况。

- **@AfterThrowing** 注解：表示这是一个异常通知，即当目标方法发生异常，该方法会被调用。

  - 样例中设置的异常类型为 **Exception** 表示所有的异常都会进入该方法中执行。

  - 若异常类型为 **ArithmeticException** 则表示只有目标方法抛出的 **ArithmeticException** 异常才会进入该方法的处理。

- **@Around** 注解：表示这是一个环绕通知。环绕通知是所有通知里功能最为强大的通知，可以实现前置通知、后置通知、异常通知以及返回通知的功能。
  - 目标方法进入环绕通知后，通过调用 **ProceedingJointPoint** 对象的 **proceed** 方法使目标方法继续执行，开发者可以在次修改目标方法的执行参数、返回值值，并且可以在此目标方法的异常。

## 测试

在浏览器中输入：[http://localhost:8080/HelloController/getUserById?id=11](http://localhost:8080/HelloController/getUserById?id=11)

控制台返回：

```markdown
getUserById 方法开始执行。。。
接口调用方法getUserById：11
getUserById 方法返回值为 chenzufeng
getUserById 方法执行结束！
getUserById 方法执行时间为 2028 ms！
```

## 过滤方法

### 改造controller、service

```java
package com.example.controller;

@RestController
@RequestMapping("/HelloController")
public class HelloController {
    @Autowired
    private UserService userService;

    @GetMapping("/getUserById")
    public String getUserById(Integer id) {
        return userService.getUserById(id);
    }
    
    @GetMapping("/getInfo")
    public String getInfo(@RequestParam String message) {
        return userService.getInfo(message);
    }
}

package com.example.service;

@Service
public class UserService {
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    public String getUserById(Integer id) {
        logger.info("接口调用方法getUserById：{}", id);
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return "chenzufeng";
    }

    public String getInfo(String message) {
        logger.info("接口调用getInfo方法：{}", message);
        return message;
    }
}
```

### 设置过滤策略

如果方法名中不含有`User`，则不执行环绕通知：

```java
package com.example.aspect;

@Aspect
@Component
public class LogAspect {
    private static final Logger logger = LoggerFactory.getLogger(LogAspect.class);

    /**
     * 设置方法白名单
     * 方法中含有User才会进行处理
     */
    private static List<String> allowMethods = new ArrayList<>();
    static {
        allowMethods.add("User");
    }

    /**
     * 方法返回任意值，service包下任意类、类中任意方法、任意参数
     */
    @Pointcut("execution(* com.example.service.*.*(..))")
    public void pointCut() {}

    /**
     * 环绕通知
     * @param proceedingJoinPoint proceedingJoinPoint
     */
    @Around(value = "pointCut()")
    public Object afterThrowing(ProceedingJoinPoint proceedingJoinPoint) throws Throwable {
        String methodName = proceedingJoinPoint.getSignature().getName();
        /*
        * 检测是否不存在满足指定行为的元素，如果不存在则返回true（如果此字符串中没有这样的字符，则返回 -1）
        * methodName.indexOf(method) != -1 是否满足？不满足（methodName中没有allowMethods中字符），返回true
        * */
        if (allowMethods.stream().noneMatch(method -> methodName.indexOf(method) != -1)) {
            return proceedingJoinPoint.proceed();
        }
        logger.info("========================开始执行环绕通知========================");

        // 统计方法执行时间
        Long startTime = System.currentTimeMillis();
        Object result = proceedingJoinPoint.proceed();
        Long endTime = System.currentTimeMillis();
        logger.info("{} 方法执行时间为 {} ms！", methodName, endTime - startTime);
        return result;
    }
}
```

## JoinPoint和ProceedingJoinPoint

### JoinPoint方法

```java
package com.example.aspect;

@Aspect
@Component
public class LogAspect {
    private static final Logger logger = LoggerFactory.getLogger(LogAspect.class);

    /**
     * 设置方法白名单
     * 方法中含有User才会进行处理
     */
    private static List<String> allowMethods = new ArrayList<>();
    static {
        allowMethods.add("User");
    }

    /**
     * 方法返回任意值，service包下任意类、类中任意方法、任意参数
     */
    @Pointcut("execution(* com.example.service.*.*(..))")
    public void pointCut() {}

    /**
     * 返回通知
     * @param joinPoint joinPoint
     * @param result 方法返回值
     */
    @AfterReturning(value = "pointCut()", returning = "result")
    public void afterReturning(JoinPoint joinPoint, Object result) {
        logger.info("========================开始执行返回通知========================");
        // 返回目标对象，即被代理对象
        Object target = joinPoint.getTarget();
        logger.info("joinPoint.getTarget()返回目标对象，即被代理对象：{}", target);
        // target.getClass().getMethods()
        String className = target.getClass().getName();
        logger.info("target.getClass().getName()返回被代理对象类名：{}", className);

        // 返回切入点参数
        Object[] args = joinPoint.getArgs();
        logger.info("joinPoint.getArgs()返回切入点参数：{}", args);
        // 返回切入点方法的名字
        String name = joinPoint.getSignature().getName();
        logger.info("joinPoint.getSignature().getName()返回切入点方法的名字：{}", name);

        logger.info("{} 方法返回值为 {}", name, result);
    }

    /**
     * 环绕通知
     * @param proceedingJoinPoint proceedingJoinPoint
     */
    @Around(value = "pointCut()")
    public Object afterThrowing(ProceedingJoinPoint proceedingJoinPoint) throws Throwable {
        String methodName = proceedingJoinPoint.getSignature().getName();
        /*
        * 检测是否不存在满足指定行为的元素，如果不存在则返回true（如果此字符串中没有这样的字符，则返回 -1）
        * methodName.indexOf(method) != -1 是否满足？不满足（methodName中没有allowMethods中字符），返回true
        * */
        if (allowMethods.stream().noneMatch(method -> methodName.indexOf(method) != -1)) {
            return proceedingJoinPoint.proceed();
        }
        logger.info("========================开始执行环绕通知========================");

        // 统计方法执行时间
        Long startTime = System.currentTimeMillis();
        Object result = proceedingJoinPoint.proceed();
        Long endTime = System.currentTimeMillis();
        logger.info("{} 方法执行时间为 {} ms！", methodName, endTime - startTime);
        return result;
    }
}
```

输出：

```markdown
========================开始执行返回通知========================
joinPoint.getTarget()返回目标对象，即被代理对象：com.example.service.UserService@2e2aee5d
target.getClass().getName()返回被代理对象类名：com.example.service.UserService
joinPoint.getArgs()返回切入点参数：11
joinPoint.getSignature().getName()返回切入点方法的名字：getInfo
getInfo 方法返回值为 11
```



# 参考资料

<span name="ref1">[1] [SpringBoot - 面向切面编程 AOP 的配置和使用（附样例）](https://www.hangge.com/blog/cache/detail_2527.html)</span>

