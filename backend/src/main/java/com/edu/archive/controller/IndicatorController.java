package com.edu.archive.controller;


// 【认证指标控制器】指标/毕业要求/课程目标三要素的完整CRUD，关联绑定与解绑。
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.edu.archive.common.ApiResponse;
import com.edu.archive.entity.*;
import com.edu.archive.mapper.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/indicators")
public class IndicatorController {
    private final EduIndicatorMapper eduIndicatorMapper;
    private final GraduationRequirementMapper graduationRequirementMapper;
    private final CourseObjectiveMapper courseObjectiveMapper;
    private final IndicatorRequirementRelMapper indicatorRequirementRelMapper;
    private final CourseObjectiveRequirementRelMapper courseObjectiveRequirementRelMapper;
    private final ArchiveIndicatorRelMapper archiveIndicatorRelMapper;

    @Autowired
    public IndicatorController(EduIndicatorMapper eduIndicatorMapper,
                               GraduationRequirementMapper graduationRequirementMapper,
                               CourseObjectiveMapper courseObjectiveMapper,
                               IndicatorRequirementRelMapper indicatorRequirementRelMapper,
                               CourseObjectiveRequirementRelMapper courseObjectiveRequirementRelMapper,
                               ArchiveIndicatorRelMapper archiveIndicatorRelMapper) {
        this.eduIndicatorMapper = eduIndicatorMapper;
        this.graduationRequirementMapper = graduationRequirementMapper;
        this.courseObjectiveMapper = courseObjectiveMapper;
        this.indicatorRequirementRelMapper = indicatorRequirementRelMapper;
        this.courseObjectiveRequirementRelMapper = courseObjectiveRequirementRelMapper;
        this.archiveIndicatorRelMapper = archiveIndicatorRelMapper;
    }

    @GetMapping("/list")
    public ApiResponse<?> indicatorList() {
        return ApiResponse.ok(eduIndicatorMapper.selectList(null));
    }

    @PostMapping("/create")
    public ApiResponse<?> createIndicator(@RequestBody EduIndicator indicator) {
        if (existsIndicatorCode(indicator.getIndicatorCode(), null)) {
            return ApiResponse.fail(400, "指标编码已存在");
        }
        eduIndicatorMapper.insert(indicator);
        return ApiResponse.ok("创建成功", indicator);
    }

    @PutMapping("/{id}")
    public ApiResponse<?> updateIndicator(@PathVariable Long id, @RequestBody EduIndicator indicator) {
        if (existsIndicatorCode(indicator.getIndicatorCode(), id)) {
            return ApiResponse.fail(400, "指标编码已存在");
        }
        indicator.setId(id);
        eduIndicatorMapper.updateById(indicator);
        return ApiResponse.ok("更新成功", indicator);
    }

    @GetMapping("/requirements")
    public ApiResponse<?> requirementList() {
        return ApiResponse.ok(graduationRequirementMapper.selectList(null));
    }

    @PostMapping("/requirements")
    public ApiResponse<?> createRequirement(@RequestBody GraduationRequirement req) {
        if (existsRequirementCode(req.getRequirementCode(), null)) {
            return ApiResponse.fail(400, "毕业要求编码已存在");
        }
        graduationRequirementMapper.insert(req);
        return ApiResponse.ok("创建成功", req);
    }

    @PutMapping("/requirements/{id}")
    public ApiResponse<?> updateRequirement(@PathVariable Long id, @RequestBody GraduationRequirement req) {
        if (existsRequirementCode(req.getRequirementCode(), id)) {
            return ApiResponse.fail(400, "毕业要求编码已存在");
        }
        req.setId(id);
        graduationRequirementMapper.updateById(req);
        return ApiResponse.ok("更新成功", req);
    }

    @GetMapping("/objectives")
    public ApiResponse<?> objectiveList() {
        return ApiResponse.ok(courseObjectiveMapper.selectList(null));
    }

    @PostMapping("/objectives")
    public ApiResponse<?> createObjective(@RequestBody CourseObjective obj) {
        if (existsObjectiveCode(obj.getCourseCode(), obj.getObjectiveCode(), null)) {
            return ApiResponse.fail(400, "该课程下目标编码已存在");
        }
        courseObjectiveMapper.insert(obj);
        return ApiResponse.ok("创建成功", obj);
    }

    @PutMapping("/objectives/{id}")
    public ApiResponse<?> updateObjective(@PathVariable Long id, @RequestBody CourseObjective obj) {
        if (existsObjectiveCode(obj.getCourseCode(), obj.getObjectiveCode(), id)) {
            return ApiResponse.fail(400, "该课程下目标编码已存在");
        }
        obj.setId(id);
        courseObjectiveMapper.updateById(obj);
        return ApiResponse.ok("更新成功", obj);
    }

    @GetMapping("/relations")
    public ApiResponse<?> relations() {
        Map<String, Object> result = new HashMap<>();
        result.put("indicators", eduIndicatorMapper.selectList(null));
        result.put("requirements", graduationRequirementMapper.selectList(null));
        result.put("objectives", courseObjectiveMapper.selectList(null));

        List<IndicatorRequirementRel> irRels = indicatorRequirementRelMapper.selectList(null);
        result.put("indicatorRequirementRels", irRels);

        List<CourseObjectiveRequirementRel> corRels = courseObjectiveRequirementRelMapper.selectList(null);
        result.put("courseObjectiveRequirementRels", corRels);

        return ApiResponse.ok(result);
    }

    @PostMapping("/{indicatorId}/bind-requirement/{requirementId}")
    public ApiResponse<?> bindRequirement(@PathVariable Long indicatorId, @PathVariable Long requirementId) {
        if (indicatorRequirementRelMapper.selectOne(
                new LambdaQueryWrapper<IndicatorRequirementRel>()
                        .eq(IndicatorRequirementRel::getIndicatorId, indicatorId)
                        .eq(IndicatorRequirementRel::getRequirementId, requirementId)) != null) {
            return ApiResponse.fail(400, "已存在该关联");
        }
        IndicatorRequirementRel rel = new IndicatorRequirementRel();
        rel.setIndicatorId(indicatorId);
        rel.setRequirementId(requirementId);
        indicatorRequirementRelMapper.insert(rel);
        return ApiResponse.ok("绑定成功", null);
    }

    @DeleteMapping("/{indicatorId}/unbind-requirement/{requirementId}")
    public ApiResponse<?> unbindRequirement(@PathVariable Long indicatorId, @PathVariable Long requirementId) {
        indicatorRequirementRelMapper.delete(
                new LambdaQueryWrapper<IndicatorRequirementRel>()
                        .eq(IndicatorRequirementRel::getIndicatorId, indicatorId)
                        .eq(IndicatorRequirementRel::getRequirementId, requirementId));
        return ApiResponse.ok("解绑成功", null);
    }

    @PostMapping("/requirements/{requirementId}/bind-objective/{objectiveId}")
    public ApiResponse<?> bindObjective(@PathVariable Long requirementId, @PathVariable Long objectiveId) {
        if (courseObjectiveRequirementRelMapper.selectOne(
                new LambdaQueryWrapper<CourseObjectiveRequirementRel>()
                        .eq(CourseObjectiveRequirementRel::getRequirementId, requirementId)
                        .eq(CourseObjectiveRequirementRel::getObjectiveId, objectiveId)) != null) {
            return ApiResponse.fail(400, "已存在该关联");
        }
        CourseObjectiveRequirementRel rel = new CourseObjectiveRequirementRel();
        rel.setRequirementId(requirementId);
        rel.setObjectiveId(objectiveId);
        courseObjectiveRequirementRelMapper.insert(rel);
        return ApiResponse.ok("绑定成功", null);
    }

    @DeleteMapping("/requirements/{requirementId}/unbind-objective/{objectiveId}")
    public ApiResponse<?> unbindObjective(@PathVariable Long requirementId, @PathVariable Long objectiveId) {
        courseObjectiveRequirementRelMapper.delete(
                new LambdaQueryWrapper<CourseObjectiveRequirementRel>()
                        .eq(CourseObjectiveRequirementRel::getRequirementId, requirementId)
                        .eq(CourseObjectiveRequirementRel::getObjectiveId, objectiveId));
        return ApiResponse.ok("解绑成功", null);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<?> deleteIndicator(@PathVariable Long id) {
        indicatorRequirementRelMapper.delete(
                new LambdaQueryWrapper<IndicatorRequirementRel>().eq(IndicatorRequirementRel::getIndicatorId, id));
        archiveIndicatorRelMapper.delete(
                new LambdaQueryWrapper<ArchiveIndicatorRel>().eq(ArchiveIndicatorRel::getIndicatorId, id));
        eduIndicatorMapper.deleteById(id);
        return ApiResponse.ok("删除成功", null);
    }

    @DeleteMapping("/requirements/{id}")
    public ApiResponse<?> deleteRequirement(@PathVariable Long id) {
        indicatorRequirementRelMapper.delete(
                new LambdaQueryWrapper<IndicatorRequirementRel>().eq(IndicatorRequirementRel::getRequirementId, id));
        courseObjectiveRequirementRelMapper.delete(
                new LambdaQueryWrapper<CourseObjectiveRequirementRel>().eq(CourseObjectiveRequirementRel::getRequirementId, id));
        archiveIndicatorRelMapper.delete(
                new LambdaQueryWrapper<ArchiveIndicatorRel>().eq(ArchiveIndicatorRel::getRequirementId, id));
        graduationRequirementMapper.deleteById(id);
        return ApiResponse.ok("删除成功", null);
    }

    @DeleteMapping("/objectives/{id}")
    public ApiResponse<?> deleteObjective(@PathVariable Long id) {
        courseObjectiveRequirementRelMapper.delete(
                new LambdaQueryWrapper<CourseObjectiveRequirementRel>().eq(CourseObjectiveRequirementRel::getObjectiveId, id));
        archiveIndicatorRelMapper.delete(
                new LambdaQueryWrapper<ArchiveIndicatorRel>().eq(ArchiveIndicatorRel::getObjectiveId, id));
        courseObjectiveMapper.deleteById(id);
        return ApiResponse.ok("删除成功", null);
    }

    private boolean existsIndicatorCode(String code, Long excludeId) {
        LambdaQueryWrapper<EduIndicator> qw = new LambdaQueryWrapper<EduIndicator>()
                .eq(EduIndicator::getIndicatorCode, code);
        if (excludeId != null) qw.ne(EduIndicator::getId, excludeId);
        return eduIndicatorMapper.selectCount(qw) > 0;
    }

    private boolean existsRequirementCode(String code, Long excludeId) {
        LambdaQueryWrapper<GraduationRequirement> qw = new LambdaQueryWrapper<GraduationRequirement>()
                .eq(GraduationRequirement::getRequirementCode, code);
        if (excludeId != null) qw.ne(GraduationRequirement::getId, excludeId);
        return graduationRequirementMapper.selectCount(qw) > 0;
    }

    private boolean existsObjectiveCode(String courseCode, String objectiveCode, Long excludeId) {
        LambdaQueryWrapper<CourseObjective> qw = new LambdaQueryWrapper<CourseObjective>()
                .eq(CourseObjective::getCourseCode, courseCode)
                .eq(CourseObjective::getObjectiveCode, objectiveCode);
        if (excludeId != null) qw.ne(CourseObjective::getId, excludeId);
        return courseObjectiveMapper.selectCount(qw) > 0;
    }
}
