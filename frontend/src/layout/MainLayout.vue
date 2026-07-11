<template>
  <el-container class="layout-wrap">
    <el-aside width="220px" class="aside">
      <div class="logo">
        <div class="logo-badge">EA</div>
        <div class="logo-text">教学档案管理系统</div>
      </div>
      <el-menu :default-active="$route.path" router class="side-menu">
        <el-menu-item v-for="item in menuItems" :key="item.path" :index="item.path">
          <el-icon><component :is="item.icon" /></el-icon>
          <span>{{ item.label }}</span>
        </el-menu-item>
      </el-menu>
    </el-aside>
    <el-container class="main-shell">
      <el-header class="header">
        <div class="welcome">
          <div class="welcome-title">欢迎使用工程教育认证档案平台</div>
          <div class="welcome-sub">当前用户：{{ profile.user?.realName || profile.user?.username || "用户" }}</div>
        </div>
        <div class="header-tools">
          <el-switch
            v-model="darkMode"
            inline-prompt
            active-text="深色"
            inactive-text="浅色"
            @change="applyTheme"
          />
          <el-tag effect="dark">{{ roleText }}</el-tag>
          <el-button text @click="logout">退出</el-button>
        </div>
      </el-header>
      <el-main class="main-content">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
// 【主布局组件】侧边栏菜单(按权限过滤)、顶部状态栏(角色标签/深色切换/退出)、主内容区。

import { computed, ref } from "vue";
import {
  DataAnalysis,
  DocumentAdd,
  DocumentChecked,
  Files,
  HomeFilled,
  Lock,
  Operation,
  PieChart,
  Setting,
  User
} from "@element-plus/icons-vue";
import { useRouter } from "vue-router";
import { clearAuth, getProfile } from "../utils/auth";

const router = useRouter();
const profile = getProfile();
const darkMode = ref(localStorage.getItem("edu-theme") === "dark");
const roleLabelMap = {
  ADMIN: "管理员",
  TEACHER: "教师",
  AUDITOR: "审核员"
};
const roleText = computed(() =>
  (profile.roles || []).map((r) => roleLabelMap[r] || r).join(",") || "-"
);

const hasPerm = (perm) => {
  const perms = profile.permissions || [];
  return !perm || perms.includes(perm);
};

const menuItems = computed(() => [
  { path: "/", label: "首页", perm: null, icon: HomeFilled },
  { path: "/users", label: "用户管理", perm: "menu:user", icon: User },
  { path: "/roles", label: "角色权限", perm: "menu:user", icon: Lock },
  { path: "/archive/upload", label: "档案上传", perm: "menu:archive", icon: DocumentAdd },
  { path: "/archive/list", label: "档案列表", perm: "menu:archive", icon: Files },
  { path: "/archive/audit", label: "档案审核", perm: "menu:archive", icon: DocumentChecked },
  { path: "/indicator/manage", label: "认证指标", perm: "menu:indicator", icon: Operation },
  { path: "/indicator/bind", label: "关联管理", perm: "menu:indicator", icon: DataAnalysis },
  { path: "/certification/feature", label: "认证特色功能", perm: "menu:report", icon: PieChart },
  { path: "/report", label: "统计报表", perm: "menu:report", icon: PieChart },
  { path: "/system", label: "系统配置", perm: "menu:system", icon: Setting }
].filter((item) => hasPerm(item.perm)));

const applyTheme = () => {
  const isDark = !!darkMode.value;
  document.body.classList.toggle("theme-dark", isDark);
  localStorage.setItem("edu-theme", isDark ? "dark" : "light");
};

applyTheme();

const logout = () => {
  clearAuth();
  router.push("/login");
};
</script>

<style scoped>
.layout-wrap { min-height: 100vh; background: transparent; }
.aside {
  border-right: 1px solid #e4ecff;
  background: linear-gradient(180deg, #f4f8ff 0%, #ffffff 60%);
  box-shadow: 2px 0 10px rgba(80, 115, 188, 0.08);
}
.logo {
  height: 64px;
  padding: 10px 12px;
  display: flex;
  align-items: center;
  gap: 10px;
  border-bottom: 1px solid #e6edff;
}
.logo-badge {
  width: 34px;
  height: 34px;
  border-radius: 10px;
  background: linear-gradient(135deg, #3f7dff, #67a8ff);
  color: #fff;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
}
.logo-text { font-weight: 700; color: #2c4276; font-size: 14px; }
.side-menu { border-right: none; background: transparent; }
.side-menu :deep(.el-menu-item) {
  border-radius: 8px;
  margin: 6px 10px;
  gap: 8px;
}
.side-menu :deep(.el-menu-item.is-active) {
  background: #eaf1ff;
  color: #2c62ce;
  font-weight: 600;
}
.main-shell { background: transparent; }
.header {
  margin: 12px 12px 0;
  border-radius: 14px;
  padding: 0 18px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  border: 1px solid #e8eeff;
  box-shadow: 0 8px 20px rgba(42, 84, 180, 0.08);
  background: rgba(255, 255, 255, 0.92);
}
.welcome-title { font-size: 15px; font-weight: 700; color: #2a3f72; }
.welcome-sub { font-size: 12px; color: #7484a8; margin-top: 2px; }
.header-tools { display: flex; align-items: center; gap: 8px; }
.main-content { padding: 12px; }

:global(body.theme-dark) .aside {
  border-right: 1px solid #2f436f;
  background: linear-gradient(180deg, #1f2d4d 0%, #17233d 65%);
  box-shadow: 2px 0 16px rgba(0, 0, 0, 0.35);
}

:global(body.theme-dark) .logo {
  border-bottom: 1px solid #324871;
}

:global(body.theme-dark) .logo-badge {
  background: linear-gradient(135deg, #5b90ff, #78b2ff);
}

:global(body.theme-dark) .logo-text {
  color: #dce7ff;
}

:global(body.theme-dark) .side-menu :deep(.el-menu-item) {
  color: #d2ddf8;
}

:global(body.theme-dark) .side-menu :deep(.el-menu-item:hover) {
  background: #263a63;
  color: #eff4ff;
}

:global(body.theme-dark) .side-menu :deep(.el-menu-item.is-active) {
  background: linear-gradient(135deg, #34548c, #426eb1) !important;
  color: #ffffff !important;
}

:global(body.theme-dark) .header {
  border-color: #304264;
  background: rgba(24, 35, 59, 0.92);
}

:global(body.theme-dark) .welcome-title {
  color: #e2ebff;
}

:global(body.theme-dark) .welcome-sub {
  color: #b8c8ea;
}
</style>
