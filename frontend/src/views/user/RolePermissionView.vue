<template>
  <el-card>
    <template #header>角色权限管理</template>
    <el-tabs v-model="tab" @tab-change="onTabChange">
      <!-- 角色管理 -->
      <el-tab-pane label="角色管理" name="roles">
        <el-button type="primary" size="small" @click="showRoleDlg()" style="margin-bottom:12px">新增角色</el-button>
        <el-table :data="roles" v-loading="loading" border>
          <el-table-column type="index" label="#" width="50" />
          <el-table-column prop="roleCode" label="编码" width="120" />
          <el-table-column prop="roleName" label="名称" width="120" />
          <el-table-column prop="roleDesc" label="描述" />
          <el-table-column prop="status" label="状态" width="70">
            <template #default="{ row }">
              <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">{{ row.status === 1 ? '启用' : '禁用' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="140">
            <template #default="{ row }">
              <el-button size="small" @click="showRoleDlg(row)">编辑</el-button>
              <el-button size="small" type="danger" @click="removeRole(row.id)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <!-- 角色-权限分配 -->
      <el-tab-pane label="权限分配" name="permAssign">
        <el-form inline>
          <el-form-item label="选择角色">
            <el-select v-model="selectedRoleId" placeholder="请选择" @change="loadRolePerms" style="width:200px">
              <el-option v-for="r in roles" :key="r.id" :label="`${r.roleCode} ${r.roleName}`" :value="r.id" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="savePerms" :disabled="!selectedRoleId">保存权限</el-button>
          </el-form-item>
        </el-form>
        <el-checkbox-group v-model="checkedPermIds" v-if="selectedRoleId">
          <el-card v-for="group in permGroups" :key="group.name" style="margin-bottom:8px" shadow="never">
            <template #header>{{ group.name }}</template>
            <el-checkbox v-for="p in group.items" :key="p.id" :value="p.id" style="margin-right:16px;margin-bottom:6px">
              {{ p.permName }} <span style="color:#909399;font-size:12px">({{ p.permCode }})</span>
            </el-checkbox>
          </el-card>
        </el-checkbox-group>
        <el-empty v-else description="请先选择一个角色" />
      </el-tab-pane>

      <!-- 用户-角色分配 -->
      <el-tab-pane label="用户角色" name="userRole">
        <el-form inline>
          <el-form-item label="选择用户">
            <el-select v-model="selectedUserId" placeholder="请选择" @change="loadUserRoles" filterable style="width:220px">
              <el-option v-for="u in users" :key="u.id" :label="`${u.username} (${u.realName})`" :value="u.id" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="saveUserRoles" :disabled="!selectedUserId">保存角色</el-button>
          </el-form-item>
        </el-form>
        <el-checkbox-group v-model="checkedUserRoleIds" v-if="selectedUserId">
          <el-checkbox v-for="r in roles" :key="r.id" :value="r.id" style="margin-right:16px;margin-bottom:6px">
            {{ r.roleName }} <span style="color:#909399;font-size:12px">({{ r.roleCode }})</span>
          </el-checkbox>
        </el-checkbox-group>
        <el-empty v-else description="请先选择一个用户" />
      </el-tab-pane>
    </el-tabs>

    <!-- 角色新增/编辑弹窗 -->
    <el-dialog v-model="roleDlg" :title="roleEditId ? '编辑角色' : '新增角色'" width="460px">
      <el-form :model="roleForm" label-width="80px">
        <el-form-item label="编码"><el-input v-model="roleForm.roleCode" :disabled="!!roleEditId" /></el-form-item>
        <el-form-item label="名称"><el-input v-model="roleForm.roleName" /></el-form-item>
        <el-form-item label="描述"><el-input v-model="roleForm.roleDesc" /></el-form-item>
        <el-form-item label="排序"><el-input-number v-model="roleForm.sortNo" :min="0" /></el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="roleForm.status" :active-value="1" :inactive-value="0" active-text="启用" inactive-text="禁用" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="roleDlg = false">取消</el-button>
        <el-button type="primary" @click="submitRole">确定</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup>
// 【角色权限管理页面】三Tab布局：角色CRUD、权限分配(分组勾选)、用户角色分配。仅ADMIN可访问。

import { computed, onMounted, ref } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import {
  roleListApi, roleCreateApi, roleUpdateApi, roleDeleteApi,
  permListApi, rolePermissionsApi, rolePermissionsSaveApi,
  userListSimpleApi, userRolesApi, userRolesSaveApi
} from "../../api/modules/admin";

const tab = ref("roles");
const loading = ref(false);
const roles = ref([]);
const perms = ref([]);
const users = ref([]);

// 角色 CRUD
const roleDlg = ref(false);
const roleEditId = ref(null);
const roleForm = ref({ roleCode: "", roleName: "", roleDesc: "", sortNo: 0, status: 1 });

const showRoleDlg = (row) => {
  if (row) {
    roleEditId.value = row.id;
    roleForm.value = { ...row };
  } else {
    roleEditId.value = null;
    roleForm.value = { roleCode: "", roleName: "", roleDesc: "", sortNo: 0, status: 1 };
  }
  roleDlg.value = true;
};

const submitRole = async () => {
  if (!roleForm.value.roleCode || !roleForm.value.roleName) return ElMessage.warning("请填写编码和名称");
  if (roleEditId.value) {
    await roleUpdateApi(roleEditId.value, roleForm.value);
    ElMessage.success("更新成功");
  } else {
    await roleCreateApi(roleForm.value);
    ElMessage.success("创建成功");
  }
  roleDlg.value = false;
  await loadRoles();
};

const removeRole = async (id) => {
  await ElMessageBox.confirm("删除角色将同时清除其权限关联和用户关联，确认？", "警告", { type: "warning" });
  await roleDeleteApi(id);
  ElMessage.success("已删除");
  await loadRoles();
};

// 权限分配
const selectedRoleId = ref(null);
const checkedPermIds = ref([]);

const permGroups = computed(() => {
  const menu = perms.value.filter(p => p.permType === "MENU");
  const button = perms.value.filter(p => p.permType === "BUTTON");
  const data = perms.value.filter(p => p.permType === "DATA");
  return [
    { name: "菜单权限", items: menu },
    { name: "按钮权限", items: button },
    { name: "数据权限", items: data },
  ].filter(g => g.items.length);
});

const loadRolePerms = async (roleId) => {
  const res = await rolePermissionsApi(roleId);
  checkedPermIds.value = res.data.permissionIds || [];
};

const savePerms = async () => {
  await rolePermissionsSaveApi(selectedRoleId.value, checkedPermIds.value);
  ElMessage.success("权限保存成功");
};

// 用户角色
const selectedUserId = ref(null);
const checkedUserRoleIds = ref([]);

const loadUserRoles = async (userId) => {
  const res = await userRolesApi(userId);
  checkedUserRoleIds.value = res.data.roleIds || [];
};

const saveUserRoles = async () => {
  await userRolesSaveApi(selectedUserId.value, checkedUserRoleIds.value);
  ElMessage.success("用户角色保存成功");
};

const loadRoles = async () => {
  roles.value = (await roleListApi()).data || [];
};

const onTabChange = async (name) => {
  if (name === "permAssign" || name === "userRole") await loadRoles();
  if (name === "userRole") users.value = (await userListSimpleApi()).data || [];
};

onMounted(async () => {
  loading.value = true;
  try {
    await loadRoles();
    perms.value = (await permListApi()).data || [];
  } finally {
    loading.value = false;
  }
});
</script>
