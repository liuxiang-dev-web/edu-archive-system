<template>
  <el-row :gutter="12">
    <el-col :span="12">
      <el-card>
        <template #header>
          <div class="card-header">
            <span>系统配置</span>
            <el-button type="primary" size="small" @click="showAddConfig">新增配置</el-button>
          </div>
        </template>
        <el-table :data="configs" v-loading="cfgLoading" height="360">
          <el-table-column prop="configGroup" label="分组" width="90">
            <template #default="{ row }">
              <el-tag size="small" type="info">{{ row.configGroup }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="configKey" label="键" min-width="140" />
          <el-table-column prop="configValue" label="值">
            <template #default="{ row }">
              <template v-if="row._editing">
                <el-input v-model="row._editValue" size="small" />
              </template>
              <template v-else>
                {{ row.isEncrypted === 1 ? '******' : row.configValue }}
              </template>
            </template>
          </el-table-column>
          <el-table-column prop="remark" label="说明" min-width="100" show-overflow-tooltip />
          <el-table-column label="操作" width="160">
            <template #default="{ row }">
              <template v-if="row._editing">
                <el-button size="small" type="primary" @click="saveRow(row)">保存</el-button>
                <el-button size="small" @click="cancelEdit(row)">取消</el-button>
              </template>
              <template v-else>
                <el-button size="small" @click="startEdit(row)">编辑</el-button>
                <el-button size="small" type="danger"
                  :disabled="row.isBuiltin === 1" @click="removeConfig(row.id)">删除</el-button>
              </template>
            </template>
          </el-table-column>
        </el-table>
      </el-card>
    </el-col>
    <el-col :span="12">
      <el-card>
        <template #header>
          <div class="card-header">
            <span>操作日志</span>
            <el-button type="primary" size="small" @click="backup">数据备份</el-button>
          </div>
        </template>
        <el-form inline>
          <el-form-item label="日志类型">
            <el-select v-model="logType" style="width: 120px" @change="loadLogs(1)">
              <el-option value="" label="全部" />
              <el-option value="LOGIN" label="登录" />
              <el-option value="OPERATION" label="操作" />
              <el-option value="ERROR" label="异常" />
            </el-select>
          </el-form-item>
          <el-form-item><el-button @click="loadLogs(1)">查询</el-button></el-form-item>
        </el-form>
        <el-table :data="logs" v-loading="logLoading" height="320" @row-click="showLogDetail">
          <el-table-column prop="logType" label="类型" width="80">
            <template #default="{ row }">
              <el-tag :type="row.logType === 'ERROR' ? 'danger' : row.logType === 'LOGIN' ? 'success' : ''" size="small">
                {{ row.logType }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="username" label="用户" width="90" />
          <el-table-column prop="actionName" label="动作" min-width="100" show-overflow-tooltip />
          <el-table-column prop="resultStatus" label="结果" width="70">
            <template #default="{ row }">
              <el-tag :type="row.resultStatus === 'SUCCESS' ? 'success' : 'danger'" size="small">
                {{ row.resultStatus === 'SUCCESS' ? '成功' : '失败' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createdAt" label="时间" width="160" />
        </el-table>
        <el-pagination
          v-model:current-page="logPageNo"
          v-model:page-size="logPageSize"
          :total="logTotal"
          layout="total, prev, pager, next"
          size="small"
          style="margin-top:8px"
          @current-change="(p) => loadLogs(p)"
        />
      </el-card>
    </el-col>
  </el-row>

  <!-- 日志详情对话框 -->
  <el-dialog v-model="logDetailDlg" title="日志详情" width="520px">
    <el-descriptions :column="2" border v-if="logDetail">
      <el-descriptions-item label="类型">{{ logDetail.logType }}</el-descriptions-item>
      <el-descriptions-item label="模块">{{ logDetail.moduleName }}</el-descriptions-item>
      <el-descriptions-item label="动作">{{ logDetail.actionName }}</el-descriptions-item>
      <el-descriptions-item label="用户">{{ logDetail.username }}</el-descriptions-item>
      <el-descriptions-item label="结果">{{ logDetail.resultStatus }}</el-descriptions-item>
      <el-descriptions-item label="时间">{{ logDetail.createdAt }}</el-descriptions-item>
      <el-descriptions-item label="IP">{{ logDetail.ipAddr }}</el-descriptions-item>
      <el-descriptions-item label="URI">{{ logDetail.requestUri }}</el-descriptions-item>
      <el-descriptions-item label="详情" :span="2">{{ logDetail.detailJson || '-' }}</el-descriptions-item>
    </el-descriptions>
  </el-dialog>

  <!-- 新增/编辑配置对话框 -->
  <el-dialog v-model="cfgDlg" :title="cfgEditingId ? '编辑配置' : '新增配置'" width="480px">
    <el-form :model="cfgForm" label-width="80px">
      <el-form-item label="配置键">
        <el-input v-model="cfgForm.configKey" :disabled="!!cfgEditingId" />
      </el-form-item>
      <el-form-item label="配置值">
        <el-input v-model="cfgForm.configValue" />
      </el-form-item>
      <el-form-item label="分组">
        <el-input v-model="cfgForm.configGroup" placeholder="如 FILE, SYSTEM" />
      </el-form-item>
      <el-form-item label="说明">
        <el-input v-model="cfgForm.remark" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="cfgDlg = false">取消</el-button>
      <el-button type="primary" @click="submitCfg">确定</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
// 【系统配置页面】配置表行内编辑/新增/删除、操作日志分页查询和详情弹窗。

import { onMounted, reactive, ref } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import { configsApi, deleteConfigApi, logsApi, saveConfigApi, backupApi } from "../../api/modules/system";

const configs = ref([]);
const cfgLoading = ref(false);
const logs = ref([]);
const logLoading = ref(false);
const logType = ref("");
const logPageNo = ref(1);
const logPageSize = ref(20);
const logTotal = ref(0);

const logDetailDlg = ref(false);
const logDetail = ref(null);

const cfgDlg = ref(false);
const cfgEditingId = ref(null);
const cfgForm = reactive({ configKey: "", configValue: "", configGroup: "SYSTEM", remark: "" });

const loadConfigs = async () => {
  cfgLoading.value = true;
  try {
    configs.value = (await configsApi()).data || [];
  } finally {
    cfgLoading.value = false;
  }
};

const loadLogs = async (pageNo = 1) => {
  logLoading.value = true;
  logPageNo.value = pageNo;
  try {
    const res = await logsApi({ logType: logType.value, pageNo, pageSize: logPageSize.value });
    logs.value = res.data?.records || [];
    logTotal.value = Number(res.data?.total || 0);
  } finally {
    logLoading.value = false;
  }
};

const showLogDetail = (row) => {
  logDetail.value = row;
  logDetailDlg.value = true;
};

// Config editing
const startEdit = (row) => {
  row._editing = true;
  row._editValue = row.configValue;
};

const cancelEdit = (row) => {
  row._editing = false;
  row._editValue = null;
};

const saveRow = async (row) => {
  await saveConfigApi({ id: row.id, configKey: row.configKey, configValue: row._editValue });
  ElMessage.success("保存成功");
  row.configValue = row._editValue;
  row._editing = false;
};

const showAddConfig = () => {
  cfgEditingId.value = null;
  cfgForm.configKey = "";
  cfgForm.configValue = "";
  cfgForm.configGroup = "SYSTEM";
  cfgForm.remark = "";
  cfgDlg.value = true;
};

const submitCfg = async () => {
  if (!cfgForm.configKey) return ElMessage.warning("请输入配置键");
  await saveConfigApi({
    id: cfgEditingId.value,
    configKey: cfgForm.configKey,
    configValue: cfgForm.configValue,
    configGroup: cfgForm.configGroup,
    remark: cfgForm.remark,
  });
  ElMessage.success(cfgEditingId.value ? "更新成功" : "新增成功");
  cfgDlg.value = false;
  await loadConfigs();
};

const removeConfig = async (id) => {
  await ElMessageBox.confirm("确认删除此配置？", "提示");
  await deleteConfigApi(id);
  ElMessage.success("已删除");
  await loadConfigs();
};

const backup = async () => {
  const res = await backupApi();
  ElMessage.success("备份任务已触发。提示：" + res.data);
};

onMounted(async () => {
  await Promise.all([loadConfigs(), loadLogs()]);
});
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
