---
typora-copy-images-to: SpringBootNotesPictures
---

# 什么是JWT

# 搭建环境

## 创建项目

![JWT-创建项目](SpringBootNotesPictures/JWT-创建项目.png)

依赖：

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>
```



## 多环境配置

`application.properties`

```properties
spring.profiles.active=dev
```

`application-dev.properties`

```properties
server.port=8081
```



## 数据库

### 连接池

Druid连接池的意义<sup><a href="#ref2">[2]</a></sup>：

==建立数据库连接==耗时耗费资源。一个数据库服务器能够同时建立的连接数也是有限的，在大型的Web应用中，可能<font color=red>同时会有成百上千的访问数据库的请求，如果Web应用程序为每一个客户请求分配一个数据库连接，将导致性能的急剧下降</font>。

数据库连接池的意义在于，能够<font color=red>重复利用数据库连接</font>，提高对请求的响应时间和服务器的性能。
连接池中提前预先建立了多个数据库连接对象，然后将连接对象保存到连接池中，当客户请求到来时，直接从池中取出一个连接对象为客户服务，当请求完成之后，客户程序调用close()方法，将连接对象放回池中。

### 添加依赖

```xml
<!--引入mybatis-->
<dependency>
    <groupId>org.mybatis.spring.boot</groupId>
    <artifactId>mybatis-spring-boot-starter</artifactId>
    <version>2.2.0</version>
</dependency>
<!--引入mysql-->
<dependency>
    <groupId>mysql</groupId>
    <artifactId>mysql-connector-java</artifactId>
    <scope>runtime</scope>
</dependency>
<!--引入druid-->
<dependency>
    <groupId>com.alibaba</groupId>
    <artifactId>druid</artifactId>
    <version>1.2.8</version>
</dependency>
```

### 配置文件

```properties
server.port=8081
spring.application.name=jwt

spring.datasource.type=com.alibaba.druid.pool.DruidDataSource
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
spring.datasource.url=jdbc:mysql://localhost:3306/jwt?characterEncoding=UTF-8
spring.datasource.username=root
spring.datasource.password=120618

# mybatis.type-aliases-package=com.example.entity
mybatis.mapper-locations=classpath:com/example/mapper/*.xml

logging.level.com.baizhi.dao=debug
```

### 创建表

```mysql
DROP TABLE IF EXISTS `t_user`;
CREATE TABLE `t_user` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_name` varchar(80) DEFAULT NULL COMMENT '用户名',
  `password` varchar(40) DEFAULT NULL COMMENT '用户密码',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
```



## Swagger

### 添加依赖

```xml
<!-- https://mvnrepository.com/artifact/io.springfox/springfox-swagger2 -->
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
```



### 添加配置文件

```java
package com.example.swagger;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.ParameterBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.schema.ModelRef;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Parameter;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

import java.util.ArrayList;
import java.util.List;

/**
 * @author chenzufeng
 * @date 2021/11/17
 */
@Configuration
@EnableSwagger2
public class SwaggerConfig {
    @Bean
    public Docket createRestApi(Environment environment) {

        // 设置要显示的Swagger环境：是否返回dev，spring.profiles.active=dev（org.springframework.core.env）
        Profiles profiles = Profiles.of("dev");
        // 获取项目的环境
        boolean environmentFlag = environment.acceptsProfiles(profiles);

        return new Docket(DocumentationType.SWAGGER_2)
                .apiInfo(apiInfo())
                // 只能在.select()前添加；配置是否启用Swagger，如果是false，在浏览器将无法访问
                .enable(environmentFlag)
                .groupName("JWT")

                // 通过 select()方法，去配置扫描接口
                .select()
                // RequestHandlerSelectors 配置如何扫描接口
                .apis(RequestHandlerSelectors.basePackage("com.example.controller"))
                // 配置如何通过path过滤，PathSelectors.ant("/example/**")：只扫描请求以/example开头的接口
                .paths(PathSelectors.any())
                .build()
                .globalOperationParameters(getParameterList());
    }

    private ApiInfo apiInfo() {
        return new ApiInfoBuilder()
                .title("JWT接口文档")
                .description("JWT接口文档")
                .version("1.0.0.RELEASE")
                // 使用了创建者设计模式
                .build();
    }

    /**
     * 设置请求头
     * @return List<Parameter>
     */
    private List<Parameter> getParameterList() {
        ParameterBuilder headers = new ParameterBuilder();
        List<Parameter> parameters = new ArrayList<>();

        Parameter cookieParam = headers.name("Cookie")
                .description("Cookie").modelRef(new ModelRef("String"))
                .parameterType("header").required(false)
                .build();

        Parameter authorization = headers.name("Authorization")
                .description("IAM Token").modelRef(new ModelRef("String"))
                .parameterType("header").required(false)
                .build();

        Parameter contentTypeParam = headers.name("Content-Type")
                .description("Content-Type").modelRef(new ModelRef("String"))
                .parameterType("header").required(false)
                .defaultValue("application/json;charset=UTF-8")
                .build();

        parameters.add(cookieParam);
        parameters.add(authorization);
        parameters.add(contentTypeParam);
        return parameters;
    }
}
```

### 地址

[http://localhost:8081/doc.html](http://localhost:8081/doc.html)



## 日志

### 添加依赖

```xml
<dependency>
    <groupId>ch.qos.logback</groupId>
    <artifactId>logback-classic</artifactId>
    <version>1.2.3</version>
</dependency>
```



### 添加配置文件

`src/main/resources/logback.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration debug="false">

    <!--定义日志文件的存储地址：当前项目JWT下log文件夹-->
    <property name="LOG_HOME" value="log"/>

    <!--控制台日志， 控制台输出 -->
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder class="ch.qos.logback.classic.encoder.PatternLayoutEncoder">
            <!--格式化输出：%d表示日期，%thread表示线程名，%-5level：级别从左显示5个字符宽度，%msg：日志消息，%n是换行符-->
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{50} - %msg%n</pattern>
        </encoder>
    </appender>

    <!--文件日志， 按照每天生成日志文件 -->
    <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <!--日志文件输出的文件名-->
            <FileNamePattern>${LOG_HOME}/JWT.%d{yyyy-MM-dd}.log</FileNamePattern>
            <!--日志文件保留天数-->
            <MaxHistory>30</MaxHistory>
        </rollingPolicy>

        <encoder class="ch.qos.logback.classic.encoder.PatternLayoutEncoder">
            <!--格式化输出：%d表示日期，%thread表示线程名，%-5level：级别从左显示5个字符宽度，%msg：日志消息，%n是换行符-->
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{50} - %msg%n</pattern>
        </encoder>

        <!--日志文件最大的大小-->
        <triggeringPolicy class="ch.qos.logback.core.rolling.SizeBasedTriggeringPolicy">
            <MaxFileSize>10MB</MaxFileSize>
        </triggeringPolicy>
    </appender>

    <!-- 日志输出级别 -->
    <root level="INFO">
        <appender-ref ref="CONSOLE"/>
        <appender-ref ref="FILE"/>
    </root>
</configuration>
```



## 统一数据处理

### 常数

`com/example/constants/ResultConstants.java`

```java
package com.example.constants;

/**
 * @author chenzufeng
 * @date 2021/11/17
 */
public class ResultConstants {
    public enum Constants {
        ;
        /**
         * 操作成功编码
         */
        public static final String CODE_SUCCESS = "200";
        /**
         * 操作成功提示信息
         */
        public static final String MSG_SUCCESS = "操作成功";

        /**
         * 操作失败编码
         */
        public static final String CODE_FAIL = "400";
        /**
         * 操作失败提示信息
         */
        public static final String MSG_FAIL = "操作失败";

        /**
         * 没有权限编码
         */
        public static final String CODE_INVALID_FAIL = "401";
        /**
         * 没有权限提示信息
         */
        public static final String MSG_INVALID_FAIL = "校验未通过，没有权限";

        /**
         * 未登录编码
         */
        public static final String CODE_FAIL_NOT_LOGIN = "305";
        /**
         * 未登录提示信息
         */
        public static final String MSG_FAIL_NOT_LOGIN = "未登录";
    }
}
```

### Result

`com/example/utils/Result.java`

```java
package com.example.utils;

import com.example.constants.ResultConstants;

/**
 * @author chenzufeng
 * @date 2021/11/17
 */
public class Result {
    
    private String code;
    private Boolean success;
    private String message;
    private Object data;

    private Result(Boolean success, String code, String message, Object data) {
        this.success = success;
        this.code = code;
        this.message = message;
        this.data = data;
    }

    /**
     * 调用成功
     * @param code 编码
     * @param message 详细信息
     * @param data 数据
     * @return 统一结果格式
     */
    public static Result success(String code, String message, Object data) {
        return new Result(true, code, message, data);
    }

    public static Result success(String code, Object data) {
        return success(code, ResultConstants.Constants.MSG_SUCCESS, data);
    }

    public static Result success(Object data) {
        return success(ResultConstants.Constants.CODE_SUCCESS, data);
    }

    public static Result success(String code, String message) {
        return success(code, message, null);
    }

    public static Result success(String message) {
        return success(ResultConstants.Constants.CODE_SUCCESS, message, null);
    }

    public static Result success() {
        return success(ResultConstants.Constants.MSG_SUCCESS);
    }

    /**
     * 调用失败
     * @param code 编码
     * @param message 详细信息
     * @param data 数据
     * @return 统一结果格式
     */
    public static Result fail(String code, String message, Object data) {
        return new Result(false, code, message, data);
    }

    public static Result fail(String code, String message) {
        return fail(code, message, null);
    }

    public static Result fail(String code, Object data) {
        return fail(code, ResultConstants.Constants.MSG_FAIL, data);
    }

    public static Result failCode(String code) {
        return fail(code, ResultConstants.Constants.MSG_FAIL);
    }

    public static Result failMessage(String message) {
        return fail(ResultConstants.Constants.CODE_FAIL, message);
    }

    public static Result fail() {
        return fail(ResultConstants.Constants.CODE_FAIL, ResultConstants.Constants.MSG_FAIL);
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public Boolean getSuccess() {
        return success;
    }

    public void setSuccess(Boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }
}
```



## 全局异常处理

### 自定义异常处理

`com/example/exception/CustomException.java`

```java
package com.example.exception;

import com.example.constants.ResultConstants;

/**
 * @author chenzufeng
 * @date 2021/11/17
 */
public class CustomException extends RuntimeException {

    private String code = ResultConstants.Constants.MSG_FAIL;

    private String message;

    public CustomException(String message) {
        super(message);
        this.message = message;
    }

    public CustomException(String code, String message) {
        super(message);
        this.code = code;
        this.message = message;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    @Override
    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
```



### 全局异常处理

`com/example/exception/GlobalExceptionHandler.java`

```java
package com.example.exception;

import com.example.utils.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * @author chenzufeng
 * @date 2021/11/17
 */
@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 处理 Exception 异常
     * @param exception Exception 异常
     * @return Result
     */
    @ExceptionHandler(Exception.class)
    public Result handlerException(Exception exception) {
        logger.warn("GlobalExceptionHandler handlerException：{}", exception);
        return Result.failMessage("系统异常！");
    }

    /**
     * 处理空指针异常
     * @param nullPointerException 空指针异常
     * @return Result
     */
    @ExceptionHandler(NullPointerException.class)
    public Result handlerNullPointerException(NullPointerException nullPointerException) {
        logger.warn("GlobalExceptionHandler handlerNullPointerException：{}", nullPointerException);
        return Result.failMessage("空指针异常！");
    }

    /**
     * 处理运行时异常
     * @param runtimeException 运行时异常
     * @return Result
     */
    @ExceptionHandler(RuntimeException.class)
    public Result handlerRuntimeException(RuntimeException runtimeException) {
        logger.warn("GlobalExceptionHandler handlerRuntimeException：{}", runtimeException);
        return Result.failMessage(runtimeException.getMessage());
    }

    /**
     * 处理自定义异常
     * @param customException 自定义异常
     * @return Result
     */
    @ExceptionHandler(CustomException.class)
    public Result handlerCustomException(CustomException customException) {
        logger.warn("GlobalExceptionHandler handlerCustomException：{}", customException);
        return Result.fail(customException.getCode(), customException.getMessage());
    }
}
```



# SpringBoot整合JWT

## 添加依赖

```xml
<!-- https://mvnrepository.com/artifact/com.auth0/java-jwt -->
<dependency>
    <groupId>com.auth0</groupId>
    <artifactId>java-jwt</artifactId>
    <version>3.10.3</version>
</dependency>
```



## 封装JWT工具

```java
package com.example.utils;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTCreator;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.example.entity.User;

import java.util.Calendar;

/**
 * @author chenzufeng
 * @date 2021/11/17
 */
public class JwtUtil {
    /**
     * signature
     */
    private static String SIGNATURE = "token!Q@W3e4r";

    /**
     * 获取token
     * @param user 用户
     * @return token
     */
    public static String getToken(User user) {
        JWTCreator.Builder builder = JWT.create();
        
        // payload
        builder.withClaim("id", user.getId());
        builder.withClaim("userName", user.getUserName());
        builder.withClaim("password", user.getPassword());

        // 设置token有效时间
        Calendar instance = Calendar.getInstance();
        instance.add(Calendar.MINUTE, 1);
        builder.withExpiresAt(instance.getTime());
        
        // signature
        return builder.sign(Algorithm.HMAC256(SIGNATURE)).toString();
    }

    /**
     * 验证token
     * @param token token
     * @return 是否有效
     */
    public static DecodedJWT verifyToken(String token) {
        return JWT.require(Algorithm.HMAC256(SIGNATURE)).build().verify(token);
    }
}
```



## 完善项目

### entity

```java
package com.example.entity;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * @author chenzufeng
 * @date 2021/11/17
 */
@ApiModel(value = "User")
public class User {
    @ApiModelProperty(value = "主键")
    private Integer id;
    
    @ApiModelProperty(value = "用户名")
    private String userName;
    
    @ApiModelProperty(value = "密码")
    private String password;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
```

### controller层

```java
package com.example.controller;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.example.constants.ResultConstants;
import com.example.entity.User;
import com.example.utils.JwtUtil;
import com.example.utils.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

/**
 * @author chenzufeng
 * @date 2021/11/17
 */
@Api(tags = "用户登录接口")
@RestController
@RequestMapping("/user")
public class UserController {

    public static final Logger logger = LoggerFactory.getLogger(UserController.class);

    @Autowired
    private UserService userService;

    @PostMapping("/login")
    @ApiOperation(value = "login")
    public Result login(@RequestBody User user) {
        User userDb = userService.login(user);
        if (userDb != null) {
            String token = JwtUtil.getToken(user);
            return Result.success(ResultConstants.Constants.CODE_SUCCESS, "登录成功！", token);
        }
        return Result.fail(ResultConstants.Constants.CODE_FAIL, "登录失败！");
    }

    @GetMapping("/test")
    @ApiOperation(value = "AuthorizationTest")
    public Result test(HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        DecodedJWT decodedJWT = JwtUtil.verifyToken(token);
        Integer id = decodedJWT.getClaim("id").asInt();
        String userName = decodedJWT.getClaim("userName").asString();
        String password = decodedJWT.getClaim("password").asString();
        logger.info("用户ID：{}， 用户名：{}，用户密码：{}", id, userName, password);
        return Result.success("请求成功！");
    }
}
```



### service层

```java
package com.example.service;

import com.example.entity.User;

/**
 * @author chenzufeng
 * @date 2021/11/17
 */
public interface UserService {
    User login(User user);
}
```



```java
package com.example.service.impl;

import com.example.entity.User;
import com.example.exception.CustomException;
import com.example.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author chenzufeng
 * @date 2021/11/17
 */
@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserDao userDao;

    @Override
    public User login(User user) {
        User userDb = userDao.login(user);
        // 如果使用 if(userDb.getId()) 会出现空指针
        if (userDb != null) {
            return userDb;
        }

        throw new CustomException("登录失败！");
    }
}
```



### dao层

```java
package com.example.dao;

import com.example.entity.User;
import org.apache.ibatis.annotations.Mapper;

/**
 * @author chenzufeng
 * @date 2021/11/17
 */
@Mapper
public interface UserDao {
    User login(User user);
}
```

`com/example/mapper/UserDaoMapper.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN" "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.example.dao.UserDao">

    <!--
      自己组装一个结果集并返回一个结果集
      1. 当【数据库中字段名称】与【实体类对象的属性名】不一致时，可以进行转换
      2. 当前查询的结果没有对应一个表时，可以自定义一个结果集
    -->
    <resultMap id="BaseResultMap" type="com.example.entity.User">
        <!-- id：标识主键字段；
              column：数据库中的字段名称（user_name）
              property：映射对象（实体对象）的属性名称（userName）
              jdbcType：数据库中字段类型（可省略）
         -->
        <id column="id" jdbcType="INTEGER" property="id" />
        <!-- 除了主键以外的数据库中字段名称 -->
        <result column="user_name" jdbcType="VARCHAR" property="userName" />
        <result column="password" jdbcType="VARCHAR" property="password" />
    </resultMap>

    <!-- sql语句片段，将公共的部分抽取出来，不用写select *影响效率 -->
    <sql id="query_base">
        SELECT
            id AS id,
            user_name AS userName,
            password AS password
        FROM
            t_user
    </sql>

    <sql id="search_fragment">
        <trim prefix="WHERE" prefixOverrides="AND | OR">
            <if test="userName != null and userName != ''">
                AND user_name = #{userName}
            </if>
            <if test="password != null and password != ''">
                AND password = #{password}
            </if>
        </trim>
    </sql>

    <select id="login" parameterType="com.example.entity.User" resultMap="BaseResultMap">
        <include refid="query_base"/>
        <include refid="search_fragment"/>
    </select>

</mapper>
```



## 拦截器

### 实现拦截器

```java
package com.example.interceptor;

import com.auth0.jwt.exceptions.InvalidClaimException;
import com.auth0.jwt.exceptions.SignatureVerificationException;
import com.auth0.jwt.exceptions.TokenExpiredException;
import com.example.exception.CustomException;
import com.example.utils.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author chenzufeng
 * @date 2021/11/17
 */
public class JwtInterceptor implements HandlerInterceptor {
    
    public static final Logger logger = LoggerFactory.getLogger(JwtInterceptor.class);
    
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 获取请求头中的token
        String token = request.getHeader("Authorization");
        logger.info("token为：{}", token);
        
        try {
            JwtUtil.verifyToken(token);
            return true;
        } catch (SignatureVerificationException e) {
            e.printStackTrace();
            throw new CustomException("签名不一致！");
        } catch (TokenExpiredException e) {
            e.printStackTrace();
            throw new CustomException("token过期！");
        } catch (InvalidClaimException e) {
            e.printStackTrace();
            throw new CustomException("失效的payload！");
        } catch (Exception e) {
            e.printStackTrace();
            throw new CustomException("token无效！");
        }
    }
}
```



### 注册拦截器

```java
package com.example.interceptor;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * @author chenzufeng
 * @date 2021/11/17
 */
@Configuration
public class InterceptorConfig implements WebMvcConfigurer {
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new JwtInterceptor())
                .addPathPatterns("/user/test")
                .excludePathPatterns("/user/login");
    }
}
```



# 参考资料

[2] <span name="ref2">[Druid连接池的意义以及使用](https://www.cnblogs.com/Jeely/p/12364571.html)</span>

