<template>
  <el-card>
    <template #header>认证指标管理</template>
    <el-tabs v-model="activeTab" @tab-change="loadAll">
      <el-tab-pane label="认证指标" name="indicators">
        <el-button type="primary" @click="showAddIndicator" style="margin-bottom:12px">新增指标</el-button>
        <el-table :data="indicators" v-loading="loading" border>
          <el-table-column type="index" label="#" width="50" />
          <el-table-column prop="indicatorCode" label="编码" width="120" />
          <el-table-column prop="indicatorName" label="名称" />
          <el-table-column label="关联毕业要求" min-width="260">
            <template #default="{ row }">
              <el-tag v-for="r in linkedReqs(row.id)" :key="r.id" closable size="small"
                @close="unbindReq(row.id, r.id)" style="margin:2px 4px">
                {{ r.requirementCode }} {{ r.requirementName }}
              </el-tag>
              <el-button size="small" link type="primary" @click="showBindReq(row)">+ 关联</el-button>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="160">
            <template #default="{ row }">
              <div style="display:flex;gap:6px">
                <el-button size="small" @click="editIndicator(row)">编辑</el-button>
                <el-button size="small" type="danger" @click="removeIndicator(row.id)">删除</el-button>
              </div>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="毕业要求" name="requirements">
        <el-button type="primary" @click="showAddRequirement" style="margin-bottom:12px">新增毕业要求</el-button>
        <el-table :data="requirements" v-loading="loading" border>
          <el-table-column type="index" label="#" width="50" />
          <el-table-column prop="requirementCode" label="编码" width="120" />
          <el-table-column prop="requirementName" label="名称" />
          <el-table-column label="关联课程目标" min-width="260">
            <template #default="{ row }">
              <el-tag v-for="o in linkedObjs(row.id)" :key="o.id" closable size="small"
                @close="unbindObj(row.id, o.id)" style="margin:2px 4px">
                {{ o.courseCode }}-{{ o.objectiveCode }} {{ o.objectiveName }}
              </el-tag>
              <el-button size="small" link type="primary" @click="showBindObj(row)">+ 关联</el-button>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="160">
            <template #default="{ row }">
              <div style="display:flex;gap:6px">
                <el-button size="small" @click="editRequirement(row)">编辑</el-button>
                <el-button size="small" type="danger" @click="removeRequirement(row.id)">删除</el-button>
              </div>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="课程目标" name="objectives">
        <el-button type="primary" @click="showAddObjective" style="margin-bottom:12px">新增课程目标</el-button>
        <el-table :data="objectives" v-loading="loading" border>
          <el-table-column type="index" label="#" width="50" />
          <el-table-column prop="courseCode" label="课程编码" width="100" />
          <el-table-column prop="courseName" label="课程名称" width="120" />
          <el-table-column prop="objectiveCode" label="目标编码" width="100" />
          <el-table-column prop="objectiveName" label="目标名称" />
          <el-table-column label="操作" width="160">
            <template #default="{ row }">
              <div style="display:flex;gap:6px">
                <el-button size="small" @click="editObjective(row)">编辑</el-button>
                <el-button size="small" type="danger" @click="removeObjective(row.id)">删除</el-button>
              </div>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>

    <!-- 新增/编辑指标 -->
    <el-dialog v-model="indicatorDlg" :title="indEditId ? '编辑认证指标' : '新增认证指标'" width="420px">
      <el-form :model="indForm" label-width="80px">
        <el-form-item label="编码"><el-input v-model="indForm.indicatorCode" /></el-form-item>
        <el-form-item label="名称"><el-input v-model="indForm.indicatorName" /></el-form-item>
        <el-form-item label="描述"><el-input v-model="indForm.indicatorDesc" type="textarea" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="indicatorDlg = false">取消</el-button>
        <el-button type="primary" @click="submitIndicator">确定</el-button>
      </template>
    </el-dialog>

    <!-- 新增/编辑毕业要求 -->
    <el-dialog v-model="reqDlg" :title="reqEditId ? '编辑毕业要求' : '新增毕业要求'" width="420px">
      <el-form :model="reqForm" label-width="80px">
        <el-form-item label="编码"><el-input v-model="reqForm.requirementCode" /></el-form-item>
        <el-form-item label="名称"><el-input v-model="reqForm.requirementName" /></el-form-item>
        <el-form-item label="描述"><el-input v-model="reqForm.requirementDesc" type="textarea" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="reqDlg = false">取消</el-button>
        <el-button type="primary" @click="submitRequirement">确定</el-button>
      </template>
    </el-dialog>

    <!-- 新增/编辑课程目标 -->
    <el-dialog v-model="objDlg" :title="objEditId ? '编辑课程目标' : '新增课程目标'" width="420px">
      <el-form :model="objForm" label-width="80px">
        <el-form-item label="课程编码"><el-input v-model="objForm.courseCode" /></el-form-item>
        <el-form-item label="课程名称"><el-input v-model="objForm.courseName" /></el-form-item>
        <el-form-item label="目标编码"><el-input v-model="objForm.objectiveCode" /></el-form-item>
        <el-form-item label="目标名称"><el-input v-model="objForm.objectiveName" /></el-form-item>
        <el-form-item label="描述"><el-input v-model="objForm.objectiveDesc" type="textarea" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="objDlg = false">取消</el-button>
        <el-button type="primary" @click="submitObjective">确定</el-button>
      </template>
    </el-dialog>

    <!-- 关联毕业要求 -->
    <el-dialog v-model="bindReqDlg" title="关联毕业要求" width="420px">
      <el-select v-model="selectedReqId" placeholder="请选择毕业要求" style="width:100%">
        <el-option v-for="r in unlinkedReqs" :key="r.id" :label="`${r.requirementCode} ${r.requirementName}`" :value="r.id" />
      </el-select>
      <template #footer>
        <el-button @click="bindReqDlg = false">取消</el-button>
        <el-button type="primary" @click="submitBindReq">确定</el-button>
      </template>
    </el-dialog>

    <!-- 关联课程目标 -->
    <el-dialog v-model="bindObjDlg" title="关联课程目标" width="420px">
      <el-select v-model="selectedObjId" placeholder="请选择课程目标" style="width:100%">
        <el-option v-for="o in unlinkedObjs" :key="o.id" :label="`${o.courseCode}-${o.objectiveCode} ${o.objectiveName}`" :value="o.id" />
      </el-select>
      <template #footer>
        <el-button @click="bindObjDlg = false">取消</el-button>
        <el-button type="primary" @click="submitBindObj">确定</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup>
// 【认证指标管理页面】三Tab：指标/毕业要求/课程目标的增删改查+关联绑定/解绑。

import { computed, onMounted, reactive, ref } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import {
  indicatorCreateApi, indicatorRelationsApi, bindRequirementApi, unbindRequirementApi,
  requirementCreateApi, bindObjectiveApi, unbindObjectiveApi,
  objectiveCreateApi, deleteIndicatorApi, deleteRequirementApi, deleteObjectiveApi,
  updateIndicatorApi, updateRequirementApi, updateObjectiveApi
} from "../../api/modules/indicator";

const activeTab = ref("indicators");
const loading = ref(false);
const indicators = ref([]);
const requirements = ref([]);
const objectives = ref([]);
const irRels = ref([]);
const corRels = ref([]);

const indicatorDlg = ref(false);
const reqDlg = ref(false);
const objDlg = ref(false);
const bindReqDlg = ref(false);
const bindObjDlg = ref(false);
const currentIndicator = ref(null);
const currentRequirement = ref(null);
const selectedReqId = ref(null);
const selectedObjId = ref(null);
const indEditId = ref(null);
const reqEditId = ref(null);
const objEditId = ref(null);

const indForm = reactive({ indicatorCode: "", indicatorName: "", indicatorDesc: "", status: 1 });
const reqForm = reactive({ requirementCode: "", requirementName: "", requirementDesc: "", status: 1 });
const objForm = reactive({ courseCode: "", courseName: "", objectiveCode: "", objectiveName: "", objectiveDesc: "", status: 1 });

const loadAll = async () => {
  loading.value = true;
  try {
    const res = await indicatorRelationsApi();
    indicators.value = res.data.indicators || [];
    requirements.value = res.data.requirements || [];
    objectives.value = res.data.objectives || [];
    irRels.value = res.data.indicatorRequirementRels || [];
    corRels.value = res.data.courseObjectiveRequirementRels || [];
  } finally {
    loading.value = false;
  }
};

const linkedReqs = (indicatorId) => {
  const ids = irRels.value.filter((r) => r.indicatorId === indicatorId).map((r) => r.requirementId);
  return requirements.value.filter((r) => ids.includes(r.id));
};

const linkedObjs = (requirementId) => {
  const ids = corRels.value.filter((r) => r.requirementId === requirementId).map((r) => r.objectiveId);
  return objectives.value.filter((o) => ids.includes(o.id));
};

const unlinkedReqs = computed(() => {
  if (!currentIndicator.value) return [];
  const linkedIds = irRels.value.filter((r) => r.indicatorId === currentIndicator.value.id).map((r) => r.requirementId);
  return requirements.value.filter((r) => !linkedIds.includes(r.id));
});

const unlinkedObjs = computed(() => {
  if (!currentRequirement.value) return [];
  const linkedIds = corRels.value.filter((r) => r.requirementId === currentRequirement.value.id).map((r) => r.objectiveId);
  return objectives.value.filter((o) => !linkedIds.includes(o.id));
});

// Indicator CRUD
const showAddIndicator = () => {
  indEditId.value = null;
  indForm.indicatorCode = "";
  indForm.indicatorName = "";
  indForm.indicatorDesc = "";
  indicatorDlg.value = true;
};
const editIndicator = (row) => {
  indEditId.value = row.id;
  indForm.indicatorCode = row.indicatorCode;
  indForm.indicatorName = row.indicatorName;
  indForm.indicatorDesc = row.indicatorDesc || "";
  indicatorDlg.value = true;
};
const submitIndicator = async () => {
  if (indEditId.value) {
    await updateIndicatorApi(indEditId.value, { ...indForm });
    ElMessage.success("更新成功");
  } else {
    await indicatorCreateApi({ ...indForm });
    ElMessage.success("新增成功");
  }
  indicatorDlg.value = false;
  await loadAll();
};

// Requirement CRUD
const showAddRequirement = () => {
  reqEditId.value = null;
  reqForm.requirementCode = "";
  reqForm.requirementName = "";
  reqForm.requirementDesc = "";
  reqDlg.value = true;
};
const editRequirement = (row) => {
  reqEditId.value = row.id;
  reqForm.requirementCode = row.requirementCode;
  reqForm.requirementName = row.requirementName;
  reqForm.requirementDesc = row.requirementDesc || "";
  reqDlg.value = true;
};
const submitRequirement = async () => {
  if (reqEditId.value) {
    await updateRequirementApi(reqEditId.value, { ...reqForm });
    ElMessage.success("更新成功");
  } else {
    await requirementCreateApi({ ...reqForm });
    ElMessage.success("新增成功");
  }
  reqDlg.value = false;
  await loadAll();
};

// Objective CRUD
const showAddObjective = () => {
  objEditId.value = null;
  objForm.courseCode = "";
  objForm.courseName = "";
  objForm.objectiveCode = "";
  objForm.objectiveName = "";
  objForm.objectiveDesc = "";
  objDlg.value = true;
};
const editObjective = (row) => {
  objEditId.value = row.id;
  objForm.courseCode = row.courseCode;
  objForm.courseName = row.courseName;
  objForm.objectiveCode = row.objectiveCode;
  objForm.objectiveName = row.objectiveName;
  objForm.objectiveDesc = row.objectiveDesc || "";
  objDlg.value = true;
};
const submitObjective = async () => {
  if (objEditId.value) {
    await updateObjectiveApi(objEditId.value, { ...objForm });
    ElMessage.success("更新成功");
  } else {
    await objectiveCreateApi({ ...objForm });
    ElMessage.success("新增成功");
  }
  objDlg.value = false;
  await loadAll();
};

// Bind / Unbind
const showBindReq = (row) => {
  currentIndicator.value = row;
  selectedReqId.value = null;
  bindReqDlg.value = true;
};
const submitBindReq = async () => {
  if (!selectedReqId.value) return ElMessage.warning("请选择毕业要求");
  await bindRequirementApi(currentIndicator.value.id, selectedReqId.value);
  ElMessage.success("关联成功");
  bindReqDlg.value = false;
  await loadAll();
};
const unbindReq = async (indicatorId, requirementId) => {
  await unbindRequirementApi(indicatorId, requirementId);
  ElMessage.success("已解除关联");
  await loadAll();
};

const showBindObj = (row) => {
  currentRequirement.value = row;
  selectedObjId.value = null;
  bindObjDlg.value = true;
};
const submitBindObj = async () => {
  if (!selectedObjId.value) return ElMessage.warning("请选择课程目标");
  await bindObjectiveApi(currentRequirement.value.id, selectedObjId.value);
  ElMessage.success("关联成功");
  bindObjDlg.value = false;
  await loadAll();
};
const unbindObj = async (requirementId, objectiveId) => {
  await unbindObjectiveApi(requirementId, objectiveId);
  ElMessage.success("已解除关联");
  await loadAll();
};

const removeIndicator = async (id) => {
  await ElMessageBox.confirm("删除指标将同时解除所有关联，确认删除？", "警告", { type: "warning" });
  await deleteIndicatorApi(id);
  ElMessage.success("已删除");
  await loadAll();
};

const removeRequirement = async (id) => {
  await ElMessageBox.confirm("删除毕业要求将同时解除所有关联，确认删除？", "警告", { type: "warning" });
  await deleteRequirementApi(id);
  ElMessage.success("已删除");
  await loadAll();
};

const removeObjective = async (id) => {
  await ElMessageBox.confirm("删除课程目标将同时解除所有关联，确认删除？", "警告", { type: "warning" });
  await deleteObjectiveApi(id);
  ElMessage.success("已删除");
  await loadAll();
};

onMounted(loadAll);
</script>
