package com.edu.archive.config;


// 【JWT认证入口点】处理未登录或Token无效的请求，返回401 JSON响应。
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
public class JwtAuthEntryPoint implements AuthenticationEntryPoint {
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        try {
            response.getOutputStream().write("{\"code\":401,\"message\":\"未登录或token无效\",\"data\":null}".getBytes(StandardCharsets.UTF_8));
        } catch (Exception ignored) {
        }
    }
}
