<template>
  <el-row :gutter="12">
    <el-col :span="8">
      <el-card>
        <template #header>档案状态分布</template>
        <div ref="statusChartRef" style="height: 280px"></div>
      </el-card>
    </el-col>
    <el-col :span="8">
      <el-card>
        <template #header>审核统计</template>
        <div ref="auditChartRef" style="height: 280px"></div>
      </el-card>
    </el-col>
    <el-col :span="8">
      <el-card>
        <template #header>指标覆盖概览</template>
        <div style="padding: 20px 0">
          <el-statistic title="档案总数" :value="indicatorStat.archiveCount || 0" />
          <el-statistic title="指标绑定关系数" :value="indicatorStat.boundRelationCount || 0" style="margin-top:16px" />
          <el-statistic title="平均每档案绑定指标数" :value="indicatorStat.avgRelationPerArchive || 0" :precision="2" style="margin-top:16px" />
        </div>
      </el-card>
    </el-col>
  </el-row>

  <el-row :gutter="12" style="margin-top: 12px">
    <el-col :span="12">
      <el-card>
        <template #header>
          课程目标达成度分析
          <span style="font-weight:400;color:#909399;font-size:13px;margin-left:8px">
            平均分 {{ objectiveAverage }} | 达成 = 支撑档案数 ≥ 1 份已归档档案
          </span>
        </template>
        <el-table :data="objectiveItems" height="320">
          <el-table-column prop="courseCode" label="课程代码" width="100" />
          <el-table-column prop="courseName" label="课程名称" width="110" />
          <el-table-column prop="objectiveCode" label="目标代码" width="100" />
          <el-table-column prop="objectiveName" label="课程目标" min-width="130" />
          <el-table-column prop="supportArchiveCount" label="支撑档案数" width="100" />
          <el-table-column label="达成度" width="170">
            <template #default="{ row }">
              <el-progress :percentage="row.attainmentScore" :stroke-width="12"
                :status="row.attainmentScore >= 80 ? 'success' : 'warning'" />
            </template>
          </el-table-column>
          <el-table-column prop="status" label="评价" width="80">
            <template #default="{ row }">
              <el-tag :type="row.status === '达成' ? 'success' : 'warning'">{{ row.status }}</el-tag>
            </template>
          </el-table-column>
        </el-table>
      </el-card>
    </el-col>
    <el-col :span="12">
      <el-card>
        <template #header>
          毕业要求达成度分析
          <span style="font-weight:400;color:#909399;font-size:13px;margin-left:8px">
            平均分 {{ requirementAverage }} | 达成 = 支撑档案数 ≥ 1 份已归档档案
          </span>
        </template>
        <el-table :data="requirementItems" height="320">
          <el-table-column prop="requirementCode" label="要求代码" width="110" />
          <el-table-column prop="requirementName" label="毕业要求" min-width="160" />
          <el-table-column prop="supportArchiveCount" label="支撑档案数" width="100" />
          <el-table-column label="达成度" width="170">
            <template #default="{ row }">
              <el-progress :percentage="row.attainmentScore" :stroke-width="12"
                :status="row.attainmentScore >= 80 ? 'success' : 'warning'" />
            </template>
          </el-table-column>
          <el-table-column prop="status" label="评价" width="80">
            <template #default="{ row }">
              <el-tag :type="row.status === '达成' ? 'success' : 'warning'">{{ row.status }}</el-tag>
            </template>
          </el-table-column>
        </el-table>
      </el-card>
    </el-col>
  </el-row>

  <el-card style="margin-top: 12px">
    <template #header>
      报表导出
      <el-button style="float:right" @click="exportAttainmentCsv">导出达成度CSV</el-button>
      <el-button style="float:right; margin-right: 8px" @click="exportCsv">导出档案CSV</el-button>
    </template>
    <el-table :data="archives" height="300">
      <el-table-column prop="archiveNo" label="档案编号" width="180" />
      <el-table-column prop="title" label="标题" />
      <el-table-column prop="lifecycleStatus" label="状态" width="140" />
      <el-table-column prop="createdAt" label="创建时间" width="180" />
    </el-table>
  </el-card>
</template>

<script setup>
// 【统计报表页面】ECharts柱状图+环形饼图+达成度进度条表格+CSV导出。

import * as echarts from "echarts";
import { nextTick, onMounted, onUnmounted, ref } from "vue";
import {
  archiveRateApi,
  auditStatApi,
  indicatorStatApi,
  courseObjectiveAttainmentApi,
  graduationRequirementAttainmentApi,
  reportArchiveListApi,
} from "../../api/modules/report";

const statusChartRef = ref(null);
const auditChartRef = ref(null);
const archives = ref([]);
const objectiveItems = ref([]);
const requirementItems = ref([]);
const objectiveAverage = ref(0);
const requirementAverage = ref(0);
const indicatorStat = ref({ archiveCount: 0, boundRelationCount: 0, avgRelationPerArchive: 0 });

let statusChart = null;
let auditChart = null;

const renderCharts = async () => {
  const archiveRate = (await archiveRateApi()).data || {};
  const auditStat = (await auditStatApi()).data || {};

  statusChart = echarts.init(statusChartRef.value);
  statusChart.setOption({
    tooltip: { trigger: "axis" },
    xAxis: { type: "category", data: ["已收集", "已提交", "已归档", "已驳回"] },
    yAxis: { type: "value", minInterval: 1 },
    series: [{
      type: "bar",
      barWidth: "50%",
      itemStyle: { borderRadius: 4 },
      data: [
        { value: archiveRate.collected || 0, itemStyle: { color: "#909399" } },
        { value: archiveRate.submitted || 0, itemStyle: { color: "#409eff" } },
        { value: archiveRate.archived || 0, itemStyle: { color: "#67c23a" } },
        { value: archiveRate.rejected || 0, itemStyle: { color: "#f56c6c" } },
      ],
    }],
  });

  auditChart = echarts.init(auditChartRef.value);
  auditChart.setOption({
    tooltip: { trigger: "item" },
    legend: { bottom: 0 },
    series: [{
      type: "pie",
      radius: ["45%", "72%"],
      center: ["50%", "45%"],
      label: { show: true, formatter: "{b}\n{d}%" },
      data: [
        { value: auditStat.pass || 0, name: "通过" },
        { value: auditStat.reject || 0, name: "驳回" },
        { value: auditStat.pending || 0, name: "待审" },
      ],
    }],
  });
};

const resizeCharts = () => {
  statusChart?.resize();
  auditChart?.resize();
};

const exportCsv = () => {
  const header = ["档案编号", "标题", "状态", "创建时间"];
  const lines = archives.value.map((x) => [x.archiveNo, x.title, x.lifecycleStatus, x.createdAt].join(","));
  const content = [header.join(","), ...lines].join("\n");
  const blob = new Blob(["﻿" + content], { type: "text/csv;charset=utf-8;" });
  const url = URL.createObjectURL(blob);
  const a = document.createElement("a");
  a.href = url;
  a.download = "archive-report.csv";
  a.click();
  URL.revokeObjectURL(url);
};

const exportAttainmentCsv = () => {
  const objHeader = ["课程代码", "课程名称", "目标代码", "课程目标", "支撑档案数", "达成度", "评价"];
  const objLines = objectiveItems.value.map((x) =>
    [x.courseCode, x.courseName, x.objectiveCode, x.objectiveName, x.supportArchiveCount, x.attainmentScore, x.status].join(",")
  );
  const reqHeader = ["毕业要求代码", "毕业要求", "支撑档案数", "达成度", "评价"];
  const reqLines = requirementItems.value.map((x) =>
    [x.requirementCode, x.requirementName, x.supportArchiveCount, x.attainmentScore, x.status].join(",")
  );
  const content = [
    "课程目标达成度分析", objHeader.join(","), ...objLines, "",
    "毕业要求达成度分析", reqHeader.join(","), ...reqLines,
  ].join("\n");
  const blob = new Blob(["﻿" + content], { type: "text/csv;charset=utf-8;" });
  const url = URL.createObjectURL(blob);
  const a = document.createElement("a");
  a.href = url;
  a.download = "attainment-report.csv";
  a.click();
  URL.revokeObjectURL(url);
};

onMounted(async () => {
  archives.value = (await reportArchiveListApi()).data || [];
  indicatorStat.value = (await indicatorStatApi()).data || {};

  const objRes = (await courseObjectiveAttainmentApi()).data || {};
  objectiveItems.value = objRes.items || [];
  objectiveAverage.value = objRes.averageScore || 0;

  const reqRes = (await graduationRequirementAttainmentApi()).data || {};
  requirementItems.value = reqRes.items || [];
  requirementAverage.value = reqRes.averageScore || 0;

  await nextTick();
  await renderCharts();
  window.addEventListener("resize", resizeCharts);
});

onUnmounted(() => {
  window.removeEventListener("resize", resizeCharts);
  statusChart?.dispose();
  auditChart?.dispose();
});
</script>
