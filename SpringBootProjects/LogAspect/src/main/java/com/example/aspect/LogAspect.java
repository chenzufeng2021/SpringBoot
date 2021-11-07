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
