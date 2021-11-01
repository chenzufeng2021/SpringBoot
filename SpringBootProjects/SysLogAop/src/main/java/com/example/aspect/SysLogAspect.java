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
