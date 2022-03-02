package com.example.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

/**
 * @author chenzufeng
 * @date 2022/3/2
 * @usage MyFilter
 */

public class Test1Filter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(Test1Filter.class);

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest httpServletRequest = (HttpServletRequest) servletRequest;
        logger.info("Test1Filter拦截到客户端向Servlet发送的请求：" + httpServletRequest.getRequestURI());
        filterChain.doFilter(httpServletRequest, servletResponse);
        logger.info("Test1Filter拦截到Servlet向客户端发送的响应！");
    }

    /**
     * 初始化方法只会执行一次
     * @param filterConfig filterConfig
     * @throws ServletException ServletException
     */
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        String filterName = filterConfig.getFilterName();
        logger.info("Test1Filter过滤器初始化方法！过滤器叫作：" + filterName);
    }

    /**
     * 在销毁Filter时自动调用
     */
    @Override
    public void destroy() {
        logger.info("Test1Filter过滤器被销毁！");
    }
}
