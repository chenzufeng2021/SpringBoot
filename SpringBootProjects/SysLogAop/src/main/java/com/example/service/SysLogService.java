package com.example.service;

import com.example.entity.SysLogBO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * @author chenzufeng
 * @date 2021/11/2
 * @usage SysLogService
 * @Slf4j 相当于 private final Logger logger = LoggerFactory.getLogger(X.class);
 */
@Slf4j
@Service
public class SysLogService {
    public boolean save(SysLogBO sysLogBO) {
        log.info(sysLogBO.getParams());
        return true;
    }
}
