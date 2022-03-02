package com.example.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author chenzufeng
 * @date 2022/3/2
 * @usage FilterController
 */
@RestController
public class FilterController {
    private static final Logger logger = LoggerFactory.getLogger(FilterController.class);

    @RequestMapping("/webFilter")
    public void testWebFilter() {
        logger.info("测试WebFilter！");
    }
}
