package com.example.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author chenzufeng
 * @date 2022/3/3
 * @usage FilterController
 */
@RestController
public class FilterController {
    private static final Logger logger = LoggerFactory.getLogger(FilterController.class);

    @RequestMapping("/filter")
    public void testFilter() {
        logger.info("testFilter()：测试路径/filter！");
    }

    @RequestMapping("/test/filter")
    public void testFilter1() {
        logger.info("testFilter1()：测试路径/test/filter！");
    }
}
