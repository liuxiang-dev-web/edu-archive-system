<template>
  <el-row :gutter="12">
    <el-col :span="12">
      <el-card>
        <template #header>1) 档案绑定认证要素</template>
        <el-form label-width="100px">
          <el-form-item label="选择档案">
            <el-select v-model="bindForm.archiveId" placeholder="请选择档案" filterable style="width:100%">
              <el-option v-for="a in archives" :key="a.id" :label="`[${a.archiveNo}] ${a.title}`" :value="a.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="认证指标">
            <el-select v-model="bindForm.indicatorId" placeholder="请选择" filterable style="width:100%">
              <el-option v-for="i in indicators" :key="i.id" :label="`${i.indicatorCode} ${i.indicatorName}`" :value="i.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="毕业要求">
            <el-select v-model="bindForm.requirementId" placeholder="可选" filterable clearable style="width:100%">
              <el-option v-for="r in requirements" :key="r.id" :label="`${r.requirementCode} ${r.requirementName}`" :value="r.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="课程目标">
            <el-select v-model="bindForm.objectiveId" placeholder="可选" filterable clearable style="width:100%">
              <el-option v-for="o in objectives" :key="o.id" :label="`${o.courseCode}-${o.objectiveCode} ${o.objectiveName}`" :value="o.id" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="bindNow">绑定</el-button>
          </el-form-item>
        </el-form>
      </el-card>
    </el-col>
    <el-col :span="12">
      <el-card>
        <template #header>2) 自动筛选佐证材料</template>
        <el-form inline>
          <el-form-item label="认证指标">
            <el-select v-model="filterForm.indicatorId" placeholder="全部" filterable clearable style="width:180px">
              <el-option v-for="i in indicators" :key="i.id" :label="i.indicatorCode" :value="i.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="毕业要求">
            <el-select v-model="filterForm.requirementId" placeholder="全部" filterable clearable style="width:180px">
              <el-option v-for="r in requirements" :key="r.id" :label="r.requirementCode" :value="r.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="课程目标">
            <el-select v-model="filterForm.objectiveId" placeholder="全部" filterable clearable style="width:180px">
              <el-option v-for="o in objectives" :key="o.id" :label="o.objectiveCode" :value="o.id" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="doFilter">筛选</el-button>
            <el-button @click="oneClickExport">一键导出ZIP</el-button>
          </el-form-item>
        </el-form>
        <el-table :data="filteredArchives" height="280">
          <el-table-column type="index" label="#" width="50" />
          <el-table-column prop="archiveNo" label="档案编号" width="170" />
          <el-table-column prop="title" label="标题" />
          <el-table-column prop="lifecycleStatus" label="状态" width="100" />
        </el-table>
      </el-card>
    </el-col>
  </el-row>

  <el-card style="margin-top: 12px">
    <template #header>3) 课程目标达成度关联档案清单</template>
    <el-form inline>
      <el-form-item label="课程目标">
        <el-select v-model="objectiveId" placeholder="请选择课程目标" filterable clearable style="width:320px">
          <el-option v-for="o in objectives" :key="o.id" :label="`${o.courseCode}-${o.objectiveCode} ${o.objectiveName}`" :value="o.id" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="loadObjectiveList">生成清单</el-button>
      </el-form-item>
    </el-form>
    <el-descriptions border :column="4" v-if="objectiveResult">
      <el-descriptions-item label="目标ID">{{ objectiveResult.objectiveId }}</el-descriptions-item>
      <el-descriptions-item label="关联档案数">{{ objectiveResult.archiveCount }}</el-descriptions-item>
      <el-descriptions-item label="已归档数">{{ objectiveResult.approvedCount }}</el-descriptions-item>
      <el-descriptions-item label="达成率">{{ Number(objectiveResult.achievementArchiveRate || 0).toFixed(2) }}%</el-descriptions-item>
    </el-descriptions>
    <el-table :data="objectiveResult?.archives || []" style="margin-top: 10px">
      <el-table-column prop="archiveNo" label="档案编号" width="180" />
      <el-table-column prop="title" label="标题" />
      <el-table-column prop="archiveTime" label="归档时间" width="180" />
      <el-table-column prop="lifecycleStatus" label="状态" width="120" />
    </el-table>
  </el-card>
</template>

<script setup>
// 【认证特色功能页面】绑定认证要素、自动筛选佐证材料、一键导出ZIP、课程达成度清单。

import { onMounted, reactive, ref } from "vue";
import { ElMessage } from "element-plus";
import {
  certificationAutoFilterApi,
  certificationBindApi,
  certificationObjectiveListApi,
} from "../../api/modules/certification";
import { archivePageApi } from "../../api/modules/archive";
import { indicatorListApi, objectiveListApi, requirementListApi } from "../../api/modules/indicator";
import { getToken } from "../../utils/auth";

const archives = ref([]);
const indicators = ref([]);
const requirements = ref([]);
const objectives = ref([]);

const bindForm = reactive({
  archiveId: null,
  indicatorId: null,
  requirementId: null,
  objectiveId: null,
});

const filterForm = reactive({
  indicatorId: null,
  requirementId: null,
  objectiveId: null,
  approvedOnly: true,
});

const filteredArchives = ref([]);
const objectiveId = ref(null);
const objectiveResult = ref(null);

const loadRefs = async () => {
  const [aRes, iRes, rRes, oRes] = await Promise.all([
    archivePageApi({ pageNo: 1, pageSize: 500 }),
    indicatorListApi(),
    requirementListApi(),
    objectiveListApi(),
  ]);
  archives.value = (aRes.data?.records || []).map((a) => ({ id: a.id, archiveNo: a.archiveNo, title: a.title }));
  indicators.value = iRes.data || [];
  requirements.value = rRes.data || [];
  objectives.value = oRes.data || [];
};

const bindNow = async () => {
  if (!bindForm.archiveId || !bindForm.indicatorId) return ElMessage.warning("请选择档案和认证指标");
  await certificationBindApi({ ...bindForm, bindSource: "MANUAL" });
  ElMessage.success("绑定成功");
  bindForm.archiveId = null;
  bindForm.indicatorId = null;
  bindForm.requirementId = null;
  bindForm.objectiveId = null;
};

const doFilter = async () => {
  const res = await certificationAutoFilterApi(filterForm);
  filteredArchives.value = res.data || [];
};

const loadObjectiveList = async () => {
  if (!objectiveId.value) return ElMessage.warning("请选择课程目标");
  const res = await certificationObjectiveListApi(objectiveId.value);
  objectiveResult.value = res.data;
};

const oneClickExport = async () => {
  const params = new URLSearchParams();
  Object.entries(filterForm).forEach(([k, v]) => {
    if (k === "approvedOnly" || v === null || v === undefined || String(v) === "") return;
    params.append(k, v);
  });
  const token = getToken();
  const res = await fetch(`/api/certification/export?${params.toString()}`, {
    headers: token ? { Authorization: `Bearer ${token}` } : {},
  });
  if (!res.ok) { ElMessage.error("导出失败"); return; }
  const blob = await res.blob();
  const disposition = res.headers.get("Content-Disposition") || "";
  const match = disposition.match(/filename\*?=(?:UTF-8''|")([^";]*)/) || disposition.match(/filename="?([^";\n]*)"?/);
  const filename = match ? decodeURIComponent(match[1]) : "cert-materials.zip";
  const url = URL.createObjectURL(blob);
  const a = document.createElement("a");
  a.href = url;
  a.download = filename;
  document.body.appendChild(a);
  a.click();
  document.body.removeChild(a);
  URL.revokeObjectURL(url);
};

onMounted(loadRefs);
</script>
