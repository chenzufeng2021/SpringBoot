package com.example.controller;

import com.example.entity.UserDTO;
import com.example.result.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.hibernate.validator.constraints.Length;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.NotEmpty;

/**
 * @author chenzufeng
 * @date 2021/11/23
 * @usage UserController
 */
@RestController
@Api(tags = "用户参数校验接口")
@Validated
public class UserController {

    @ApiOperation(value = "添加用户")
    @PostMapping("/saveUser")
    public Result saveUser(@RequestBody @Validated UserDTO userDTO) {
        // 通过校验才会执行业务逻辑
        return Result.success(userDTO);
    }

    @GetMapping("/getByAccount")
    @ApiOperation(value = "根据账号获取信息")
    public Result getByAccount(
            @RequestParam @ApiParam(value = "账号", required = true)
            @NotEmpty @Length(max = 3) String account,
            @RequestParam @ApiParam(value = "用户名", required = true)
            @NotEmpty @Length(max = 3)String userName) {
        return Result.success();
    }
}
