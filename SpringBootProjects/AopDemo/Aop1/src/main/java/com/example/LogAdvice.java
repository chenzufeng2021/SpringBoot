package com.example;

import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

/**
 * @author chenzufeng
 * @date 2022/4/4
 */
@Aspect
@Component
public class LogAdvice {
    /**
     * 定义一个切点：所有被 GetMapping 注解修饰的方法会织入 advice
     */
    @Pointcut("@annotation(org.springframework.web.bind.annotation.GetMapping)")
    private void logAdvicePointcut() {
        System.out.println("logAdvicePointcut没有被执行！");
    }

    /**
     * Before表示logAdvice方法将在目标方法执行前执行
     */
    @Before("logAdvicePointcut()")
    public void logAdvice() {
        System.out.println("Get请求的advice被触发了！");
    }
}
