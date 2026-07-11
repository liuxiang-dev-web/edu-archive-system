<template>
  <el-card>
    <template #header>
      <div class="card-header">
        <span>用户管理</span>
        <el-button v-if="isAdmin" type="primary" @click="showAddDialog">新增用户</el-button>
      </div>
    </template>
    <el-table :data="users" v-loading="loading">
      <el-table-column type="index" label="#" width="50" />
      <el-table-column prop="username" label="用户名" />
      <el-table-column prop="realName" label="姓名" />
      <el-table-column prop="email" label="邮箱" />
      <el-table-column prop="status" label="状态">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'danger'">{{ row.status === 1 ? "启用" : "禁用" }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="280">
        <template #default="{ row }">
          <el-button size="small" @click="changeStatus(row, 1)">启用</el-button>
          <el-button size="small" type="warning" @click="changeStatus(row, 0)">禁用</el-button>
          <el-button size="small" type="primary" @click="loadRoles(row)">角色</el-button>
          <el-button size="small" type="danger" @click="remove(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" title="新增用户" width="480px" @closed="resetForm">
      <el-form :model="form" :rules="rules" ref="formRef" label-width="80px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" maxlength="64" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" type="password" show-password maxlength="32" />
        </el-form-item>
        <el-form-item label="姓名" prop="realName">
          <el-input v-model="form.realName" maxlength="64" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="form.email" maxlength="128" />
        </el-form-item>
        <el-form-item label="角色" prop="roleCode">
          <el-select v-model="form.roleCode" style="width: 100%">
            <el-option label="管理员 (ADMIN)" value="ADMIN" />
            <el-option label="教师 (TEACHER)" value="TEACHER" />
            <el-option label="审核员 (AUDITOR)" value="AUDITOR" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitAdd">确定</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup>
// 【用户管理页面】新增/删除/启用/禁用用户，角色查看。仅管理员可见新增和删除按钮。

import { computed, onMounted, reactive, ref } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import { userDeleteApi, userPageApi, userRolesApi, userStatusApi } from "../../api/modules/user";
import { registerApi } from "../../api/modules/auth";
import { getProfile } from "../../utils/auth";

const users = ref([]);
const loading = ref(false);
const dialogVisible = ref(false);
const formRef = ref(null);

const isAdmin = computed(() => {
  const profile = getProfile();
  return (profile.roles || []).includes("ADMIN");
});

const form = reactive({
  username: "",
  password: "",
  realName: "",
  email: "",
  roleCode: "TEACHER",
});

const rules = {
  username: [
    { required: true, message: "请输入用户名", trigger: "blur" },
    { min: 3, max: 64, message: "用户名长度需为3-64", trigger: "blur" },
  ],
  password: [
    { required: true, message: "请输入密码", trigger: "blur" },
    { min: 6, max: 32, message: "密码长度需为6-32", trigger: "blur" },
  ],
  realName: [{ required: true, message: "请输入真实姓名", trigger: "blur" }],
  roleCode: [{ required: true, message: "请选择角色", trigger: "change" }],
};

const resetForm = () => {
  formRef.value?.resetFields();
};

const loadUsers = async () => {
  loading.value = true;
  try {
    const res = await userPageApi();
    users.value = res.data || [];
  } finally {
    loading.value = false;
  }
};

const showAddDialog = () => {
  dialogVisible.value = true;
};

const submitAdd = async () => {
  const valid = await formRef.value.validate().catch(() => false);
  if (!valid) return;
  await registerApi({
    username: form.username,
    password: form.password,
    realName: form.realName,
    email: form.email || undefined,
    roleCode: form.roleCode,
  });
  ElMessage.success("新增用户成功");
  dialogVisible.value = false;
  await loadUsers();
};

const changeStatus = async (row, status) => {
  await userStatusApi(row.id, status);
  ElMessage.success("状态更新成功");
  await loadUsers();
};

const loadRoles = async (row) => {
  const res = await userRolesApi(row.id);
  ElMessageBox.alert(JSON.stringify(res.data || [], null, 2), `${row.username} 的角色关系`, { confirmButtonText: "确定" });
};

const remove = async (row) => {
  const profile = getProfile();
  if (profile.user?.username === row.username) {
    ElMessage.warning("不能删除自己");
    return;
  }
  await ElMessageBox.confirm(`确认删除用户"${row.username}"？该操作不可恢复。`, "警告", { type: "warning" });
  await userDeleteApi(row.id);
  ElMessage.success("已删除");
  await loadUsers();
};

onMounted(loadUsers);
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
