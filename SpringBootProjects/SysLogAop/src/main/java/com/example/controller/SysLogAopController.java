package com.example.controller;

import com.example.annotation.SysLog;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author chenzufeng
 * @date 2021/11/2
 * @usage SysLogAopController
 */
@RestController
@RequestMapping("/aop")
public class SysLogAopController {
    @SysLog("测试AOP")
    @GetMapping("/test")
    public String test(
            @RequestParam("name") String name,
            @RequestParam("age") Integer age) {
        return name + " " + age;
    }
}
