package com.example.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author chenzufeng
 * @date 2021/11/16
 * @usage UserController
 */
@RestController
public class UserController {
    @GetMapping("/user/detail")
    public String userDetail() {
        return "/user/detail";
    }

    @GetMapping("/userCenter")
    public String userCenter() {
        return "/userCenter";
    }
}
