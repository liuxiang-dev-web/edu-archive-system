<template>
  <div class="login-wrap">
    <div class="hero-panel">
      <div class="hero-content">
        <h2>教学档案管理系统</h2>
        <p>面向工程教育认证的教学资料全流程数字化管理平台</p>
      </div>
    </div>
    <el-card class="login-card edu-page-card">
      <h2 class="login-title">账号登录</h2>
      <el-form :model="form" label-position="top">
        <el-form-item label="用户名"><el-input v-model="form.username" size="large" /></el-form-item>
        <el-form-item label="密码"><el-input v-model="form.password" type="password" size="large" show-password /></el-form-item>
      </el-form>
      <el-button type="primary" size="large" class="login-btn" @click="login">登录系统</el-button>
    </el-card>
  </div>
</template>

<script setup>
// 【登录页面】用户名密码表单，登录成功后存储Token和Profile，跳转首页。

import { onMounted, reactive } from "vue";
import { useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import { loginApi, meApi } from "../api/modules/auth";
import { clearAuth, setProfile, setToken } from "../utils/auth";

const router = useRouter();
const form = reactive({ username: "", password: "" });

onMounted(() => {
  clearAuth();
});

const login = async () => {
  const res = await loginApi(form);
  setToken(res.data.token);
  const profile = await meApi();
  setProfile(profile.data);
  ElMessage.success("登录成功");
  router.push("/");
};
</script>

<style scoped>
.login-wrap {
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: space-evenly;
  padding: 24px;
  background: linear-gradient(135deg, #eef4ff 0%, #f8fbff 50%, #eef3ff 100%);
}

.hero-panel {
  width: 48%;
  min-width: 360px;
  max-width: 760px;
  height: 72vh;
  border-radius: 18px;
  position: relative;
  overflow: hidden;
  background: #f4f8ff url("../assets/edu-hero.svg") center / cover no-repeat;
  box-shadow: 0 14px 28px rgba(49, 88, 170, 0.18);
}

.hero-content {
  position: absolute;
  left: 34px;
  top: 30px;
  color: #2a3f73;
  text-shadow: 0 1px 0 rgba(255, 255, 255, 0.7);
}

.hero-content h2 {
  margin: 0;
  font-size: 34px;
}

.hero-content p {
  margin-top: 8px;
  font-size: 15px;
}

.login-card {
  width: 420px;
  border-radius: 14px;
}

.login-title {
  margin: 0 0 10px;
  color: #2c3f70;
}

.login-btn {
  width: 100%;
}

@media (max-width: 980px) {
  .hero-panel {
    display: none;
  }

  .login-wrap {
    justify-content: center;
  }
}
</style>
