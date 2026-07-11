package com.edu.archive.controller;


// 【认证特色控制器】档案绑定认证要素、自动筛选佐证材料、一键导出ZIP、课程目标达成度清单。
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.edu.archive.common.ApiResponse;
import com.edu.archive.entity.ArchiveIndicatorRel;
import com.edu.archive.entity.ArchiveInfo;
import com.edu.archive.mapper.ArchiveIndicatorRelMapper;
import com.edu.archive.mapper.ArchiveInfoMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@RestController
@RequestMapping("/api/certification")
public class CertificationController {
    private final ArchiveInfoMapper archiveInfoMapper;
    private final ArchiveIndicatorRelMapper archiveIndicatorRelMapper;

    @Autowired
    public CertificationController(ArchiveInfoMapper archiveInfoMapper,
                                   ArchiveIndicatorRelMapper archiveIndicatorRelMapper) {
        this.archiveInfoMapper = archiveInfoMapper;
        this.archiveIndicatorRelMapper = archiveIndicatorRelMapper;
    }

    @Value("${app.upload-dir}")
    private String uploadDir;

    /**
     * 1) 档案绑定认证指标、毕业要求、课程目标
     */
    @PostMapping("/bind")
    public ApiResponse<?> bindArchive(@RequestBody ArchiveIndicatorRel rel) {
        if (rel.getArchiveId() == null || rel.getIndicatorId() == null) {
            return ApiResponse.fail(400, "archiveId 和 indicatorId 不能为空");
        }
        ArchiveInfo archive = archiveInfoMapper.selectById(rel.getArchiveId());
        if (archive == null) {
            return ApiResponse.fail(404, "档案不存在");
        }
        // 检查重复绑定
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ArchiveIndicatorRel> checkQw =
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ArchiveIndicatorRel>()
                        .eq(ArchiveIndicatorRel::getArchiveId, rel.getArchiveId())
                        .eq(ArchiveIndicatorRel::getIndicatorId, rel.getIndicatorId());
        if (rel.getRequirementId() != null) checkQw.eq(ArchiveIndicatorRel::getRequirementId, rel.getRequirementId());
        else checkQw.isNull(ArchiveIndicatorRel::getRequirementId);
        if (rel.getObjectiveId() != null) checkQw.eq(ArchiveIndicatorRel::getObjectiveId, rel.getObjectiveId());
        else checkQw.isNull(ArchiveIndicatorRel::getObjectiveId);
        if (archiveIndicatorRelMapper.selectCount(checkQw) > 0) {
            return ApiResponse.fail(400, "该档案已绑定相同的指标组合，请勿重复绑定");
        }
        rel.setBindSource(StringUtils.hasText(rel.getBindSource()) ? rel.getBindSource() : "MANUAL");
        archiveIndicatorRelMapper.insert(rel);
        return ApiResponse.ok("绑定成功", rel);
    }

    /**
     * 2) 按认证要求自动筛选佐证材料
     */
    @GetMapping("/materials/auto-filter")
    public ApiResponse<?> autoFilterMaterials(@RequestParam(required = false) Long indicatorId,
                                              @RequestParam(required = false) Long requirementId,
                                              @RequestParam(required = false) Long objectiveId,
                                              @RequestParam(defaultValue = "true") boolean approvedOnly) {
        LambdaQueryWrapper<ArchiveIndicatorRel> relQw = new LambdaQueryWrapper<>();
        relQw.eq(indicatorId != null, ArchiveIndicatorRel::getIndicatorId, indicatorId)
                .eq(requirementId != null, ArchiveIndicatorRel::getRequirementId, requirementId)
                .eq(objectiveId != null, ArchiveIndicatorRel::getObjectiveId, objectiveId);
        List<ArchiveIndicatorRel> relList = archiveIndicatorRelMapper.selectList(relQw);
        if (relList.isEmpty()) {
            return ApiResponse.ok(List.of());
        }
        Set<Long> archiveIds = relList.stream().map(ArchiveIndicatorRel::getArchiveId).collect(Collectors.toSet());

        LambdaQueryWrapper<ArchiveInfo> archiveQw = new LambdaQueryWrapper<>();
        archiveQw.in(ArchiveInfo::getId, archiveIds)
                .eq(approvedOnly, ArchiveInfo::getLifecycleStatus, "APPROVED")
                .orderByDesc(ArchiveInfo::getArchiveTime);
        return ApiResponse.ok(archiveInfoMapper.selectList(archiveQw));
    }

    /**
     * 3) 生成“课程目标达成度关联档案清单”
     */
    @GetMapping("/objective/{objectiveId}/archive-list")
    public ApiResponse<?> objectiveArchiveList(@PathVariable Long objectiveId) {
        List<ArchiveIndicatorRel> relList = archiveIndicatorRelMapper.selectList(
                new LambdaQueryWrapper<ArchiveIndicatorRel>().eq(ArchiveIndicatorRel::getObjectiveId, objectiveId)
        );
        Set<Long> archiveIds = relList.stream().map(ArchiveIndicatorRel::getArchiveId).collect(Collectors.toSet());
        List<ArchiveInfo> archives = archiveIds.isEmpty()
                ? new ArrayList<>()
                : archiveInfoMapper.selectList(new LambdaQueryWrapper<ArchiveInfo>()
                .in(ArchiveInfo::getId, archiveIds)
                .orderByDesc(ArchiveInfo::getArchiveTime));

        long approvedCount = archives.stream().filter(a -> "APPROVED".equalsIgnoreCase(a.getLifecycleStatus())).count();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("objectiveId", objectiveId);
        result.put("archiveCount", archives.size());
        result.put("approvedCount", approvedCount);
        result.put("achievementArchiveRate", archives.isEmpty() ? 0 : (approvedCount * 100.0 / archives.size()));
        result.put("archives", archives);
        return ApiResponse.ok(result);
    }

    /**
     * 4) 支持认证材料一键导出（ZIP）
     */
    @GetMapping("/export")
    public ResponseEntity<FileSystemResource> exportCertificationMaterials(@RequestParam(required = false) Long indicatorId,
                                                                           @RequestParam(required = false) Long requirementId,
                                                                           @RequestParam(required = false) Long objectiveId) throws Exception {
        List<ArchiveIndicatorRel> relList = archiveIndicatorRelMapper.selectList(
                new LambdaQueryWrapper<ArchiveIndicatorRel>()
                        .eq(indicatorId != null, ArchiveIndicatorRel::getIndicatorId, indicatorId)
                        .eq(requirementId != null, ArchiveIndicatorRel::getRequirementId, requirementId)
                        .eq(objectiveId != null, ArchiveIndicatorRel::getObjectiveId, objectiveId)
        );
        Set<Long> archiveIds = relList.stream().map(ArchiveIndicatorRel::getArchiveId).collect(Collectors.toSet());
        List<ArchiveInfo> archives = archiveIds.isEmpty()
                ? List.of()
                : archiveInfoMapper.selectList(new LambdaQueryWrapper<ArchiveInfo>()
                .in(ArchiveInfo::getId, archiveIds)
                .eq(ArchiveInfo::getLifecycleStatus, "APPROVED"));

        File outDir = new File(uploadDir, "exports");
        if (!outDir.exists()) {
            outDir.mkdirs();
        }
        String fileName = "cert-materials-" + DateTimeFormatter.ofPattern("yyyyMMddHHmmss").format(LocalDateTime.now()) + ".zip";
        File zipFile = new File(outDir, fileName);

        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile), StandardCharsets.UTF_8)) {
            // manifest
            ZipEntry manifestEntry = new ZipEntry("manifest.csv");
            zos.putNextEntry(manifestEntry);
            PrintWriter writer = new PrintWriter(new OutputStreamWriter(zos, StandardCharsets.UTF_8));
            writer.println("archiveId,archiveNo,title,fileName,status,archiveTime");
            for (ArchiveInfo a : archives) {
                writer.printf("%d,%s,%s,%s,%s,%s%n",
                        a.getId(),
                        safe(a.getArchiveNo()),
                        safe(a.getTitle()),
                        safe(a.getFileName()),
                        safe(a.getLifecycleStatus()),
                        a.getArchiveTime() == null ? "" : a.getArchiveTime());
            }
            writer.flush();
            zos.closeEntry();

            // files
            for (ArchiveInfo a : archives) {
                File f = new File(a.getFilePath());
                if (!f.exists() || !f.isFile()) {
                    continue;
                }
                zos.putNextEntry(new ZipEntry("files/" + a.getId() + "-" + sanitizeFileName(a.getFileName())));
                try (FileInputStream fis = new FileInputStream(f)) {
                    fis.transferTo(zos);
                }
                zos.closeEntry();
            }
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .body(new FileSystemResource(zipFile));
    }

    private String safe(String v) {
        return v == null ? "" : v.replace(",", " ");
    }

    private String sanitizeFileName(String fileName) {
        if (!StringUtils.hasText(fileName)) {
            return "unknown.bin";
        }
        return fileName.replaceAll("[\\\\/:*?\"<>|]", "_");
    }
}
