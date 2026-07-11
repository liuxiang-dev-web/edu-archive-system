package com.edu.archive.security;


// 【用户详情加载服务】从数据库加载用户及角色权限，供Spring Security认证和授权使用。
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.edu.archive.entity.SysUser;
import com.edu.archive.mapper.SysUserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class CustomUserDetailsService implements UserDetailsService {
    private final SysUserMapper userMapper;

    @Autowired
    public CustomUserDetailsService(SysUserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        SysUser user = userMapper.selectOne(new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username));
        if (user == null) {
            throw new UsernameNotFoundException("user not found");
        }
        List<GrantedAuthority> authorities = new ArrayList<>();
        List<String> roleCodes = userMapper.findRoleCodesByUserId(user.getId());
        List<String> permissionCodes = userMapper.findPermissionCodesByUserId(user.getId());
        roleCodes.forEach(roleCode -> authorities.add(new SimpleGrantedAuthority("ROLE_" + roleCode)));
        permissionCodes.forEach(code -> authorities.add(new SimpleGrantedAuthority(code)));
        return new User(user.getUsername(), user.getPasswordHash(), user.getStatus() == 1, true, true, true, authorities);
    }
}
