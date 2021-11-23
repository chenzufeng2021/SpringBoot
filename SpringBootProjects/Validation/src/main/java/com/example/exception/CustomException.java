package com.example.exception;

import com.example.result.ResultConstants;

/**
 * @author chenzufeng
 * @date 2021/11/17
 */
public class CustomException extends RuntimeException {

    private String code = ResultConstants.Constants.MSG_FAIL;

    private String message;

    public CustomException(String message) {
        super(message);
        this.message = message;
    }

    public CustomException(String code, String message) {
        super(message);
        this.code = code;
        this.message = message;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    @Override
    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
