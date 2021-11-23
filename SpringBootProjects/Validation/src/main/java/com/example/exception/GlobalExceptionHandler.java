package com.example.exception;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
import com.example.result.Result;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Path;
import java.util.List;
import java.util.Set;

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
        logger.warn("GlobalExceptionHandler handlerException：{}", ExceptionUtils.getStackTrace(exception));
        return Result.failMessage("系统异常！");
    }

    /**
     * 处理空指针异常
     * @param nullPointerException 空指针异常
     * @return Result
     */
    @ExceptionHandler(NullPointerException.class)
    public Result handlerNullPointerException(NullPointerException nullPointerException) {
        logger.warn("GlobalExceptionHandler handlerNullPointerException：{}", ExceptionUtils.getStackTrace(nullPointerException));
        return Result.failMessage("空指针异常！");
    }

    /**
     * 处理运行时异常
     * @param runtimeException 运行时异常
     * @return Result
     */
    @ExceptionHandler(RuntimeException.class)
    public Result handlerRuntimeException(RuntimeException runtimeException) {
        logger.warn("GlobalExceptionHandler handlerRuntimeException：{}", ExceptionUtils.getStackTrace(runtimeException));
        return Result.failMessage(runtimeException.getMessage());
    }

    /**
     * 处理自定义异常
     * @param customException 自定义异常
     * @return Result
     */
    @ExceptionHandler(CustomException.class)
    public Result handlerCustomException(CustomException customException) {
        logger.warn("GlobalExceptionHandler handlerCustomException：{}", ExceptionUtils.getStackTrace(customException));
        return Result.fail(customException.getCode(), customException.getMessage());
    }

    /**
     * 处理Json请求体对象参数校验失败抛出的异常
     * @param e MethodArgumentNotValidException
     * @return Result
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result handlerJsonParamsException(MethodArgumentNotValidException e) {
        BindingResult bindingResult = e.getBindingResult();
        StringBuilder stringBuilder = new StringBuilder("参数校验失败：");
        for (FieldError fieldError : bindingResult.getFieldErrors()) {
            stringBuilder.append(fieldError.getField()).append(": ")
                    .append(fieldError.getDefaultMessage()).append("; ");
        }
        String message = stringBuilder.toString();
        logger.warn("GlobalExceptionHandler handlerJsonParamsException：{}", message);
        return Result.failMessage(message);
    }

    /**
     * 处理单个参数校验失败抛出的异常
     * @param e ConstraintViolationException
     * @return Result
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public Result handleConstraintViolationException(ConstraintViolationException e) {
        logger.warn("GlobalExceptionHandler handlerJsonParamsException：{}", e.getMessage());
        // cn.hutool.core.collection.CollectionUtil
        /*
        List errorList = CollectionUtil.newArrayList();
        Set<ConstraintViolation<?>> constraintViolations = e.getConstraintViolations();
        for (ConstraintViolation<?> constraintViolation : constraintViolations) {
            StringBuilder stringBuilder = new StringBuilder();
            Path propertyPath = constraintViolation.getPropertyPath();
            String[] pathArr = StrUtil.splitToArray(propertyPath.toString(), ".");
            String message = stringBuilder.append(pathArr[1])
                    .append(constraintViolation.getMessage()).toString();
            errorList.add(message);
        }
         */
        return Result.failMessage(e.getMessage());
    }
}
