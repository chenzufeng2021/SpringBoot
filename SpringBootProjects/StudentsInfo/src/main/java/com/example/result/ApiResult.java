package com.example.result;

import java.io.Serializable;

/**
 * @author chenzufeng
 * @date 2021/10/22
 * @usage ApiResult
 */
public class ApiResult<T extends Object> implements Serializable {

    private static final long serialVersionUID = -2312680312398041174L;

    /**
     * 状态码
     */
    private String code;

    /**
     * 数据对象
     */
    private T data;

    /**
     * 错误码
     */
    private String errorCode;

    /**
     * 信息
     */
    private String message;

    /**
     * 数据对象，兼容存量API
     */
    private T model;

    /**
     * 兼容标识：开启后，兼容原ApiResult数据输出model，默认false
     */
    private transient boolean compatible = false;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public T getData() {
        return data;
    }

    /**
     * 设置数据对象data
     * @param data 数据对象
     */
    public void setData(T data) {
        this.data = data;

        if (this.compatible) {
            this.model = data;
        }
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getModel() {
        return model;
    }

    public void setModel(T model) {
        this.model = model;
    }

    public boolean isCompatible() {
        return compatible;
    }

    /**
     * 设置是否兼容旧模式
     * @param compatible compatible
     */
    public void setCompatible(boolean compatible) {
        this.compatible = compatible;

        if (this.compatible) {
            this.model = data;
        }
    }
}
