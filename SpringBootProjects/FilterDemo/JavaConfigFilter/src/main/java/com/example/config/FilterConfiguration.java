package com.example.config;

import com.example.filter.Test1Filter;
import com.example.filter.Test2Filter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author chenzufeng
 * @date 2021/11/16
 * @usage FilterConfig
 */
@Configuration
public class FilterConfiguration {

    @Bean
    public FilterRegistrationBean<Test1Filter> test1FilterRegistration() {
        // 注册过滤器
        FilterRegistrationBean<Test1Filter> bean = new FilterRegistrationBean<Test1Filter>();
        bean.setFilter(new Test1Filter());
        // 过滤器名称
        bean.setName("filter1");
        // 过滤所有路径
        bean.addUrlPatterns("/*");
        // 设置优先级
        bean.setOrder(10);
        return bean;
    }

    @Bean
    public FilterRegistrationBean<Test2Filter> test2FilterRegistration() {
        // 注册过滤器
        FilterRegistrationBean<Test2Filter> bean = new FilterRegistrationBean<Test2Filter>();
        bean.setFilter(new Test2Filter());
        // 过滤器名称
        bean.setName("filter2");
        // 过滤所有路径
        bean.addUrlPatterns("/test/*");
        // 设置优先级
        bean.setOrder(6);
        return bean;
    }
}
