<template>
  <el-card v-loading="loading">
    <template #header>
      <div class="card-header">
        <span>档案详情</span>
        <el-button @click="$router.back()">← 返回</el-button>
      </div>
    </template>
    <el-descriptions :column="2" border v-if="detail">
      <el-descriptions-item label="档案编号">{{ detail.archiveNo }}</el-descriptions-item>
      <el-descriptions-item label="标题">{{ detail.title }}</el-descriptions-item>
      <el-descriptions-item label="分类ID">{{ detail.categoryId }}</el-descriptions-item>
      <el-descriptions-item label="状态">{{ detail.lifecycleStatus }}</el-descriptions-item>
      <el-descriptions-item label="文件名">{{ detail.fileName }}</el-descriptions-item>
      <el-descriptions-item label="版本">{{ detail.versionNo }}</el-descriptions-item>
      <el-descriptions-item label="备注" :span="2">{{ detail.remark }}</el-descriptions-item>
    </el-descriptions>
    <el-divider />
    <el-form :model="edit" inline>
      <el-form-item label="标题"><el-input v-model="edit.title" /></el-form-item>
      <el-form-item label="分类ID"><el-input v-model.number="edit.categoryId" /></el-form-item>
      <el-form-item label="备注"><el-input v-model="edit.remark" /></el-form-item>
      <el-form-item><el-button type="primary" @click="save">保存</el-button></el-form-item>
      <el-form-item><el-button @click="submitAudit">提交审核</el-button></el-form-item>
    </el-form>

    <el-divider />
    <h4 style="margin-bottom:12px">指标关联</h4>
    <el-form inline style="margin-bottom:12px">
      <el-form-item label="认证指标">
        <el-select v-model="bindForm.indicatorId" placeholder="请选择" filterable style="width:200px">
          <el-option v-for="i in indicators" :key="i.id" :label="`${i.indicatorCode} ${i.indicatorName}`" :value="i.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="毕业要求">
        <el-select v-model="bindForm.requirementId" placeholder="可选" filterable clearable style="width:200px">
          <el-option v-for="r in requirements" :key="r.id" :label="`${r.requirementCode} ${r.requirementName}`" :value="r.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="课程目标">
        <el-select v-model="bindForm.objectiveId" placeholder="可选" filterable clearable style="width:200px">
          <el-option v-for="o in objectives" :key="o.id" :label="`${o.courseCode}-${o.objectiveCode}`" :value="o.id" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="addBinding">新增关联</el-button>
      </el-form-item>
    </el-form>

    <el-table :data="bindings" border>
      <el-table-column prop="indicatorName" label="认证指标" />
      <el-table-column prop="requirementName" label="毕业要求" />
      <el-table-column prop="objectiveName" label="课程目标" />
      <el-table-column prop="bindSource" label="来源" width="100" />
      <el-table-column label="操作" width="80">
        <template #default="{ row }">
          <el-button size="small" type="danger" @click="removeBinding(row.id)">解绑</el-button>
        </template>
      </el-table-column>
    </el-table>
  </el-card>
</template>

<script setup>
// 【档案详情页面】编辑元数据、内联指标关联(下拉绑定/解绑)、提交审核。

import { onMounted, reactive, ref } from "vue";
import { useRoute } from "vue-router";
import { ElMessage } from "element-plus";
import {
  archiveDetailApi, archiveUpdateApi, archiveSubmitAuditApi,
  archiveBindIndicatorApi, archiveIndicatorBindingsApi, archiveIndicatorRelDeleteApi
} from "../../api/modules/archive";
import { indicatorListApi, objectiveListApi, requirementListApi } from "../../api/modules/indicator";

const route = useRoute();
const loading = ref(false);
const detail = ref(null);
const edit = reactive({ title: "", categoryId: null, remark: "" });
const bindings = ref([]);
const indicators = ref([]);
const requirements = ref([]);
const objectives = ref([]);
const bindForm = reactive({ indicatorId: null, requirementId: null, objectiveId: null });

const loadDetail = async () => {
  loading.value = true;
  try {
    const res = await archiveDetailApi(route.params.id);
    detail.value = res.data;
    edit.title = res.data.title;
    edit.categoryId = res.data.categoryId;
    edit.remark = res.data.remark;
  } finally {
    loading.value = false;
  }
};

const loadBindings = async () => {
  bindings.value = (await archiveIndicatorBindingsApi(route.params.id)).data || [];
};

const loadRefs = async () => {
  indicators.value = (await indicatorListApi()).data || [];
  requirements.value = (await requirementListApi()).data || [];
  objectives.value = (await objectiveListApi()).data || [];
};

const addBinding = async () => {
  if (!bindForm.indicatorId) return ElMessage.warning("请选择认证指标");
  await archiveBindIndicatorApi(route.params.id, {
    indicatorId: bindForm.indicatorId,
    requirementId: bindForm.requirementId,
    objectiveId: bindForm.objectiveId,
    bindSource: "MANUAL",
  });
  ElMessage.success("关联成功");
  bindForm.indicatorId = null;
  bindForm.requirementId = null;
  bindForm.objectiveId = null;
  await loadBindings();
};

const removeBinding = async (relId) => {
  await archiveIndicatorRelDeleteApi(route.params.id, relId);
  ElMessage.success("已解绑");
  await loadBindings();
};

const save = async () => {
  await archiveUpdateApi(route.params.id, edit);
  ElMessage.success("更新成功");
  await loadDetail();
};

const submitAudit = async () => {
  await archiveSubmitAuditApi(route.params.id);
  ElMessage.success("已提交审核");
  await loadDetail();
};

onMounted(() => {
  loadDetail();
  loadBindings();
  loadRefs();
});
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
