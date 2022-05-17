# 概述

出于安全考虑，SpringBoot 配置文件中的敏感信息通常需要对它进行加密、脱敏处理，尽量不使用明文。Jasypt 开源安全框架就是专门用于处理 SpringBoot 属性加密的，在配置文件中使用特定格式直接配置密文，然后应用启动的时候，Jasypt 会自动将密码解密成明文供程序使用。

Jasypt 加密属性配置格式：`secret.property=ENC(nrmZtkF7T0kjG/VodDvBw93Ct8EgjCA+)`，`ENC()` 就是它的标识，程序启动的时候，会自动解密其中的内容，如果解密失败，则会报错。

获取这些属性值和平时没有区别，直接使用如 `@Value("${secret.property}")` 获取即可，取值并不需要特殊处理。

jasypt  同一个密钥（secretKey）对同一个内容执行加密，每次生成的密文都是不一样的，但是根据根据这些密文解密成原内容都是一样的。

## 使用方式

### 方式一

如果是 SpringBoot 应用程序，使用了注解 `@SpringBootApplication` 或者 `@EnableAutoConfiguration`，那么只需添加 `jasypt-spring-boot-starter` 依赖，此时==整个 Spring 环境就会支持可加密属性配置==（这意味着任何系统属性、环境属性、命令行参数，yaml、properties 和任何其他自定义属性源可以包含加密属性）：

```xml
<dependency>
    <groupId>com.github.ulisesbocchio</groupId>
    <artifactId>jasypt-spring-boot-starter</artifactId>
    <version>3.0.4</version>
</dependency>
```

### 方式二

1、如果没有使用 `@SpringBootApplication` 或者 `@EnableAutoConfiguration`，则将` jasypt-spring-boot` 添加到项目：

```xml
<dependency>
    <groupId>com.github.ulisesbocchio</groupId>
    <artifactId>jasypt-spring-boot</artifactId>
    <version>3.0.4</version>
</dependency>
```

2、然后将 `@EnableEncryptableProperties` 添加到配置类中，以便在整个 Spring 环境中启用可加密属性：

```java
@Configuration
@EnableEncryptableProperties
public class MyApplication {
    ...
}
```

### 方式三

1、如果不使用 `@SpringBootApplication` 或者 `@EnableAutoConfiguration` 自动配置注解，并且不想在整个 Spring 环境中启用可加密的属性，则首先将以下依赖项添加到项目中：

```xml
<dependency>
    <groupId>com.github.ulisesbocchio</groupId>
    <artifactId>jasypt-spring-boot</artifactId>
    <version>3.0.4</version>
</dependency>
```

2、然后在配置文件中添加任意数量的 `@EncryptablePropertySource` 注解，就像使用 Spring 的 `@PropertySource` 注解一样：

```java
@Configuration
@EncryptablePropertySource(name = "EncryptedProperties", value = "classpath:encrypted.properties")
public class MyApplication {
	...
}
```

或者还可以使用 `@EncryptablePropertySources` 注解来对 `@EncryptablePropertySource` 类型的注解进行分组：

```java
@Configuration
@EncryptablePropertySources({@EncryptablePropertySource("classpath:encrypted.properties"),
                             @EncryptablePropertySource("classpath:encrypted2.properties")})
public class MyApplication {
    ...
}
```

# 使用实例

## 添加依赖

```xml
<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
        <version>2.3.7.RELEASE</version>
    </dependency>

    <dependency>
        <groupId>com.github.ulisesbocchio</groupId>
        <artifactId>jasypt-spring-boot-starter</artifactId>
        <version>3.0.3</version>
    </dependency>
</dependencies>
```



## 工具类加解密

第一步要==获取密文==，就是将需要加密的数据进行加密，方法有很多，官方提供了 ==jar 包==，可以从==命令行==操作，也可以直接使用代码编写一个==工具类==进行加密。

编写工具类进行加密时，需注意：

- Jasypt 默认使用 StringEncryptor 解密属性，所以加密时默认也得使用 StringEncryptor 加密，否则启动时解密失败报错；
- 加密与解密对 StringEncryptor 设置的属性必须要一致，例如加密时使用什么算法，那么解密时也得一样，否则启动时解密失败报错。
- 加密算法 `PBEWithMD5AndDES`是 md5 加 des 标准加密；官网默认的是 `PBEWITHHMACSHA512ANDAES_256`，是 sha512 加 AES 高级加密。
- 如果想使用 `PBEWITHHMACSHA512ANDAES_256` 算法，<u>==需要 Java JDK 1.9 及以上支持，或者添加 JCE 无限强度权限策略文件^**待确认**^==</u>，否则运行会报错：加密引发异常，一个可能的原因是您正在使用强加密算法，并且您没有在这个Java 虚拟机中安装 Java 加密扩展（JCE）无限强权限策略文件。

工具类：

```java
package com.example;

import org.jasypt.encryption.pbe.PooledPBEStringEncryptor;
import org.jasypt.encryption.pbe.StandardPBEByteEncryptor;
import org.jasypt.encryption.pbe.config.SimpleStringPBEConfig;
import org.springframework.util.StringUtils;

public class JasyptUtils {
    /**
     * 默认加解密算法
     */
    public static final String DEFAULT_ALGORITHM = "PBEWITHHMACSHA512ANDAES_256";

    /**
     * 默认加盐的类名
     */
    public static final String DEFAULT_SALT_GENERATOR_CLASSNAME = "org.jasypt.salt.RandomSaltGenerator";

    /**
     * 默认初始向量 IV 生成器的类名
     */
    public static final String DEFAULT_IV_GENERATOR_CLASSNAME = "org.jasypt.iv.RandomIvGenerator";

    /**
     * 默认字符串输出格式
     */
    public static final String DEFAULT_STRING_OUTPUT_TYPE = "base64";

    /**
     * 构造返回加解密所需的 PBEConfig
     * @param password 密钥
     * @param algorithm 算法
     * @param keyObtentionIterations 哈希迭代次数
     * @return SimpleStringPBEConfig
     */
    public static SimpleStringPBEConfig getSimpleStringPBEConfig(String password, String algorithm, String keyObtentionIterations) {
        // 简单字符串形式的PBE配置
        SimpleStringPBEConfig config = new SimpleStringPBEConfig();
        config.setPassword(password);
        config.setAlgorithm(StringUtils.isEmpty(algorithm) ? DEFAULT_ALGORITHM : algorithm);
        config.setKeyObtentionIterations(
                StringUtils.isEmpty(keyObtentionIterations) ?
                        String.valueOf(StandardPBEByteEncryptor.DEFAULT_KEY_OBTENTION_ITERATIONS) : keyObtentionIterations
        );
        config.setPoolSize("1");
        config.setProviderName(null);
        config.setSaltGeneratorClassName(DEFAULT_SALT_GENERATOR_CLASSNAME);
        config.setIvGeneratorClassName(DEFAULT_IV_GENERATOR_CLASSNAME);
        config.setStringOutputType(DEFAULT_STRING_OUTPUT_TYPE);
        return config;
    }

    /**
     * 加解密
     * @param password 加解密密钥（必须）
     * @param message 加解密内容（必须）
     * @param isEncrypt true：加密；false：解密
     * @param algorithm 加解密算法
     * @param keyObtentionIterations 哈希迭代次数
     * @return 结果
     */
    public static String stringEncryptor(String password, String message, Boolean isEncrypt, String algorithm, String keyObtentionIterations) {
        PooledPBEStringEncryptor encryptor = new PooledPBEStringEncryptor();
        encryptor.setConfig(getSimpleStringPBEConfig(password, algorithm, keyObtentionIterations));
        return isEncrypt ? encryptor.encrypt(message) : encryptor.decrypt(message);
    }
}
```

## 获取加密后内容

```java
package com.example;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class Controller {
    @GetMapping("/jasypt")
    public String encrypt(String password, String message, Boolean isEncrypt, String algorithm, String keyObtentionIterations) {
        return JasyptUtils.stringEncryptor(password, message, isEncrypt, algorithm, keyObtentionIterations);
    }
}
```

## 配置文件设置

```properties
jasypt.encryptor.password=2022

test=ENC(tbP3ARBg1K6hhyCLd0H1hAi83zP+x3p19h6T9lKOGZ2AArxUbNuszpW7XWp7MFg4)
```

验证：

```java
@RestController
public class Controller {
    @Value("${test}")
    private String message;

    @GetMapping("/message")
    public String getMessage() {
        return message;
    }
}
```



# 自定义配置文件

[SpringBoot系列——加载自定义配置文件 - huanzi-qch - 博客园 (cnblogs.com)](https://www.cnblogs.com/huanzi-qch/p/11122107.html)

[Springboot实战之读取自定义配置文件 - 云+社区 - 腾讯云 (tencent.com)](https://cloud.tencent.com/developer/article/1481440)

[SpringBoot自定义配置文件详解](http://42.192.98.147:8888/article/23.html)

@PropertySource可以用来加载指定的配置文件，默认它只能加载*.properties文件，不能加载诸如yaml等文件。

[Spring Boot实现加载自定义配置文件 - 掘金 (juejin.cn)](https://juejin.cn/post/6874426451043876871)：重要！！！

# 原理

总结来说：其通过 `BeanFactoryPostProcessor#postProcessBeanFactory` 方法，获取所有的 `propertySource` 对象，将所有 `propertySource` 都会重新包装成新的 `EncryptablePropertySourceWrapper`。

解密的时候，也是使用 `EncryptablePropertySourceWrapper#getProperty` 方法，如果通过 `prefixes/suffixes` 包裹的属性，那么返回解密后的值；如果没有被包裹，那么返回原生的值。

## @EnableAutoConfiguration原理

@EnableAutoConfiguration 注解 @Import(AutoConfigurationImportSelector.class)，这个配置类实现了 ImportSelector 接口，重写其 selectImports 方法：

```java
List<String> configurations = getCandidateConfigurations(annotationMetadata, attributes);
```

getCandidateConfigurations 方法会从 classpath 中搜索所有 `META-INF/spring.factories` 配置文件，然后，将其中`org.springframework.boot.autoconfigure.EnableAutoConfiguration` key 对应的配置项加载到 Spring 容器中。这样就实现了在 SpringBoot 中加载外部项目的 bean 或者第三方 jar 中的 bean：

```java
protected List<String> getCandidateConfigurations(AnnotationMetadata metadata,
      AnnotationAttributes attributes) {
   List<String> configurations = SpringFactoriesLoader.loadFactoryNames(
         getSpringFactoriesLoaderFactoryClass(), getBeanClassLoader());
   Assert.notEmpty(configurations,
         "No auto configuration classes found in META-INF/spring.factories. If you "
               + "are using a custom packaging, make sure that file is correct.");
   return configurations;
}
```







# 参考资料

[^1]: [Jasypt 开源加密库使用教程-蚩尤后裔的博客-CSDN博客_org.jasypt](https://blog.csdn.net/wangmx1993328/article/details/106421101)
[^2]: [Jasypt integration for Spring boot (github.com)](https://github.com/ulisesbocchio/jasypt-spring-boot)
[^3]: [Spring boot使用jasypt加密原理解析_const伐伐的博客-CSDN博客_stringencryptor](https://blog.csdn.net/u013905744/article/details/86508236)

重要https://blog.csdn.net/runlion_123/article/details/107056608：原理

