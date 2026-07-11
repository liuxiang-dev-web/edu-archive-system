package com.edu.archive.controller;


// 【认证控制器】处理用户登录、注册、获取当前用户信息。JWT令牌在此签发。
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.edu.archive.common.ApiResponse;
import com.edu.archive.common.SecurityUtils;
import com.edu.archive.dto.AuthDtos;
import com.edu.archive.entity.SysLog;
import com.edu.archive.entity.SysRole;
import com.edu.archive.entity.SysUser;
import com.edu.archive.entity.SysUserRole;
import com.edu.archive.mapper.SysLogMapper;
import com.edu.archive.mapper.SysRoleMapper;
import com.edu.archive.mapper.SysUserMapper;
import com.edu.archive.mapper.SysUserRoleMapper;
import com.edu.archive.security.CustomUserDetailsService;
import com.edu.archive.security.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private static final Logger log = LoggerFactory.getLogger(AuthController.class);
    private final AuthenticationManager authenticationManager;
    private final CustomUserDetailsService userDetailsService;
    private final JwtUtil jwtUtil;
    private final SysUserMapper userMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysRoleMapper roleMapper;
    private final SysLogMapper sysLogMapper;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public AuthController(AuthenticationManager authenticationManager,
                          CustomUserDetailsService userDetailsService,
                          JwtUtil jwtUtil,
                          SysUserMapper userMapper,
                          SysUserRoleMapper userRoleMapper,
                          SysRoleMapper roleMapper,
                          SysLogMapper sysLogMapper,
                          PasswordEncoder passwordEncoder) {
        this.authenticationManager = authenticationManager;
        this.userDetailsService = userDetailsService;
        this.jwtUtil = jwtUtil;
        this.userMapper = userMapper;
        this.userRoleMapper = userRoleMapper;
        this.roleMapper = roleMapper;
        this.sysLogMapper = sysLogMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/register")
    public ApiResponse<?> register(@Valid @RequestBody AuthDtos.RegisterReq req) {
        if (userMapper.selectOne(new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, req.username())) != null) {
            return ApiResponse.fail(400, "用户名已存在");
        }
        SysUser user = new SysUser();
        user.setUsername(req.username());
        user.setPasswordHash(passwordEncoder.encode(req.password()));
        user.setRealName(req.realName());
        user.setEmail(req.email());
        user.setStatus(1);
        userMapper.insert(user);
        String roleCode = req.roleCode() == null ? "TEACHER" : req.roleCode();
        SysRole role = roleMapper.selectOne(new LambdaQueryWrapper<SysRole>().eq(SysRole::getRoleCode, roleCode));
        if (role != null) {
            SysUserRole userRole = new SysUserRole();
            userRole.setUserId(user.getId());
            userRole.setRoleId(role.getId());
            userRoleMapper.insert(userRole);
        }
        return ApiResponse.ok("注册成功", null);
    }

    @PostMapping("/login")
    public ApiResponse<?> login(@Valid @RequestBody AuthDtos.LoginReq req, HttpServletRequest request) {
        try {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(req.username(), req.password()));
            UserDetails userDetails = userDetailsService.loadUserByUsername(req.username());
            String token = jwtUtil.generateToken(userDetails);
            SysUser user = userMapper.selectOne(new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, req.username()));
            writeLoginLog(user.getId(), req.username(), "SUCCESS", request);
            List<String> roles = userMapper.findRoleCodesByUserId(user.getId());
            return ApiResponse.ok(new AuthDtos.AuthResp(token, req.username(), roles.isEmpty() ? "TEACHER" : roles.get(0)));
        } catch (org.springframework.security.authentication.DisabledException e) {
            writeLoginLog(null, req.username(), "DISABLED", request);
            log.warn("Disabled account login attempt: {}", req.username());
            return ApiResponse.fail(401, "账号已被禁用，请联系管理员");
        } catch (Exception e) {
            writeLoginLog(null, req.username(), "FAIL", request);
            log.error("Login failed for {}", req.username(), e);
            return ApiResponse.fail(401, "用户名或密码错误");
        }
    }

    @PostMapping("/logout")
    public ApiResponse<?> logout() {
        return ApiResponse.ok("退出成功", null);
    }

    @GetMapping("/me")
    public ApiResponse<?> me() {
        String username = SecurityUtils.username();
        SysUser user = userMapper.selectOne(new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username));
        List<String> permissions = userMapper.findPermissionCodesByUserId(user.getId());
        List<String> roles = userMapper.findRoleCodesByUserId(user.getId());
        Map<String, Object> map = new HashMap<>();
        map.put("user", user);
        map.put("roles", roles);
        map.put("permissions", permissions);
        return ApiResponse.ok(map);
    }

    private void writeLoginLog(Long userId, String username, String status, HttpServletRequest request) {
        SysLog log = new SysLog();
        log.setLogType("LOGIN");
        log.setModuleName("AUTH");
        log.setActionName("LOGIN");
        log.setUserId(userId);
        log.setUsername(username);
        log.setRequestUri(request.getRequestURI());
        log.setRequestMethod(request.getMethod());
        log.setIpAddr(request.getRemoteAddr());
        log.setUserAgent(request.getHeader("User-Agent"));
        log.setResultStatus(status);
        sysLogMapper.insert(log);
    }
}
