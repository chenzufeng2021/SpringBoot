package com.example;

import java.lang.annotation.*;

/**
 * @author chenzufeng
 * @date 2022/4/4
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface PermissionAnnotation {
}
