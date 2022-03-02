package com.example.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import java.io.IOException;

/**
 * @author chenzufeng
 * @date 2022/3/2
 * @usage MyFilter
 */
@Order(1)
@WebFilter(filterName = "myFilter", urlPatterns = "/*")
public class MyFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(MyFilter.class);

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        logger.info("我是过滤器的执行方法，客户端向Servlet发送的请求被我拦截到了！");
        filterChain.doFilter(servletRequest, servletResponse);
        logger.info("我是过滤器的执行方法，Servlet向客户端发送的响应被我拦截到了！");
    }

    /**
     * 初始化方法只会执行一次
     * @param filterConfig filterConfig
     * @throws ServletException ServletException
     */
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        String filterName = filterConfig.getFilterName();
        logger.info("过滤器初始化方法！过滤器叫作：" + filterName);
    }

    /**
     * 在销毁Filter时自动调用
     */
    @Override
    public void destroy() {
        logger.info("过滤器被销毁！");
    }
}
