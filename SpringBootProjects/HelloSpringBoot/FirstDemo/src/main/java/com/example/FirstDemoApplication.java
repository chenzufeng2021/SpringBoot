package com.example;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * @author chenzufeng
 * SpringBoot 项目启动入口
 * @SpringBootApplication SpringBoot 核心注解，主要用于开启 Spring 自动配置
 */
@SpringBootApplication
public class FirstDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(FirstDemoApplication.class, args);
    }

}
