package com.example.utils;

import com.example.constants.ResultConstants;

/**
 * @author chenzufeng
 * @date 2021/11/17
 */
public class Result {

    private String code;
    private Boolean success;
    private String message;
    private Object data;

    private Result(Boolean success, String code, String message, Object data) {
        this.success = success;
        this.code = code;
        this.message = message;
        this.data = data;
    }

    /**
     * 调用成功
     * @param code 编码
     * @param message 详细信息
     * @param data 数据
     * @return 统一结果格式
     */
    public static Result success(String code, String message, Object data) {
        return new Result(true, code, message, data);
    }

    public static Result success(String code, Object data) {
        return success(code, ResultConstants.Constants.MSG_SUCCESS, data);
    }

    public static Result success(Object data) {
        return success(ResultConstants.Constants.CODE_SUCCESS, data);
    }

    public static Result success(String code, String message) {
        return success(code, message, null);
    }

    public static Result success(String message) {
        return success(ResultConstants.Constants.CODE_SUCCESS, message, null);
    }

    public static Result success() {
        return success(ResultConstants.Constants.MSG_SUCCESS);
    }

    /**
     * 调用失败
     * @param code 编码
     * @param message 详细信息
     * @param data 数据
     * @return 统一结果格式
     */
    public static Result fail(String code, String message, Object data) {
        return new Result(false, code, message, data);
    }

    public static Result fail(String code, String message) {
        return fail(code, message, null);
    }

    public static Result fail(String code, Object data) {
        return fail(code, ResultConstants.Constants.MSG_FAIL, data);
    }

    public static Result failCode(String code) {
        return fail(code, ResultConstants.Constants.MSG_FAIL);
    }

    public static Result failMessage(String message) {
        return fail(ResultConstants.Constants.CODE_FAIL, message);
    }

    public static Result fail() {
        return fail(ResultConstants.Constants.CODE_FAIL, ResultConstants.Constants.MSG_FAIL);
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public Boolean getSuccess() {
        return success;
    }

    public void setSuccess(Boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }
}
