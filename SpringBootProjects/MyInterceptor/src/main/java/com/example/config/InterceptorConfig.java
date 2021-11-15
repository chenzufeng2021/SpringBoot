package com.example.config;

import com.example.interceptor.UserInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * @author chenzufeng
 * @date 2021/11/15
 * @usage InterceptorConfig 注册拦截器
 */
@Configuration
public class InterceptorConfig implements WebMvcConfigurer {
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 定义需要拦截的路径：拦截user下的所有必须登录后才能访问的接口
        String[] addPathPatterns = {"/user/**"};

        //定义不需要拦截的路径
        String[] excludePathPatterns = {
                "/user/admin",
                "/user/error",
                "/user/login"
        };

        // 添加需要注册的拦截器对象
        registry.addInterceptor(new UserInterceptor())
                // 添加需要拦截的路径
                .addPathPatterns(addPathPatterns)
                // 添加不需要拦截的路径
                .excludePathPatterns(excludePathPatterns);
    }
}
