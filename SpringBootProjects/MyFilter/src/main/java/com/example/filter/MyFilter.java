package com.example.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import java.io.IOException;

/**
 * @author chenzufeng
 * @date 2021/11/16
 * @usage MyFilter
 */
// @WebFilter(urlPatterns = "/myFilter")
public class MyFilter implements Filter {
    private static final Logger logger = LoggerFactory.getLogger(MyFilter.class);

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        logger.info("==========进入过滤器==========");

        filterChain.doFilter(servletRequest,servletResponse);
    }
}
