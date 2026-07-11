package com.edu.archive.controller;


// 【角色权限管理控制器】角色CRUD、权限列表、角色-权限关联、用户-角色分配。仅ADMIN可访问。
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.edu.archive.common.ApiResponse;
import com.edu.archive.entity.*;
import com.edu.archive.mapper.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class RolePermissionController {
    private final SysRoleMapper roleMapper;
    private final SysMenuPermissionMapper permMapper;
    private final SysRolePermissionMapper rolePermMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysUserMapper userMapper;

    @Autowired
    public RolePermissionController(SysRoleMapper roleMapper,
                                     SysMenuPermissionMapper permMapper,
                                     SysRolePermissionMapper rolePermMapper,
                                     SysUserRoleMapper userRoleMapper,
                                     SysUserMapper userMapper) {
        this.roleMapper = roleMapper;
        this.permMapper = permMapper;
        this.rolePermMapper = rolePermMapper;
        this.userRoleMapper = userRoleMapper;
        this.userMapper = userMapper;
    }

    // ===== 角色管理 =====
    @GetMapping("/roles")
    public ApiResponse<?> listRoles() {
        return ApiResponse.ok(roleMapper.selectList(
                new LambdaQueryWrapper<SysRole>().orderByAsc(SysRole::getSortNo)));
    }

    @PostMapping("/roles")
    public ApiResponse<?> createRole(@RequestBody SysRole role) {
        SysRole exist = roleMapper.selectOne(
                new LambdaQueryWrapper<SysRole>().eq(SysRole::getRoleCode, role.getRoleCode()));
        if (exist != null) return ApiResponse.fail(400, "角色编码已存在");
        roleMapper.insert(role);
        return ApiResponse.ok("创建成功", role);
    }

    @PutMapping("/roles/{id}")
    public ApiResponse<?> updateRole(@PathVariable Long id, @RequestBody SysRole role) {
        SysRole exist = roleMapper.selectOne(
                new LambdaQueryWrapper<SysRole>().eq(SysRole::getRoleCode, role.getRoleCode()).ne(SysRole::getId, id));
        if (exist != null) return ApiResponse.fail(400, "角色编码已存在");
        role.setId(id);
        roleMapper.updateById(role);
        return ApiResponse.ok("更新成功", null);
    }

    @DeleteMapping("/roles/{id}")
    public ApiResponse<?> deleteRole(@PathVariable Long id) {
        rolePermMapper.delete(new LambdaQueryWrapper<SysRolePermission>().eq(SysRolePermission::getRoleId, id));
        userRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getRoleId, id));
        roleMapper.deleteById(id);
        return ApiResponse.ok("删除成功", null);
    }

    // ===== 权限列表 =====
    @GetMapping("/permissions")
    public ApiResponse<?> listPermissions() {
        return ApiResponse.ok(permMapper.selectList(
                new LambdaQueryWrapper<SysMenuPermission>().orderByAsc(SysMenuPermission::getSortNo)));
    }

    // ===== 角色-权限关联 =====
    @GetMapping("/roles/{roleId}/permissions")
    public ApiResponse<?> rolePermissions(@PathVariable Long roleId) {
        List<SysRolePermission> rels = rolePermMapper.selectList(
                new LambdaQueryWrapper<SysRolePermission>().eq(SysRolePermission::getRoleId, roleId));
        List<Long> permIds = rels.stream().map(SysRolePermission::getPermissionId).collect(Collectors.toList());
        return ApiResponse.ok(Map.of("roleId", roleId, "permissionIds", permIds));
    }

    @PostMapping("/roles/{roleId}/permissions")
    public ApiResponse<?> saveRolePermissions(@PathVariable Long roleId, @RequestBody Map<String, List<Long>> body) {
        List<Long> permIds = body.getOrDefault("permissionIds", List.of());
        rolePermMapper.delete(new LambdaQueryWrapper<SysRolePermission>().eq(SysRolePermission::getRoleId, roleId));
        for (Long permId : permIds) {
            SysRolePermission rel = new SysRolePermission();
            rel.setRoleId(roleId);
            rel.setPermissionId(permId);
            rolePermMapper.insert(rel);
        }
        return ApiResponse.ok("保存成功", null);
    }

    // ===== 用户-角色分配 =====
    @GetMapping("/users/{userId}/roles")
    public ApiResponse<?> userRoles(@PathVariable Long userId) {
        List<SysUserRole> rels = userRoleMapper.selectList(
                new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, userId));
        List<Long> roleIds = rels.stream().map(SysUserRole::getRoleId).collect(Collectors.toList());
        SysUser user = userMapper.selectById(userId);
        return ApiResponse.ok(Map.of("userId", userId, "username", user != null ? user.getUsername() : "", "roleIds", roleIds));
    }

    @PostMapping("/users/{userId}/roles")
    public ApiResponse<?> saveUserRoles(@PathVariable Long userId, @RequestBody Map<String, List<Long>> body) {
        List<Long> roleIds = body.getOrDefault("roleIds", List.of());
        userRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, userId));
        for (Long roleId : roleIds) {
            SysUserRole rel = new SysUserRole();
            rel.setUserId(userId);
            rel.setRoleId(roleId);
            userRoleMapper.insert(rel);
        }
        return ApiResponse.ok("保存成功", null);
    }

    @GetMapping("/users")
    public ApiResponse<?> listUsers() {
        List<SysUser> users = userMapper.selectList(
                new LambdaQueryWrapper<SysUser>().select(SysUser::getId, SysUser::getUsername, SysUser::getRealName));
        return ApiResponse.ok(users);
    }
}
