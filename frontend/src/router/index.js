// 【路由配置】路由表定义、beforeEach守卫(登录校验+权限过滤)。

import { createRouter, createWebHistory } from "vue-router";
import LoginView from "../views/LoginView.vue";
import MainLayout from "../layout/MainLayout.vue";
import HomeView from "../views/HomeView.vue";
import UserManageView from "../views/user/UserManageView.vue";
import RolePermissionView from "../views/user/RolePermissionView.vue";
import ArchiveUploadView from "../views/archive/ArchiveUploadView.vue";
import ArchiveListView from "../views/archive/ArchiveListView.vue";
import ArchiveDetailView from "../views/archive/ArchiveDetailView.vue";
import ArchiveAuditView from "../views/archive/ArchiveAuditView.vue";
import IndicatorManageView from "../views/indicator/IndicatorManageView.vue";
import IndicatorBindView from "../views/indicator/IndicatorBindView.vue";
import CertificationFeatureView from "../views/certification/CertificationFeatureView.vue";
import ReportView from "../views/report/ReportView.vue";
import SystemView from "../views/system/SystemView.vue";
import { getProfile, getToken } from "../utils/auth";

const routes = [
  { path: "/login", component: LoginView },
  {
    path: "/",
    component: MainLayout,
    children: [
      { path: "", component: HomeView },
      { path: "users", component: UserManageView, meta: { perm: "menu:user" } },
      { path: "roles", component: RolePermissionView, meta: { perm: "menu:user" } },
      { path: "archive/upload", component: ArchiveUploadView, meta: { perm: "menu:archive" } },
      { path: "archive/list", component: ArchiveListView, meta: { perm: "menu:archive" } },
      { path: "archive/detail/:id", component: ArchiveDetailView, meta: { perm: "menu:archive" } },
      { path: "archive/audit", component: ArchiveAuditView, meta: { perm: "menu:archive" } },
      { path: "indicator/manage", component: IndicatorManageView, meta: { perm: "menu:indicator" } },
      { path: "indicator/bind", component: IndicatorBindView, meta: { perm: "menu:indicator" } },
      { path: "certification/feature", component: CertificationFeatureView, meta: { perm: "menu:report" } },
      { path: "report", component: ReportView, meta: { perm: "menu:report" } },
      { path: "system", component: SystemView, meta: { perm: "menu:system" } }
    ]
  }
];

const router = createRouter({
  history: createWebHistory(),
  routes
});

router.beforeEach((to, _, next) => {
  if (to.path === "/login") return next();
  if (!getToken()) return next("/login");
  const perm = to.meta?.perm;
  if (perm) {
    const profile = getProfile();
    const perms = profile.permissions || [];
    if (!perms.includes(perm)) return next("/");
  }
  next();
});

export default router;
