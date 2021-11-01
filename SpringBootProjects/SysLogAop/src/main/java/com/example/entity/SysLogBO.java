package com.example.entity;

import lombok.Data;

/**
 * @author chenzufeng
 * @date 2021/11/2
 * @usage SysLogBO
 */
@Data
public class SysLogBO {

    private String className;

    private String methodName;

    private String params;

    private Long executeTime;

    private String remark;

    private String createDate;
}
