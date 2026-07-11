<template>
  <el-card>
    <template #header>档案上传</template>
    <el-form :model="form" label-width="90px" style="max-width: 700px">
      <el-form-item label="标题"><el-input v-model="form.title" /></el-form-item>
      <el-form-item label="分类ID"><el-input v-model.number="form.categoryId" /></el-form-item>
      <el-form-item label="备注"><el-input v-model="form.remark" /></el-form-item>
      <el-form-item label="文件">
        <el-upload :auto-upload="false" :on-change="onFileChange">
          <el-button>选择文件</el-button>
        </el-upload>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="submit">上传</el-button>
      </el-form-item>
    </el-form>
  </el-card>
</template>

<script setup>
// 【档案上传页面】选择文件+填写元数据(标题/分类/备注)+上传。

import { reactive, ref } from "vue";
import { ElMessage } from "element-plus";
import { archiveUploadApi } from "../../api/modules/archive";

const selectedFile = ref(null);
const form = reactive({ title: "", categoryId: 1, remark: "" });
const onFileChange = (file) => (selectedFile.value = file.raw);

const submit = async () => {
  if (!selectedFile.value) return ElMessage.warning("请选择文件");
  const fd = new FormData();
  fd.append("file", selectedFile.value);
  fd.append("title", form.title);
  fd.append("categoryId", String(form.categoryId));
  fd.append("remark", form.remark);
  await archiveUploadApi(fd);
  ElMessage.success("上传成功");
  form.title = "";
  form.remark = "";
  selectedFile.value = null;
};
</script>
