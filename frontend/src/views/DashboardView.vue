<template>
  <el-container>
    <el-header style="display:flex;justify-content:space-between;align-items:center">
      <div>面向工程教育认证的教学档案管理系统</div>
      <div>
        <el-tag>{{ roleCode }}</el-tag>
        <el-button text @click="logout">退出</el-button>
      </div>
    </el-header>
    <el-main>
      <el-tabs>
        <el-tab-pane v-if="hasPerm('menu:archive')" label="档案管理">
          <el-upload :auto-upload="false" :on-change="onFileChange">
            <el-button type="primary">选择文件</el-button>
          </el-upload>
          <el-input v-model="uploadForm.title" placeholder="标题" style="width:220px;margin:8px" />
          <el-input v-model="uploadForm.category" placeholder="分类" style="width:220px;margin:8px" />
          <el-input v-model="uploadForm.courseName" placeholder="课程名" style="width:220px;margin:8px" />
          <el-input v-model="uploadForm.indicatorPoint" placeholder="指标点" style="width:220px;margin:8px" />
          <el-button v-if="hasPerm('button:archive:upload')" type="success" @click="upload">上传档案</el-button>

          <el-table :data="archives" style="margin-top:12px">
            <el-table-column type="index" label="#" width="50"/>
            <el-table-column prop="title" label="标题"/>
            <el-table-column prop="courseName" label="课程"/>
            <el-table-column prop="indicatorPoint" label="指标点"/>
            <el-table-column prop="status" label="状态"/>
            <el-table-column label="操作" width="360">
              <template #default="scope">
                <el-button size="small" @click="submitAudit(scope.row.id)">提交审核</el-button>
                <el-button v-if="hasPerm('button:archive:audit')" size="small" type="success" @click="audit(scope.row.id, 'PASS')">通过</el-button>
                <el-button v-if="hasPerm('button:archive:audit')" size="small" type="warning" @click="audit(scope.row.id, 'REJECT')">驳回</el-button>
                <el-button size="small" @click="download(scope.row.id)">下载</el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-pagination
            v-model:current-page="pageNo"
            v-model:page-size="pageSize"
            :total="total"
            layout="total, prev, pager, next"
            style="margin-top: 12px"
            @current-change="loadArchives"
          />
        </el-tab-pane>

        <el-tab-pane v-if="hasPerm('menu:archive')" label="指标映射">
          <el-input v-model="mapping.indicatorName" placeholder="认证指标" style="width:220px;margin:8px" />
          <el-input v-model="mapping.graduationRequirement" placeholder="毕业要求" style="width:220px;margin:8px" />
          <el-input v-model="mapping.courseObjective" placeholder="课程目标" style="width:220px;margin:8px" />
          <el-button v-if="hasPerm('button:indicator:edit')" type="primary" @click="saveMapping">保存映射</el-button>
          <el-table :data="mappings" style="margin-top:12px">
            <el-table-column prop="indicatorName" label="指标" />
            <el-table-column prop="graduationRequirement" label="毕业要求" />
            <el-table-column prop="courseObjective" label="课程目标" />
          </el-table>
        </el-tab-pane>

        <el-tab-pane v-if="hasPerm('menu:report')" label="统计报表">
          <el-button @click="loadReports">刷新报表</el-button>
          <pre>{{ reports }}</pre>
          <el-button type="success" @click="exportExcel">导出Excel</el-button>
          <el-button type="warning" @click="exportPdf">导出PDF</el-button>
        </el-tab-pane>

        <el-tab-pane v-if="hasPerm('menu:system')" label="系统管理">
          <el-button @click="backup">数据备份</el-button>
          <el-button @click="loadLogs">加载日志</el-button>
          <pre>{{ logs }}</pre>
        </el-tab-pane>
      </el-tabs>
    </el-main>
  </el-container>
</template>

<script setup>
// 【首页仪表盘】展示最近档案列表、快捷入口按钮。

import { onMounted, reactive, ref } from "vue";
import { ElMessage } from "element-plus";
import http from "../api/http";
import { useRouter } from "vue-router";

const router = useRouter();
const roleCode = localStorage.getItem("roleCode");
const permissions = ref(JSON.parse(localStorage.getItem("permissions") || "[]"));
const archives = ref([]);
const mappings = ref([]);
const reports = ref({});
const logs = ref({});
const selectedFile = ref(null);
const pageNo = ref(1);
const pageSize = ref(10);
const total = ref(0);
const uploadForm = reactive({ title: "", category: "", courseName: "", indicatorPoint: "" });
const mapping = reactive({ indicatorName: "", graduationRequirement: "", courseObjective: "" });
const loadProfile = async () => {
  const res = await http.get("/auth/me");
  if (res.success) {
    permissions.value = res.data.permissions || [];
    localStorage.setItem("permissions", JSON.stringify(permissions.value));
  }
};

const loadArchives = async () => {
  const res = await http.get("/archive/list", { params: { pageNo: pageNo.value, pageSize: pageSize.value } });
  archives.value = res.data?.records || [];
  total.value = Number(res.data?.total || 0);
};
const hasPerm = (perm) => permissions.value.includes(perm) || roleCode === "ADMIN";
const onFileChange = (f) => (selectedFile.value = f.raw);
const upload = async () => {
  if (!selectedFile.value) return ElMessage.warning("请选择文件");
  const formData = new FormData();
  formData.append("file", selectedFile.value);
  Object.keys(uploadForm).forEach((k) => formData.append(k, uploadForm[k]));
  const res = await http.post("/archive/upload", formData);
  res.success ? ElMessage.success("上传成功") : ElMessage.error(res.message);
  loadArchives();
};
const submitAudit = async (id) => { await http.post(`/archive/${id}/submit-audit`); loadArchives(); };
const audit = async (id, action) => { await http.post(`/archive/${id}/audit?action=${action}`); loadArchives(); };
const download = (id) => window.open(`/api/archive/download/${id}`);
const saveMapping = async () => { await http.post("/indicators", mapping); loadMappings(); };
const loadMappings = async () => { const res = await http.get("/indicators"); mappings.value = res.data || []; };
const loadReports = async () => {
  const a = await http.get("/reports/archive-rate");
  const b = await http.get("/reports/audit-stat");
  reports.value = { archiveRate: a.data, auditStat: b.data };
};
const exportExcel = () => window.open("/api/archive/export/excel");
const exportPdf = () => window.open("/api/archive/export/pdf");
const backup = async () => { const res = await http.post("/system/backup"); ElMessage.success(String(res.data || "")); };
const loadLogs = async () => {
  const op = await http.get("/system/operation-logs");
  const lg = await http.get("/system/login-logs");
  logs.value = { operationLogs: op.data, loginLogs: lg.data };
};
const logout = () => { localStorage.clear(); router.push("/login"); };

onMounted(async () => {
  await loadProfile();
  await Promise.all([loadArchives(), loadMappings(), loadReports(), loadLogs()]);
});
</script>
