package com.example.config;

import com.example.filter.MyFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author chenzufeng
 * @date 2021/11/16
 * @usage FilterConfig
 */
@Configuration
public class FilterConfig {

    @Bean
    public FilterRegistrationBean myFilterRegistration() {
        // 注册过滤器
        FilterRegistrationBean filterRegistrationBean = new FilterRegistrationBean(new MyFilter());
        // 添加过滤路径
        filterRegistrationBean.addUrlPatterns("/user/*");

        return filterRegistrationBean;
    }
}
