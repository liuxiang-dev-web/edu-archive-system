<template>
  <el-card>
    <template #header>档案审核</template>
    <el-table :data="records" v-loading="loading">
      <el-table-column type="index" label="#" width="50" />
      <el-table-column prop="archiveNo" label="档案编号" width="170" />
      <el-table-column prop="title" label="标题" />
      <el-table-column prop="fileName" label="文件名" />
      <el-table-column prop="lifecycleStatus" label="状态" width="100" />
      <el-table-column label="审核意见" width="180">
        <template #default="{ row }">
          <el-input v-model="comments[row.id]" placeholder="输入意见" size="small" />
        </template>
      </el-table-column>
      <el-table-column label="操作" width="280">
        <template #default="{ row }">
          <el-button size="small" @click="$router.push(`/archive/detail/${row.id}`)">详情</el-button>
          <el-button size="small" type="success" @click="audit(row.id, 'PASS')">通过</el-button>
          <el-button size="small" type="warning" @click="audit(row.id, 'REJECT')">驳回</el-button>
        </template>
      </el-table-column>
    </el-table>
  </el-card>
</template>

<script setup>
// 【档案审核页面】待审列表、通过/驳回操作、审核意见填写。仅审核员和管理员可见。

import { onMounted, reactive, ref } from "vue";
import { ElMessage } from "element-plus";
import { archiveAuditApi, archivePageApi } from "../../api/modules/archive";

const loading = ref(false);
const records = ref([]);
const comments = reactive({});

const loadData = async () => {
  loading.value = true;
  try {
    const res = await archivePageApi({ pageNo: 1, pageSize: 50, lifecycleStatus: "SUBMITTED" });
    records.value = res.data?.records || [];
  } finally {
    loading.value = false;
  }
};

const audit = async (id, action) => {
  await archiveAuditApi(id, { action, comment: comments[id] || "" });
  ElMessage.success("审核完成");
  await loadData();
};

onMounted(loadData);
</script>
