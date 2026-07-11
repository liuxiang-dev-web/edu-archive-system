package com.edu.archive.dto;


// 【认证DTO】LoginReq(登录请求)、RegisterReq(注册请求，含email字段)、AuthResp(认证响应)。
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class AuthDtos {
    public record LoginReq(
            @NotBlank(message = "用户名不能为空") String username,
            @NotBlank(message = "密码不能为空") String password
    ) {}
    public record RegisterReq(
            @NotBlank(message = "用户名不能为空") @Size(min = 3, max = 64, message = "用户名长度需为3-64") String username,
            @NotBlank(message = "密码不能为空") @Size(min = 6, max = 32, message = "密码长度需为6-32") String password,
            @NotBlank(message = "真实姓名不能为空") String realName,
            String roleCode,
            String email
    ) {}
    public record AuthResp(String token, String username, String roleCode) {}
}
