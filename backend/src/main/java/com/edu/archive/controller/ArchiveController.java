package com.edu.archive.controller;


// 【档案管理控制器】档案上传、检索分页、预览下载、编辑删除、提交审核、审核处理、指标绑定。核心业务控制器。
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.edu.archive.common.ApiResponse;
import com.edu.archive.common.SecurityUtils;
import com.edu.archive.entity.ArchiveAuditFlow;
import com.edu.archive.entity.ArchiveInfo;
import com.edu.archive.entity.ArchiveIndicatorRel;
import com.edu.archive.entity.SysUser;
import com.edu.archive.mapper.ArchiveAuditFlowMapper;
import com.edu.archive.mapper.ArchiveInfoMapper;
import com.edu.archive.mapper.ArchiveIndicatorRelMapper;
import com.edu.archive.mapper.CourseObjectiveMapper;
import com.edu.archive.mapper.EduIndicatorMapper;
import com.edu.archive.mapper.GraduationRequirementMapper;
import com.edu.archive.mapper.SysUserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/archive")
public class ArchiveController {
    private final ArchiveInfoMapper archiveInfoMapper;
    private final ArchiveAuditFlowMapper archiveAuditFlowMapper;
    private final ArchiveIndicatorRelMapper archiveIndicatorRelMapper;
    private final EduIndicatorMapper eduIndicatorMapper;
    private final GraduationRequirementMapper graduationRequirementMapper;
    private final CourseObjectiveMapper courseObjectiveMapper;
    private final SysUserMapper userMapper;

    @Autowired
    public ArchiveController(ArchiveInfoMapper archiveInfoMapper,
                             ArchiveAuditFlowMapper archiveAuditFlowMapper,
                             ArchiveIndicatorRelMapper archiveIndicatorRelMapper,
                             EduIndicatorMapper eduIndicatorMapper,
                             GraduationRequirementMapper graduationRequirementMapper,
                             CourseObjectiveMapper courseObjectiveMapper,
                             SysUserMapper userMapper) {
        this.archiveInfoMapper = archiveInfoMapper;
        this.archiveAuditFlowMapper = archiveAuditFlowMapper;
        this.archiveIndicatorRelMapper = archiveIndicatorRelMapper;
        this.eduIndicatorMapper = eduIndicatorMapper;
        this.graduationRequirementMapper = graduationRequirementMapper;
        this.courseObjectiveMapper = courseObjectiveMapper;
        this.userMapper = userMapper;
    }

    @Value("${app.upload-dir}")
    private String uploadDir;

    @PostMapping("/upload")
    @PreAuthorize("hasAuthority('button:archive:upload')")
    public ApiResponse<?> upload(@RequestParam("file") MultipartFile file,
                                 @RequestParam String title,
                                 @RequestParam Long categoryId,
                                 @RequestParam(required = false) String remark) throws Exception {
        if (file == null || file.isEmpty()) {
            return ApiResponse.fail(400, "上传文件不能为空");
        }
        if (!StringUtils.hasText(title)) {
            return ApiResponse.fail(400, "标题不能为空");
        }
        Path baseDir = Paths.get(uploadDir).toAbsolutePath().normalize();
        Files.createDirectories(baseDir);
        String originalName = file.getOriginalFilename() == null ? "unknown.bin" : file.getOriginalFilename();
        String ext = originalName.contains(".") ? originalName.substring(originalName.lastIndexOf(".") + 1) : "bin";
        String saveName = UUID.randomUUID() + "." + ext;
        Path targetPath = baseDir.resolve(saveName).normalize();
        if (!targetPath.startsWith(baseDir)) {
            return ApiResponse.fail(400, "非法保存路径");
        }
        try (var in = file.getInputStream()) {
            Files.copy(in, targetPath, StandardCopyOption.REPLACE_EXISTING);
        }
        File target = targetPath.toFile();

        ArchiveInfo info = new ArchiveInfo();
        info.setArchiveNo("AR" + DateTimeFormatter.ofPattern("yyyyMMddHHmmss").format(LocalDateTime.now()));
        info.setTitle(title);
        info.setCategoryId(categoryId);
        info.setFileName(originalName);
        info.setFilePath(target.getAbsolutePath());
        info.setFileExt(ext.toLowerCase());
        info.setFileSize(file.getSize());
        info.setVersionNo(1);
        info.setLifecycleStatus("COLLECTED");
        info.setCollectedBy(currentUser().getId());
        info.setRemark(remark);
        archiveInfoMapper.insert(info);
        return ApiResponse.ok("上传成功", info);
    }

    @GetMapping("/page")
    public ApiResponse<?> page(@RequestParam(defaultValue = "1") Long pageNo,
                               @RequestParam(defaultValue = "10") Long pageSize,
                               @RequestParam(required = false) String title,
                               @RequestParam(required = false) Long categoryId,
                               @RequestParam(required = false) String lifecycleStatus,
                               @RequestParam(required = false) String startDate,
                               @RequestParam(required = false) String endDate) {
        LambdaQueryWrapper<ArchiveInfo> qw = new LambdaQueryWrapper<>();
        qw.like(StringUtils.hasText(title), ArchiveInfo::getTitle, title)
                .eq(categoryId != null, ArchiveInfo::getCategoryId, categoryId)
                .eq(StringUtils.hasText(lifecycleStatus), ArchiveInfo::getLifecycleStatus, lifecycleStatus)
                .orderByDesc(ArchiveInfo::getId);

        if (StringUtils.hasText(startDate) && StringUtils.hasText(endDate)) {
            qw.between(ArchiveInfo::getCreatedAt, LocalDate.parse(startDate).atStartOfDay(), LocalDate.parse(endDate).atTime(23, 59, 59));
        }
        if (!SecurityUtils.hasAuthority("data:archive:all")) {
            qw.eq(ArchiveInfo::getCollectedBy, currentUser().getId());
        }
        return ApiResponse.ok(archiveInfoMapper.selectPage(new Page<>(pageNo, pageSize), qw));
    }

    @GetMapping("/{id}")
    public ApiResponse<?> detail(@PathVariable Long id) {
        ArchiveInfo info = archiveInfoMapper.selectById(id);
        if (info == null) {
            return ApiResponse.fail(404, "档案不存在");
        }
        return ApiResponse.ok(info);
    }

    @PutMapping("/{id}")
    public ApiResponse<?> update(@PathVariable Long id, @RequestBody ArchiveInfo req) {
        ArchiveInfo info = archiveInfoMapper.selectById(id);
        if (info == null) {
            return ApiResponse.fail(404, "档案不存在");
        }
        info.setTitle(req.getTitle());
        info.setCategoryId(req.getCategoryId());
        info.setRemark(req.getRemark());
        archiveInfoMapper.updateById(info);
        return ApiResponse.ok("更新成功", null);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<?> delete(@PathVariable Long id) {
        ArchiveInfo info = archiveInfoMapper.selectById(id);
        if (info == null) {
            return ApiResponse.fail(404, "档案不存在");
        }
        archiveAuditFlowMapper.delete(
                new LambdaQueryWrapper<ArchiveAuditFlow>().eq(ArchiveAuditFlow::getArchiveId, id));
        archiveIndicatorRelMapper.delete(
                new LambdaQueryWrapper<ArchiveIndicatorRel>().eq(ArchiveIndicatorRel::getArchiveId, id));
        File file = new File(info.getFilePath());
        if (file.exists()) {
            file.delete();
        }
        archiveInfoMapper.deleteById(id);
        return ApiResponse.ok("删除成功", null);
    }

    @PostMapping("/{id}/submit-audit")
    public ApiResponse<?> submitAudit(@PathVariable Long id) {
        ArchiveInfo info = archiveInfoMapper.selectById(id);
        if (info == null) {
            return ApiResponse.fail(404, "档案不存在");
        }
        info.setLifecycleStatus("SUBMITTED");
        archiveInfoMapper.updateById(info);

        ArchiveAuditFlow flow = new ArchiveAuditFlow();
        flow.setArchiveId(id);
        flow.setAuditType("ARCHIVE_AUDIT");
        flow.setStepNo(1);
        flow.setSubmitterId(currentUser().getId());
        flow.setAuditStatus("PENDING");
        archiveAuditFlowMapper.insert(flow);
        return ApiResponse.ok("提交审核成功", null);
    }

    @PostMapping("/{id}/audit")
    @PreAuthorize("hasAuthority('button:archive:audit')")
    public ApiResponse<?> audit(@PathVariable Long id, @RequestBody Map<String, String> body) {
        ArchiveInfo info = archiveInfoMapper.selectById(id);
        if (info == null) {
            return ApiResponse.fail(404, "档案不存在");
        }
        String action = body.getOrDefault("action", "REJECT");
        String comment = body.getOrDefault("comment", "");
        boolean pass = "PASS".equalsIgnoreCase(action);
        info.setLifecycleStatus(pass ? "APPROVED" : "REJECTED");
        info.setArchiveTime(pass ? LocalDateTime.now() : null);
        archiveInfoMapper.updateById(info);

        ArchiveAuditFlow flow = new ArchiveAuditFlow();
        flow.setArchiveId(id);
        flow.setAuditType("ARCHIVE_AUDIT");
        flow.setStepNo(2);
        flow.setSubmitterId(info.getCollectedBy());
        flow.setReviewerId(currentUser().getId());
        flow.setAuditStatus(pass ? "PASSED" : "REJECTED");
        flow.setAuditComment(comment);
        flow.setReviewTime(LocalDateTime.now());
        archiveAuditFlowMapper.insert(flow);
        return ApiResponse.ok("审核完成", null);
    }

    @PostMapping("/{id}/bind-indicator")
    public ApiResponse<?> bindIndicator(@PathVariable Long id, @RequestBody ArchiveIndicatorRel rel) {
        rel.setArchiveId(id);
        if (!StringUtils.hasText(rel.getBindSource())) {
            rel.setBindSource("MANUAL");
        }
        // 检查是否已存在相同绑定
        LambdaQueryWrapper<ArchiveIndicatorRel> checkQw = new LambdaQueryWrapper<ArchiveIndicatorRel>()
                .eq(ArchiveIndicatorRel::getArchiveId, id)
                .eq(ArchiveIndicatorRel::getIndicatorId, rel.getIndicatorId());
        if (rel.getRequirementId() != null) {
            checkQw.eq(ArchiveIndicatorRel::getRequirementId, rel.getRequirementId());
        } else {
            checkQw.isNull(ArchiveIndicatorRel::getRequirementId);
        }
        if (rel.getObjectiveId() != null) {
            checkQw.eq(ArchiveIndicatorRel::getObjectiveId, rel.getObjectiveId());
        } else {
            checkQw.isNull(ArchiveIndicatorRel::getObjectiveId);
        }
        if (archiveIndicatorRelMapper.selectCount(checkQw) > 0) {
            return ApiResponse.fail(400, "该档案已绑定相同的指标组合，请勿重复绑定");
        }
        archiveIndicatorRelMapper.insert(rel);
        return ApiResponse.ok("绑定成功", rel);
    }

    @GetMapping("/{id}/indicator-bindings")
    public ApiResponse<?> getIndicatorBindings(@PathVariable Long id) {
        List<ArchiveIndicatorRel> rels = archiveIndicatorRelMapper.selectList(
                new LambdaQueryWrapper<ArchiveIndicatorRel>().eq(ArchiveIndicatorRel::getArchiveId, id));
        List<Map<String, Object>> result = new java.util.ArrayList<>();
        for (ArchiveIndicatorRel rel : rels) {
            Map<String, Object> item = new java.util.LinkedHashMap<>();
            item.put("id", rel.getId());
            item.put("archiveId", rel.getArchiveId());
            item.put("indicatorId", rel.getIndicatorId());
            item.put("requirementId", rel.getRequirementId());
            item.put("objectiveId", rel.getObjectiveId());
            item.put("bindSource", rel.getBindSource());
            if (rel.getIndicatorId() != null) {
                item.put("indicatorName", eduIndicatorMapper.selectById(rel.getIndicatorId()).getIndicatorName());
            }
            if (rel.getRequirementId() != null) {
                item.put("requirementName", graduationRequirementMapper.selectById(rel.getRequirementId()).getRequirementName());
            }
            if (rel.getObjectiveId() != null) {
                item.put("objectiveName", courseObjectiveMapper.selectById(rel.getObjectiveId()).getObjectiveName());
            }
            result.add(item);
        }
        return ApiResponse.ok(result);
    }

    @DeleteMapping("/{archiveId}/indicator-rel/{relId}")
    public ApiResponse<?> deleteIndicatorBinding(@PathVariable Long archiveId, @PathVariable Long relId) {
        archiveIndicatorRelMapper.delete(new LambdaQueryWrapper<ArchiveIndicatorRel>()
                .eq(ArchiveIndicatorRel::getId, relId)
                .eq(ArchiveIndicatorRel::getArchiveId, archiveId));
        return ApiResponse.ok("解绑成功", null);
    }

    @GetMapping("/download/{id}")
    public ResponseEntity<FileSystemResource> download(@PathVariable Long id) {
        ArchiveInfo info = archiveInfoMapper.selectById(id);
        if (info == null) {
            return ResponseEntity.notFound().build();
        }
        File file = new File(info.getFilePath());
        if (!file.exists()) {
            return ResponseEntity.notFound().build();
        }
        MediaType mediaType = resolveMediaType(info.getFileExt());
        String encodedFileName = URLEncoder.encode(info.getFileName(), StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + encodedFileName + "\"; filename*=UTF-8''" + encodedFileName)
                .body(new FileSystemResource(file));
    }

    @GetMapping("/preview/{id}")
    public ResponseEntity<FileSystemResource> preview(@PathVariable Long id) {
        ArchiveInfo info = archiveInfoMapper.selectById(id);
        if (info == null) {
            return ResponseEntity.notFound().build();
        }
        File file = new File(info.getFilePath());
        if (!file.exists()) {
            return ResponseEntity.notFound().build();
        }
        MediaType mediaType = resolveMediaType(info.getFileExt());
        boolean canInline = isInlinePreviewable(info.getFileExt());
        if (canInline) {
            String encodedFileName = URLEncoder.encode(info.getFileName(), StandardCharsets.UTF_8).replace("+", "%20");
            return ResponseEntity.ok()
                    .contentType(mediaType)
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "inline; filename=\"" + encodedFileName + "\"; filename*=UTF-8''" + encodedFileName)
                    .body(new FileSystemResource(file));
        }
        return ResponseEntity.ok().contentType(mediaType).body(new FileSystemResource(file));
    }

    private MediaType resolveMediaType(String ext) {
        if (ext == null) return MediaType.APPLICATION_OCTET_STREAM;
        return switch (ext.toLowerCase()) {
            case "pdf" -> MediaType.APPLICATION_PDF;
            case "docx" -> MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
            case "doc" -> MediaType.parseMediaType("application/msword");
            case "xlsx" -> MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            case "xls" -> MediaType.parseMediaType("application/vnd.ms-excel");
            case "pptx" -> MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.presentationml.presentation");
            case "ppt" -> MediaType.parseMediaType("application/vnd.ms-powerpoint");
            case "png" -> MediaType.IMAGE_PNG;
            case "jpg", "jpeg" -> MediaType.IMAGE_JPEG;
            case "gif" -> MediaType.IMAGE_GIF;
            case "txt" -> MediaType.TEXT_PLAIN;
            case "html", "htm" -> MediaType.TEXT_HTML;
            case "zip" -> MediaType.parseMediaType("application/zip");
            default -> MediaType.APPLICATION_OCTET_STREAM;
        };
    }

    private boolean isInlinePreviewable(String ext) {
        if (ext == null) return false;
        return switch (ext.toLowerCase()) {
            case "pdf", "png", "jpg", "jpeg", "gif", "txt" -> true;
            default -> false;
        };
    }

    private SysUser currentUser() {
        return userMapper.selectOne(new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, SecurityUtils.username()));
    }
}
