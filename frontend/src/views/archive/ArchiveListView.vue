<template>
  <el-card>
    <template #header>档案列表</template>
    <el-form inline :model="query">
      <el-form-item label="标题"><el-input v-model="query.title" clearable /></el-form-item>
      <el-form-item label="分类ID"><el-input v-model.number="query.categoryId" clearable /></el-form-item>
      <el-form-item label="状态"><el-input v-model="query.lifecycleStatus" clearable /></el-form-item>
      <el-form-item><el-button type="primary" @click="loadData">查询</el-button></el-form-item>
    </el-form>
    <el-table :data="records" v-loading="loading">
      <el-table-column type="index" label="#" width="50" />
      <el-table-column prop="archiveNo" label="档案编号" width="170" />
      <el-table-column prop="title" label="标题" />
      <el-table-column prop="fileExt" label="类型" width="80">
        <template #default="{ row }">
          <el-tag size="small" type="info">{{ (row.fileExt || '').toUpperCase() }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="lifecycleStatus" label="状态" width="120" />
      <el-table-column label="操作" width="380">
        <template #default="{ row }">
          <el-button size="small" @click="$router.push(`/archive/detail/${row.id}`)">详情</el-button>
          <el-button size="small" type="warning" @click="preview(row)">预览</el-button>
          <el-button size="small" type="success" @click="download(row.id)">下载</el-button>
          <el-button size="small" type="danger" @click="remove(row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination
      v-model:current-page="query.pageNo"
      v-model:page-size="query.pageSize"
      :total="total"
      layout="total, prev, pager, next"
      style="margin-top: 12px"
      @current-change="loadData"
    />

    <!-- 预览弹窗 -->
    <el-dialog v-model="previewDlg" :title="previewTitle" :width="previewIsImage ? '70vw' : '860px'" top="2vh" @closed="stopPreview">
      <template v-if="previewIsImage">
        <div style="text-align:center;max-height:80vh;overflow:auto">
          <img :src="previewUrl" style="max-width:100%;max-height:80vh;object-fit:contain;display:block;margin:0 auto" />
        </div>
      </template>
      <template v-else-if="previewInline">
        <iframe :src="previewUrl" style="width:100%;height:75vh;border:1px solid #e4e7ed;border-radius:6px" />
      </template>
      <template v-else>
        <el-result icon="info" title="此文件类型不支持在线预览" :sub-title="`${previewFileName} (${previewExt}) 无法在浏览器中直接显示`">
          <template #extra>
            <el-button type="primary" @click="triggerDownload(previewFileId); previewDlg = false">下载文件</el-button>
            <el-button @click="previewDlg = false">关闭</el-button>
          </template>
        </el-result>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup>
// 【档案列表页面】多条件检索、分页表格、预览弹窗(iframe/下载)、fetch下载、删除。

import { onMounted, reactive, ref } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import { archiveDeleteApi, archivePageApi } from "../../api/modules/archive";
import { getToken } from "../../utils/auth";

const loading = ref(false);
const records = ref([]);
const total = ref(0);
const query = reactive({ pageNo: 1, pageSize: 10, title: "", categoryId: null, lifecycleStatus: "" });

const previewDlg = ref(false);
const previewUrl = ref("");
const previewTitle = ref("");
const previewInline = ref(false);
const previewIsImage = ref(false);
const previewFileName = ref("");
const previewExt = ref("");
const previewFileId = ref(null);

const isInlineType = (ext) => ["pdf", "png", "jpg", "jpeg", "gif", "txt"].includes((ext || "").toLowerCase());
const isImageType = (ext) => ["png", "jpg", "jpeg", "gif"].includes((ext || "").toLowerCase());

const loadData = async () => {
  loading.value = true;
  try {
    const res = await archivePageApi(query);
    records.value = res.data?.records || [];
    total.value = Number(res.data?.total || 0);
  } finally {
    loading.value = false;
  }
};

const triggerDownload = async (id) => {
  const token = getToken();
  const res = await fetch(`/api/archive/download/${id}`, {
    headers: token ? { Authorization: `Bearer ${token}` } : {},
  });
  if (!res.ok) {
    const text = await res.text().catch(() => "");
    ElMessage.error(text || "下载失败");
    return;
  }
  const blob = await res.blob();
  const disposition = res.headers.get("Content-Disposition") || "";
  const match = disposition.match(/filename\*?=(?:UTF-8''|")([^";]*)/) || disposition.match(/filename="?([^";\n]*)"?/);
  const filename = match ? decodeURIComponent(match[1]) : "download";
  const url = URL.createObjectURL(blob);
  const a = document.createElement("a");
  a.href = url;
  a.download = filename;
  document.body.appendChild(a);
  a.click();
  document.body.removeChild(a);
  URL.revokeObjectURL(url);
};

const preview = (row) => {
  const ext = (row.fileExt || "").toLowerCase();
  previewFileId.value = row.id;
  previewFileName.value = row.fileName || row.title;
  previewExt.value = ext.toUpperCase();
  previewTitle.value = `预览：${row.fileName || row.title}`;
  previewUrl.value = `/api/archive/preview/${row.id}`;
  previewIsImage.value = isImageType(ext);
  previewInline.value = ext === "pdf" || ext === "txt";
  previewDlg.value = true;
};

const stopPreview = () => {
  previewUrl.value = "";
};

const download = (id) => triggerDownload(id);
const remove = async (id) => {
  await ElMessageBox.confirm("确认删除该档案？", "提示");
  await archiveDeleteApi(id);
  ElMessage.success("删除成功");
  await loadData();
};

onMounted(loadData);
</script>
