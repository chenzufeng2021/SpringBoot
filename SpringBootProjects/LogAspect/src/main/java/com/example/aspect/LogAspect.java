package com.example.aspect;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

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
     * 前置通知
     * @param joinPoint joinPoint
     */
    @Before(value = "pointCut()")
    public void before(JoinPoint joinPoint) {
        logger.info("========================开始执行前置通知========================");
        String name = joinPoint.getSignature().getName();
        logger.info("{} 方法开始执行。。。", name);
    }

    /**
     * 后置通知
     * @param joinPoint joinPoint
     */
    @After(value = "pointCut()")
    public void after(JoinPoint joinPoint) {
        logger.info("========================开始执行后置通知========================");
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
     * 异常通知
     * @param joinPoint joinPoint
     * @param exception 异常
     */
    @AfterThrowing(value = "pointCut()", throwing = "exception")
    public void afterThrowing(JoinPoint joinPoint, Exception exception) {
        logger.info("========================开始执行异常通知========================");
        String name = joinPoint.getSignature().getName();
        logger.info("{} 方法抛出 {} 异常！", name, exception);
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
