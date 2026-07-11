package com.edu.archive.controller;


// 【统计报表控制器】归档率统计、审核统计、课程目标/毕业要求达成度计算（归一化算法）。
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.edu.archive.common.ApiResponse;
import com.edu.archive.entity.ArchiveAuditFlow;
import com.edu.archive.entity.ArchiveInfo;
import com.edu.archive.entity.ArchiveIndicatorRel;
import com.edu.archive.entity.CourseObjective;
import com.edu.archive.entity.GraduationRequirement;
import com.edu.archive.mapper.ArchiveIndicatorRelMapper;
import com.edu.archive.mapper.ArchiveAuditFlowMapper;
import com.edu.archive.mapper.ArchiveInfoMapper;
import com.edu.archive.mapper.CourseObjectiveMapper;
import com.edu.archive.mapper.GraduationRequirementMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/reports")
public class ReportController {
    private final ArchiveInfoMapper archiveInfoMapper;
    private final ArchiveAuditFlowMapper archiveAuditFlowMapper;
    private final ArchiveIndicatorRelMapper archiveIndicatorRelMapper;
    private final CourseObjectiveMapper courseObjectiveMapper;
    private final GraduationRequirementMapper graduationRequirementMapper;

    @Autowired
    public ReportController(ArchiveInfoMapper archiveInfoMapper,
                            ArchiveAuditFlowMapper archiveAuditFlowMapper,
                            ArchiveIndicatorRelMapper archiveIndicatorRelMapper,
                            CourseObjectiveMapper courseObjectiveMapper,
                            GraduationRequirementMapper graduationRequirementMapper) {
        this.archiveInfoMapper = archiveInfoMapper;
        this.archiveAuditFlowMapper = archiveAuditFlowMapper;
        this.archiveIndicatorRelMapper = archiveIndicatorRelMapper;
        this.courseObjectiveMapper = courseObjectiveMapper;
        this.graduationRequirementMapper = graduationRequirementMapper;
    }

    @GetMapping("/archive-list")
    public ApiResponse<?> archiveList() {
        List<ArchiveInfo> list = archiveInfoMapper.selectList(
                new LambdaQueryWrapper<ArchiveInfo>().orderByDesc(ArchiveInfo::getCreatedAt)
        );
        return ApiResponse.ok(list);
    }

    @GetMapping("/archive-rate")
    public ApiResponse<?> archiveRate() {
        long total = archiveInfoMapper.selectCount(null);
        long collected = archiveInfoMapper.selectCount(new LambdaQueryWrapper<ArchiveInfo>().eq(ArchiveInfo::getLifecycleStatus, "COLLECTED"));
        long submitted = archiveInfoMapper.selectCount(new LambdaQueryWrapper<ArchiveInfo>().eq(ArchiveInfo::getLifecycleStatus, "SUBMITTED"));
        long archived = archiveInfoMapper.selectCount(new LambdaQueryWrapper<ArchiveInfo>().eq(ArchiveInfo::getLifecycleStatus, "APPROVED"));
        long rejected = archiveInfoMapper.selectCount(new LambdaQueryWrapper<ArchiveInfo>().eq(ArchiveInfo::getLifecycleStatus, "REJECTED"));
        Map<String, Object> map = new HashMap<>();
        map.put("total", total);
        map.put("collected", collected);
        map.put("submitted", submitted);
        map.put("archived", archived);
        map.put("rejected", rejected);
        map.put("rate", total == 0 ? 0 : (archived * 100.0 / total));
        return ApiResponse.ok(map);
    }

    @GetMapping("/audit-stat")
    public ApiResponse<?> auditStat() {
        List<ArchiveAuditFlow> list = archiveAuditFlowMapper.selectList(
                new LambdaQueryWrapper<ArchiveAuditFlow>().eq(ArchiveAuditFlow::getAuditType, "ARCHIVE_AUDIT")
        );
        long pass = list.stream().filter(a -> "PASSED".equalsIgnoreCase(a.getAuditStatus())).count();
        long reject = list.stream().filter(a -> "REJECTED".equalsIgnoreCase(a.getAuditStatus())).count();
        long pending = list.stream().filter(a -> "PENDING".equalsIgnoreCase(a.getAuditStatus())).count();
        Map<String, Object> map = new HashMap<>();
        map.put("pass", pass);
        map.put("reject", reject);
        map.put("pending", pending);
        map.put("total", list.size());
        return ApiResponse.ok(map);
    }

    @GetMapping("/indicator-stat")
    public ApiResponse<?> indicatorStat() {
        long boundCount = archiveIndicatorRelMapper.selectCount(null);
        long archiveCount = archiveInfoMapper.selectCount(null);
        Map<String, Object> map = new HashMap<>();
        map.put("archiveCount", archiveCount);
        map.put("boundRelationCount", boundCount);
        map.put("avgRelationPerArchive", archiveCount == 0 ? 0 : (boundCount * 1.0 / archiveCount));
        return ApiResponse.ok(map);
    }

    @GetMapping("/course-objective-attainment")
    public ApiResponse<?> courseObjectiveAttainment() {
        Set<Long> approvedArchiveIds = approvedArchiveIds();
        List<CourseObjective> objectives = courseObjectiveMapper.selectList(
                new LambdaQueryWrapper<CourseObjective>()
                        .eq(CourseObjective::getStatus, 1)
                        .orderByAsc(CourseObjective::getCourseCode, CourseObjective::getObjectiveCode)
        );
        if (objectives.isEmpty()) {
            return ApiResponse.ok(Map.of("items", List.of(), "averageScore", 0, "evaluatedCount", 0));
        }

        Map<Long, Long> objectiveCountMap = relationCountByKey(approvedArchiveIds, true);
        long maxCount = objectiveCountMap.values().stream().max(Long::compareTo).orElse(0L);
        if (maxCount == 0) {
            maxCount = 1;
        }

        List<Map<String, Object>> items = new ArrayList<>();
        double scoreSum = 0;
        for (CourseObjective objective : objectives) {
            long supportCount = objectiveCountMap.getOrDefault(objective.getId(), 0L);
            double score = supportCount * 100.0 / maxCount;
            score = round2(score);
            scoreSum += score;
            Map<String, Object> row = new HashMap<>();
            row.put("objectiveId", objective.getId());
            row.put("courseCode", objective.getCourseCode());
            row.put("courseName", objective.getCourseName());
            row.put("objectiveCode", objective.getObjectiveCode());
            row.put("objectiveName", objective.getObjectiveName());
            row.put("supportArchiveCount", supportCount);
            row.put("attainmentScore", score);
            row.put("status", score >= 80 ? "达成" : "待改进");
            items.add(row);
        }

        Map<String, Object> res = new HashMap<>();
        res.put("items", items);
        res.put("averageScore", round2(scoreSum / objectives.size()));
        res.put("evaluatedCount", objectives.size());
        return ApiResponse.ok(res);
    }

    @GetMapping("/graduation-requirement-attainment")
    public ApiResponse<?> graduationRequirementAttainment() {
        Set<Long> approvedArchiveIds = approvedArchiveIds();
        List<GraduationRequirement> requirements = graduationRequirementMapper.selectList(
                new LambdaQueryWrapper<GraduationRequirement>()
                        .eq(GraduationRequirement::getStatus, 1)
                        .orderByAsc(GraduationRequirement::getRequirementCode)
        );
        if (requirements.isEmpty()) {
            return ApiResponse.ok(Map.of("items", List.of(), "averageScore", 0, "evaluatedCount", 0));
        }

        Map<Long, Long> requirementCountMap = relationCountByKey(approvedArchiveIds, false);
        long maxCount = requirementCountMap.values().stream().max(Long::compareTo).orElse(0L);
        if (maxCount == 0) {
            maxCount = 1;
        }

        List<Map<String, Object>> items = new ArrayList<>();
        double scoreSum = 0;
        for (GraduationRequirement requirement : requirements) {
            long supportCount = requirementCountMap.getOrDefault(requirement.getId(), 0L);
            double score = supportCount * 100.0 / maxCount;
            score = round2(score);
            scoreSum += score;
            Map<String, Object> row = new HashMap<>();
            row.put("requirementId", requirement.getId());
            row.put("requirementCode", requirement.getRequirementCode());
            row.put("requirementName", requirement.getRequirementName());
            row.put("supportArchiveCount", supportCount);
            row.put("attainmentScore", score);
            row.put("status", score >= 80 ? "达成" : "待改进");
            items.add(row);
        }

        Map<String, Object> res = new HashMap<>();
        res.put("items", items);
        res.put("averageScore", round2(scoreSum / requirements.size()));
        res.put("evaluatedCount", requirements.size());
        return ApiResponse.ok(res);
    }

    private Set<Long> approvedArchiveIds() {
        List<ArchiveInfo> approvedArchives = archiveInfoMapper.selectList(
                new LambdaQueryWrapper<ArchiveInfo>()
                        .select(ArchiveInfo::getId)
                        .eq(ArchiveInfo::getLifecycleStatus, "APPROVED")
        );
        return approvedArchives.stream().map(ArchiveInfo::getId).collect(Collectors.toSet());
    }

    private Map<Long, Long> relationCountByKey(Set<Long> approvedArchiveIds, boolean byObjective) {
        if (approvedArchiveIds.isEmpty()) {
            return Map.of();
        }
        LambdaQueryWrapper<ArchiveIndicatorRel> query = new LambdaQueryWrapper<ArchiveIndicatorRel>()
                .in(ArchiveIndicatorRel::getArchiveId, approvedArchiveIds);
        if (byObjective) {
            query.isNotNull(ArchiveIndicatorRel::getObjectiveId);
        } else {
            query.isNotNull(ArchiveIndicatorRel::getRequirementId);
        }
        List<ArchiveIndicatorRel> rels = archiveIndicatorRelMapper.selectList(query);

        Map<Long, Set<Long>> keyArchiveSet = new HashMap<>();
        for (ArchiveIndicatorRel rel : rels) {
            Long keyId = byObjective ? rel.getObjectiveId() : rel.getRequirementId();
            if (keyId == null) {
                continue;
            }
            keyArchiveSet.computeIfAbsent(keyId, k -> new HashSet<>()).add(rel.getArchiveId());
        }

        Map<Long, Long> countMap = new HashMap<>();
        for (Map.Entry<Long, Set<Long>> entry : keyArchiveSet.entrySet()) {
            countMap.put(entry.getKey(), (long) entry.getValue().size());
        }
        return countMap;
    }

    private double round2(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}
