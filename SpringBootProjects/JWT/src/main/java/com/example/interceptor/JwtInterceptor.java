package com.example.interceptor;

import com.auth0.jwt.exceptions.InvalidClaimException;
import com.auth0.jwt.exceptions.SignatureVerificationException;
import com.auth0.jwt.exceptions.TokenExpiredException;
import com.example.exception.CustomException;
import com.example.utils.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author chenzufeng
 * @date 2021/11/17
 */
public class JwtInterceptor implements HandlerInterceptor {

    public static final Logger logger = LoggerFactory.getLogger(JwtInterceptor.class);

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 获取请求头中的token
        String token = request.getHeader("Authorization");
        logger.info("token为：{}", token);

        try {
            JwtUtil.verifyToken(token);
            return true;
        } catch (SignatureVerificationException e) {
            e.printStackTrace();
            throw new CustomException("签名不一致！");
        } catch (TokenExpiredException e) {
            e.printStackTrace();
            throw new CustomException("token过期！");
        } catch (InvalidClaimException e) {
            e.printStackTrace();
            throw new CustomException("失效的payload！");
        } catch (Exception e) {
            e.printStackTrace();
            throw new CustomException("token无效！");
        }
    }
}
