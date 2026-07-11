package com.edu.archive.controller;


// 【用户管理控制器】用户列表查询、删除（含外键清理）、状态启用/禁用。仅ADMIN可访问。
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.edu.archive.common.ApiResponse;
import com.edu.archive.common.SecurityUtils;
import com.edu.archive.entity.*;
import com.edu.archive.mapper.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final SysUserMapper userMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysLogMapper sysLogMapper;
    private final SysConfigMapper sysConfigMapper;
    private final ArchiveInfoMapper archiveInfoMapper;
    private final ArchiveAuditFlowMapper archiveAuditFlowMapper;

    @Autowired
    public UserController(SysUserMapper userMapper,
                          SysUserRoleMapper userRoleMapper,
                          SysLogMapper sysLogMapper,
                          SysConfigMapper sysConfigMapper,
                          ArchiveInfoMapper archiveInfoMapper,
                          ArchiveAuditFlowMapper archiveAuditFlowMapper) {
        this.userMapper = userMapper;
        this.userRoleMapper = userRoleMapper;
        this.sysLogMapper = sysLogMapper;
        this.sysConfigMapper = sysConfigMapper;
        this.archiveInfoMapper = archiveInfoMapper;
        this.archiveAuditFlowMapper = archiveAuditFlowMapper;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<?> list() {
        return ApiResponse.ok(userMapper.selectList(
                new LambdaQueryWrapper<SysUser>().orderByAsc(SysUser::getId)));
    }

    @GetMapping("/{userId}/roles")
    public ApiResponse<?> userRoles(@PathVariable Long userId) {
        List<SysUserRole> roles = userRoleMapper.selectList(
                new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, userId)
        );
        return ApiResponse.ok(roles);
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<?> status(@PathVariable Long id, @RequestParam Integer status) {
        SysUser user = new SysUser();
        user.setId(id);
        user.setStatus(status);
        userMapper.updateById(user);
        return ApiResponse.ok("更新成功", null);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<?> delete(@PathVariable Long id) {
        SysUser target = userMapper.selectById(id);
        if (target == null) {
            return ApiResponse.fail(404, "用户不存在");
        }
        String currentUsername = SecurityUtils.username();
        if (currentUsername != null && currentUsername.equals(target.getUsername())) {
            return ApiResponse.fail(400, "不能删除自己");
        }
        // 检查是否有档案归属，有则不允许删除
        long archiveCount = archiveInfoMapper.selectCount(
                new LambdaQueryWrapper<ArchiveInfo>().eq(ArchiveInfo::getCollectedBy, id));
        if (archiveCount > 0) {
            return ApiResponse.fail(400, "该用户下还有 " + archiveCount + " 份档案，请先删除档案或移交后再操作");
        }
        // 检查是否有提交的审核流程
        long submitCount = archiveAuditFlowMapper.selectCount(
                new LambdaQueryWrapper<ArchiveAuditFlow>().eq(ArchiveAuditFlow::getSubmitterId, id));
        if (submitCount > 0) {
            return ApiResponse.fail(400, "该用户还有 " + submitCount + " 条审核记录，无法删除");
        }
        // 解除各外键关联
        archiveAuditFlowMapper.update(null,
                new LambdaUpdateWrapper<ArchiveAuditFlow>().eq(ArchiveAuditFlow::getReviewerId, id)
                        .set(ArchiveAuditFlow::getReviewerId, null));
        sysLogMapper.update(null,
                new LambdaUpdateWrapper<SysLog>().eq(SysLog::getUserId, id).set(SysLog::getUserId, null));
        sysConfigMapper.update(null,
                new LambdaUpdateWrapper<SysConfig>().eq(SysConfig::getUpdatedBy, id).set(SysConfig::getUpdatedBy, null));
        userRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, id));
        userMapper.deleteById(id);
        return ApiResponse.ok("删除成功", null);
    }
}
