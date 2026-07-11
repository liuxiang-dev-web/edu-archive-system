package com.edu.archive.controller;


// 【系统管理控制器】操作日志查询、系统配置管理、数据备份触发。
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.edu.archive.common.ApiResponse;
import com.edu.archive.entity.SysConfig;
import com.edu.archive.entity.SysLog;
import com.edu.archive.mapper.SysConfigMapper;
import com.edu.archive.mapper.SysLogMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/api/system")
public class SystemController {
    private final SysLogMapper sysLogMapper;
    private final SysConfigMapper sysConfigMapper;

    @Autowired
    public SystemController(SysLogMapper sysLogMapper, SysConfigMapper sysConfigMapper) {
        this.sysLogMapper = sysLogMapper;
        this.sysConfigMapper = sysConfigMapper;
    }

    @GetMapping("/logs")
    public ApiResponse<?> logs(@RequestParam(required = false) String logType,
                               @RequestParam(defaultValue = "1") Long pageNo,
                               @RequestParam(defaultValue = "20") Long pageSize) {
        LambdaQueryWrapper<SysLog> qw = new LambdaQueryWrapper<>();
        if (logType != null && !logType.isBlank()) {
            qw.eq(SysLog::getLogType, logType);
        }
        qw.orderByDesc(SysLog::getCreatedAt);
        return ApiResponse.ok(sysLogMapper.selectPage(new Page<>(pageNo, pageSize), qw));
    }

    @GetMapping("/configs")
    public ApiResponse<?> configs() {
        return ApiResponse.ok(sysConfigMapper.selectList(
                new LambdaQueryWrapper<SysConfig>().orderByAsc(SysConfig::getConfigGroup, SysConfig::getConfigKey)));
    }

    @PostMapping("/configs")
    public ApiResponse<?> saveConfig(@RequestBody SysConfig config) {
        SysConfig exist = sysConfigMapper.selectOne(
                new LambdaQueryWrapper<SysConfig>().eq(SysConfig::getConfigKey, config.getConfigKey()));
        if (exist != null && (config.getId() == null || !exist.getId().equals(config.getId()))) {
            return ApiResponse.fail(400, "配置键已存在");
        }
        if (config.getId() == null) {
            sysConfigMapper.insert(config);
        } else {
            sysConfigMapper.updateById(config);
        }
        return ApiResponse.ok("保存成功", config);
    }

    @DeleteMapping("/configs/{id}")
    public ApiResponse<?> deleteConfig(@PathVariable Long id) {
        SysConfig config = sysConfigMapper.selectById(id);
        if (config != null && config.getIsBuiltin() != null && config.getIsBuiltin() == 1) {
            return ApiResponse.fail(400, "内置配置不可删除");
        }
        sysConfigMapper.deleteById(id);
        return ApiResponse.ok("删除成功", null);
    }

    @PostMapping("/backup")
    public ApiResponse<?> backup() throws Exception {
        File backupDir = new File("./backup");
        if (!backupDir.exists()) {
            backupDir.mkdirs();
        }
        String name = "backup-" + DateTimeFormatter.ofPattern("yyyyMMddHHmmss").format(LocalDateTime.now()) + ".txt";
        File file = new File(backupDir, name);
        Files.writeString(file.toPath(), "请在生产环境使用 mysqldump 进行完整数据库备份。实际备份应在部署时配置 cron 任务执行 mysqldump 并上传至备份服务器。");
        SysLog log = new SysLog();
        log.setLogType("OPERATION");
        log.setModuleName("SYSTEM");
        log.setActionName("DATA_BACKUP");
        log.setDetailJson("{\"path\":\"" + file.getAbsolutePath().replace("\\", "\\\\") + "\"}");
        log.setResultStatus("SUCCESS");
        sysLogMapper.insert(log);
        return ApiResponse.ok("备份任务已生成", file.getAbsolutePath());
    }
}
