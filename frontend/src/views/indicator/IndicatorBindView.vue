<template>
  <el-card>
    <template #header>档案-指标关联管理</template>

    <el-form inline style="margin-bottom:16px">
      <el-form-item label="选择档案">
        <el-select v-model="selectedArchiveId" placeholder="请选择档案" filterable @change="loadBindings" style="width:260px">
          <el-option v-for="a in archives" :key="a.id" :label="`[${a.archiveNo}] ${a.title}`" :value="a.id" />
        </el-select>
      </el-form-item>
    </el-form>

    <el-divider />

    <el-form v-if="selectedArchiveId" inline>
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
        <el-button type="primary" @click="submitBind">绑定</el-button>
      </el-form-item>
    </el-form>

    <el-table v-if="selectedArchiveId" :data="bindings" v-loading="bindingLoading" border style="margin-top:12px">
      <el-table-column type="index" label="#" width="50" />
      <el-table-column prop="indicatorName" label="认证指标" />
      <el-table-column prop="requirementName" label="毕业要求" />
      <el-table-column prop="objectiveName" label="课程目标" />
      <el-table-column prop="bindSource" label="来源" width="100" />
      <el-table-column label="操作" width="80">
        <template #default="{ row }">
          <el-button size="small" type="danger" @click="unbind(row.id)">解绑</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-empty v-else description="请先选择一个档案" />
  </el-card>
</template>

<script setup>
// 【关联管理页面】下拉选择档案→下拉选择指标/要求/目标→绑定。已有关联表格展示和解绑。

import { onMounted, reactive, ref } from "vue";
import { ElMessage } from "element-plus";
import { archiveBindIndicatorApi, archiveIndicatorBindingsApi, archiveIndicatorRelDeleteApi, archivePageApi } from "../../api/modules/archive";
import { indicatorListApi, objectiveListApi, requirementListApi } from "../../api/modules/indicator";

const selectedArchiveId = ref(null);
const archives = ref([]);
const indicators = ref([]);
const requirements = ref([]);
const objectives = ref([]);
const bindings = ref([]);
const bindingLoading = ref(false);

const bindForm = reactive({ indicatorId: null, requirementId: null, objectiveId: null });

const loadArchives = async () => {
  const res = await archivePageApi({ pageNo: 1, pageSize: 500 });
  archives.value = (res.data?.records || []).map((a) => ({ id: a.id, archiveNo: a.archiveNo, title: a.title }));
};

const loadRefs = async () => {
  indicators.value = (await indicatorListApi()).data || [];
  requirements.value = (await requirementListApi()).data || [];
  objectives.value = (await objectiveListApi()).data || [];
};

const loadBindings = async () => {
  if (!selectedArchiveId.value) { bindings.value = []; return; }
  bindingLoading.value = true;
  try {
    bindings.value = (await archiveIndicatorBindingsApi(selectedArchiveId.value)).data || [];
  } finally {
    bindingLoading.value = false;
  }
};

const submitBind = async () => {
  if (!bindForm.indicatorId) return ElMessage.warning("请选择认证指标");
  await archiveBindIndicatorApi(selectedArchiveId.value, {
    indicatorId: bindForm.indicatorId,
    requirementId: bindForm.requirementId,
    objectiveId: bindForm.objectiveId,
    bindSource: "MANUAL",
  });
  ElMessage.success("绑定成功");
  bindForm.indicatorId = null;
  bindForm.requirementId = null;
  bindForm.objectiveId = null;
  await loadBindings();
};

const unbind = async (relId) => {
  await archiveIndicatorRelDeleteApi(selectedArchiveId.value, relId);
  ElMessage.success("已解绑");
  await loadBindings();
};

onMounted(() => {
  loadArchives();
  loadRefs();
});
</script>
