package com.example.controller;

import com.example.entity.User;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

/**
 * @author chenzufeng
 * @date 2021/11/15
 * @usage UserController
 */
@Api(tags = "拦截器接口")
@RestController
@RequestMapping("/user")
public class UserController {

    @ApiOperation(value = "用户登录")
    @PostMapping("/login")
    public String login(
            @RequestBody User user,
            HttpServletRequest request) {
        // 将用户信息存放在session中（setAttribute(String var1, Object var2)）
        request.getSession().setAttribute("user", user);
        return "用户登录认证成功！";
    }

    @ApiOperation(value = "用户登录后访问用户中心")
    @GetMapping("/userCenter")
    public String userCenter() {
        return "访问用户中心！";
    }

    @ApiOperation(value = "管理员直接访问")
    @GetMapping("/admin")
    public String admin() {
        return "管理员直接访问！";
    }

    @ApiOperation(value = "用户访问失败")
    @GetMapping("/error")
    public String error() {
        return "用户访问失败！";
    }
}
