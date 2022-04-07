package com.example;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author chenzufeng
 * @date 2022/4/4
 */
@RestController
@RequestMapping("/permission")
public class PermissionController {
    @PostMapping("check")
    @PermissionAnnotation
    public String getUser(@RequestBody User user) {
        System.out.println(user);
        return user.toString();
    }
}
