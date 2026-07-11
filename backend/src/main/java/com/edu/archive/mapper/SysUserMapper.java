package com.edu.archive.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.edu.archive.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {
    @Select("""
            SELECT r.role_code
            FROM sys_user_role ur
            JOIN sys_role r ON ur.role_id = r.id
            WHERE ur.user_id = #{userId}
            """)
    List<String> findRoleCodesByUserId(Long userId);

    @Select("""
            SELECT p.perm_code
            FROM sys_user_role ur
            JOIN sys_role_permission rp ON ur.role_id = rp.role_id
            JOIN sys_menu_permission p ON rp.permission_id = p.id
            WHERE ur.user_id = #{userId}
            """)
    List<String> findPermissionCodesByUserId(Long userId);
}
