package com.example.utils;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTCreator;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.example.entity.User;

import java.util.Calendar;

/**
 * @author chenzufeng
 * @date 2021/11/17
 */
public class JwtUtil {
    /**
     * signature
     */
    private static String SIGNATURE = "token!Q@W3e4r";

    /**
     * 获取token
     * @param user 用户
     * @return token
     */
    public static String getToken(User user) {
        JWTCreator.Builder builder = JWT.create();

        // payload
        builder.withClaim("id", user.getId());
        builder.withClaim("userName", user.getUserName());
        builder.withClaim("password", user.getPassword());

        // 设置token有效时间
        Calendar instance = Calendar.getInstance();
        instance.add(Calendar.MINUTE, 10);
        builder.withExpiresAt(instance.getTime());

        // signature
        return builder.sign(Algorithm.HMAC256(SIGNATURE)).toString();
    }

    /**
     * 验证token
     * @param token token
     * @return 是否有效
     */
    public static DecodedJWT verifyToken(String token) {
        return JWT.require(Algorithm.HMAC256(SIGNATURE)).build().verify(token);
    }
}
