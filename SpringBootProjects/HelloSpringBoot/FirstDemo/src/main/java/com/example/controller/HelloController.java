package com.example.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author chenzufeng
 * @date 2021/10/13
 * @usage HelloController
 */
@RestController
public class HelloController {
    @RequestMapping(value = "/helloSpringBoot")
    public String helloSpringBoot() {
        return "Hello SpringBoot !";
    }
}
