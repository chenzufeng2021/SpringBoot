package com.example;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author chenzufeng
 * @date 2022/4/4
 */
@RestController
@RequestMapping("/aop")
public class AopController {
    @GetMapping("get")
    public String aopGet() {
        return "aopGet";
    }

    @PostMapping("post")
    public String aopPost(String id) {
        return "aopPost";
    }
}
