package com.example.constants;

/**
 * @author chenzufeng
 * @date 2021/11/17
 */
public class ResultConstants {
    public enum Constants {
        ;
        /**
         * 操作成功编码
         */
        public static final String CODE_SUCCESS = "200";
        /**
         * 操作成功提示信息
         */
        public static final String MSG_SUCCESS = "操作成功";

        /**
         * 操作失败编码
         */
        public static final String CODE_FAIL = "400";
        /**
         * 操作失败提示信息
         */
        public static final String MSG_FAIL = "操作失败";

        /**
         * 没有权限编码
         */
        public static final String CODE_INVALID_FAIL = "401";
        /**
         * 没有权限提示信息
         */
        public static final String MSG_INVALID_FAIL = "校验未通过，没有权限";

        /**
         * 未登录编码
         */
        public static final String CODE_FAIL_NOT_LOGIN = "305";
        /**
         * 未登录提示信息
         */
        public static final String MSG_FAIL_NOT_LOGIN = "未登录";
    }
}
