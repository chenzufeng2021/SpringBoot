package com.example.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * @author chenzufeng
 * @date 2021/11/7
 * @usage UserService
 */
@Service
public class UserService {
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    public String getUserById(Integer id) {
        logger.info("接口调用方法getUserById：{}", id);
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return "chenzufeng";
    }
}
