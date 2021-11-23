package com.example.entity;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.NotBlank;

/**
 * @author chenzufeng
 * @date 2021/11/23
 * @usage UserDTO
 */
@Data
@ApiModel(value = "UserDTO")
public class UserDTO {
    @ApiModelProperty(value = "用户ID")
    private Long userId;

    @ApiModelProperty(value = "用户名")
    @NotBlank
    @Length(max = 3)
    private String userName;

    @ApiModelProperty(value = "账号")
    @NotBlank
    @Length(max = 3)
    private String account;

    @ApiModelProperty(value = "密码")
    @NotBlank
    @Length(max = 3)
    private String password;
}
