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
