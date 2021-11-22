package com.example.controller;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.example.constants.ResultConstants;
import com.example.entity.User;
import com.example.service.UserService;
import com.example.utils.JwtUtil;
import com.example.utils.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

/**
 * @author chenzufeng
 * @date 2021/11/17
 */
@Api(tags = "用户登录接口")
@RestController
@RequestMapping("/user")
public class UserController {

    public static final Logger logger = LoggerFactory.getLogger(UserController.class);

    @Autowired
    private UserService userService;

    @PostMapping("/login")
    @ApiOperation(value = "login")
    public Result login(@RequestBody User user) {
        User userDb = userService.login(user);
        if (userDb != null) {
            String token = JwtUtil.getToken(userDb);
            return Result.success(ResultConstants.Constants.CODE_SUCCESS, "登录成功！", token);
        }
        return Result.fail(ResultConstants.Constants.CODE_FAIL, "登录失败！");
    }

    @GetMapping("/test")
    @ApiOperation(value = "AuthorizationTest")
    public Result test(HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        DecodedJWT decodedJWT = JwtUtil.verifyToken(token);
        Integer id = decodedJWT.getClaim("id").asInt();
        // decodedJWT.getClaim("password").asString()会使得userName为空
        String userName = decodedJWT.getClaims().get("userName").asString();
        String password = decodedJWT.getClaims().get("password").asString();
        logger.info("用户ID：{}， 用户名：{}，用户密码：{}", id, userName, password);
        return Result.success("请求成功！");
    }
}
