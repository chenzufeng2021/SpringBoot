package com.example;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

/**
 * @author chenzufeng
 * @date 2022/4/4
 */
@Aspect
@Component
public class PermissionFirstAdvice {
    /**
     * 定义一个切面，括号内写入自定义注解的路径
     */
    @Pointcut("@annotation(com.example.PermissionAnnotation)")
    private void permissionCheck() {}

    @Around("permissionCheck()")
    public Object permissionCheckFirst(ProceedingJoinPoint proceedingJoinPoint) throws Throwable {
        System.out.println("==========第一个切面==========");
        // 获取请求参数
        Object[] args = proceedingJoinPoint.getArgs();
        User user = (User) args[0];
        if (user.getId() < 0) {
            return "{\"message\":\"illegal id\",\"code\":403}";
        }

        // 修改请求参数
        ObjectMapper objectMapper = new ObjectMapper();
        args[0] = objectMapper.readValue("{\"id\":2, \"name\":\"zufeng\"}", User.class);
        return proceedingJoinPoint.proceed(args);
    }
}
