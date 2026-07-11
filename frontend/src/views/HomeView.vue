<template>
  <div class="home-wrap">
    <el-card class="edu-page-card hero-card">
      <div class="hero-main">
        <div>
          <h3 class="edu-section-title">系统首页</h3>
          <p>面向工程教育认证的教学档案管理系统前端管理端已就绪。</p>
          <p>当前账号可通过下方快捷入口直达高频业务页面。</p>
          <el-space wrap>
            <el-button type="primary" @click="checkHealth">检查后端连通性</el-button>
            <el-button @click="loadOverview">刷新统计概览</el-button>
          </el-space>
          <p v-if="healthText" class="health">{{ healthText }}</p>
        </div>
        <div class="hero-image" />
      </div>
    </el-card>

    <el-row :gutter="12" style="margin-top: 12px">
      <el-col :xs="24" :sm="12" :md="6">
        <el-card class="edu-page-card metric-card">
          <div class="metric-label">档案总数</div>
          <div class="metric-value">{{ overview.archiveTotal }}</div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :md="6">
        <el-card class="edu-page-card metric-card">
          <div class="metric-label">已归档数</div>
          <div class="metric-value">{{ overview.archiveApproved }}</div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :md="6">
        <el-card class="edu-page-card metric-card">
          <div class="metric-label">课程目标平均达成度</div>
          <div class="metric-value">{{ overview.objectiveAvg }}%</div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :md="6">
        <el-card class="edu-page-card metric-card">
          <div class="metric-label">毕业要求平均达成度</div>
          <div class="metric-value">{{ overview.requirementAvg }}%</div>
        </el-card>
      </el-col>
    </el-row>

    <el-card class="edu-page-card quick-card" style="margin-top: 12px">
      <template #header>
        <div class="quick-header">快捷入口</div>
      </template>
      <el-row :gutter="12">
        <el-col
          v-for="entry in quickEntries"
          :key="entry.path"
          :xs="24"
          :sm="12"
          :md="8"
          :lg="6"
        >
          <div class="quick-item" @click="to(entry.path)">
            <el-icon class="quick-icon"><component :is="entry.icon" /></el-icon>
            <div class="quick-text">
              <div class="quick-title">{{ entry.title }}</div>
              <div class="quick-desc">{{ entry.desc }}</div>
            </div>
          </div>
        </el-col>
      </el-row>
    </el-card>

    <el-row :gutter="12" style="margin-top: 12px">
      <el-col :xs="24" :md="12">
        <el-card class="edu-page-card stat-card">
          <template #header>审核通过率</template>
          <div class="stat-value">{{ overview.auditPassRate }}%</div>
          <div class="stat-sub">通过 {{ overview.auditPass }} / 总审核 {{ overview.auditTotal }}</div>
        </el-card>
      </el-col>
      <el-col :xs="24" :md="12">
        <el-card class="edu-page-card stat-card">
          <template #header>认证关联密度</template>
          <div class="stat-value">{{ overview.avgRelPerArchive }}</div>
          <div class="stat-sub">平均每份档案关联指标/要求/目标数量</div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from "vue";
import {
  DataAnalysis,
  DocumentAdd,
  DocumentChecked,
  Files,
  Histogram,
  Link,
  Operation,
  Setting,
  User
} from "@element-plus/icons-vue";
import { ElMessage } from "element-plus";
import { useRouter } from "vue-router";
import { healthApi } from "../api/modules/health";
import {
  archiveRateApi,
  auditStatApi,
  courseObjectiveAttainmentApi,
  graduationRequirementAttainmentApi,
  indicatorStatApi
} from "../api/modules/report";
import { getProfile } from "../utils/auth";

const router = useRouter();
const healthText = ref("");
const profile = getProfile();
const perms = profile.permissions || [];
const hasPerm = (perm) => !perm || perms.includes(perm);

const overview = reactive({
  archiveTotal: 0,
  archiveApproved: 0,
  auditPass: 0,
  auditTotal: 0,
  auditPassRate: 0,
  avgRelPerArchive: 0,
  objectiveAvg: 0,
  requirementAvg: 0
});

const allEntries = [
  { path: "/users", title: "用户管理", desc: "用户状态与角色查看", perm: "menu:user", icon: User },
  { path: "/archive/upload", title: "档案上传", desc: "新增教学档案材料", perm: "menu:archive", icon: DocumentAdd },
  { path: "/archive/list", title: "档案列表", desc: "检索与维护档案信息", perm: "menu:archive", icon: Files },
  { path: "/archive/audit", title: "档案审核", desc: "进行通过/驳回审核", perm: "menu:archive", icon: DocumentChecked },
  { path: "/indicator/manage", title: "认证指标", desc: "维护指标/毕业要求/目标", perm: "menu:indicator", icon: Operation },
  { path: "/indicator/bind", title: "关联管理", desc: "绑定档案与认证要素", perm: "menu:indicator", icon: Link },
  { path: "/report", title: "统计报表", desc: "查看达成度与统计分析", perm: "menu:report", icon: Histogram },
  { path: "/system", title: "系统配置", desc: "参数、日志与备份管理", perm: "menu:system", icon: Setting },
  { path: "/certification/feature", title: "认证特色", desc: "自动筛选、导出、清单", perm: "menu:report", icon: DataAnalysis }
];
const quickEntries = computed(() => allEntries.filter((x) => hasPerm(x.perm)));

const to = (path) => router.push(path);

const checkHealth = async () => {
  const res = await healthApi();
  healthText.value = `后端状态: ${res.data.status}, 时间: ${res.data.time}`;
  ElMessage.success("后端连通正常");
};

const loadOverview = async () => {
  try {
    const [archiveRate, auditStat, indicatorStat, objectiveRes, requirementRes] = await Promise.all([
      archiveRateApi(),
      auditStatApi(),
      indicatorStatApi(),
      courseObjectiveAttainmentApi(),
      graduationRequirementAttainmentApi()
    ]);
    const a = archiveRate.data || {};
    const au = auditStat.data || {};
    const i = indicatorStat.data || {};
    const o = objectiveRes.data || {};
    const r = requirementRes.data || {};

    overview.archiveTotal = a.total || 0;
    overview.archiveApproved = a.archived || 0;
    overview.auditPass = au.pass || 0;
    overview.auditTotal = au.total || 0;
    overview.auditPassRate = overview.auditTotal ? Number(((overview.auditPass * 100) / overview.auditTotal).toFixed(2)) : 0;
    overview.avgRelPerArchive = Number((i.avgRelationPerArchive || 0).toFixed(2));
    overview.objectiveAvg = Number((o.averageScore || 0).toFixed(2));
    overview.requirementAvg = Number((r.averageScore || 0).toFixed(2));
  } catch {
    ElMessage.warning("统计概览获取失败，请稍后重试");
  }
};

onMounted(() => {
  loadOverview();
});
</script>

<style scoped>
.home-wrap p {
  color: var(--edu-text-sub);
}

.hero-card {
  background: linear-gradient(135deg, var(--edu-primary-soft) 0%, var(--edu-card-bg) 60%);
}

.hero-main {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.hero-image {
  width: 42%;
  min-height: 190px;
  border-radius: 12px;
  background: #ecf3ff url("../assets/edu-hero.svg") center/cover no-repeat;
  border: 1px solid #dce7ff;
}

.health {
  margin-top: 8px;
  color: #385da7;
}

.stat-card {
  color: var(--edu-text-main);
  min-height: 138px;
}

.metric-card {
  min-height: 122px;
}

.metric-label {
  color: var(--edu-text-sub);
  font-size: 13px;
}

.metric-value {
  margin-top: 8px;
  font-size: 28px;
  font-weight: 700;
  color: var(--edu-text-main);
}

.quick-card {
  padding-bottom: 4px;
}

.quick-header {
  font-weight: 700;
  color: var(--edu-text-main);
}

.quick-item {
  cursor: pointer;
  border: 1px solid var(--edu-card-border);
  background: color-mix(in srgb, var(--edu-card-bg) 90%, #eef3ff 10%);
  border-radius: 12px;
  padding: 14px;
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
  transition: all 0.2s ease;
}

.quick-item:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 18px rgba(64, 103, 190, 0.14);
  border-color: #9ab7ff;
}

.quick-icon {
  font-size: 20px;
  color: #4b7fff;
}

.quick-title {
  color: var(--edu-text-main);
  font-weight: 700;
}

.quick-desc {
  color: var(--edu-text-sub);
  font-size: 12px;
  margin-top: 2px;
}

.stat-value {
  font-size: 30px;
  font-weight: 700;
  color: var(--edu-text-main);
}

.stat-sub {
  margin-top: 10px;
  color: var(--edu-text-sub);
  font-size: 13px;
}

@media (max-width: 980px) {
  .hero-image {
    display: none;
  }
}
</style>
