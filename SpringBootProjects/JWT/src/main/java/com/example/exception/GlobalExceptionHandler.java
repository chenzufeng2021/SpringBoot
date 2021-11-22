package com.example.exception;

import com.example.utils.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

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
        logger.warn("GlobalExceptionHandler handlerException：{}", exception);
        return Result.failMessage("系统异常！");
    }

    /**
     * 处理空指针异常
     * @param nullPointerException 空指针异常
     * @return Result
     */
    @ExceptionHandler(NullPointerException.class)
    public Result handlerNullPointerException(NullPointerException nullPointerException) {
        logger.warn("GlobalExceptionHandler handlerNullPointerException：{}", nullPointerException);
        return Result.failMessage("空指针异常！");
    }

    /**
     * 处理运行时异常
     * @param runtimeException 运行时异常
     * @return Result
     */
    @ExceptionHandler(RuntimeException.class)
    public Result handlerRuntimeException(RuntimeException runtimeException) {
        logger.warn("GlobalExceptionHandler handlerRuntimeException：{}", runtimeException);
        return Result.failMessage(runtimeException.getMessage());
    }

    /**
     * 处理自定义异常
     * @param customException 自定义异常
     * @return Result
     */
    @ExceptionHandler(CustomException.class)
    public Result handlerCustomException(CustomException customException) {
        logger.warn("GlobalExceptionHandler handlerCustomException：{}", customException);
        return Result.fail(customException.getCode(), customException.getMessage());
    }
}
