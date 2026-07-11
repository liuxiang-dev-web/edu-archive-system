// 【Axios实例】BaseURL=/api、请求拦截器(注入Token)、响应拦截器(统一错误处理、401跳转登录)。

import axios from "axios";
import { ElMessage } from "element-plus";
import { clearAuth, getToken } from "../utils/auth";

const http = axios.create({
  // Dev: use Vite proxy "/api" -> "http://localhost:8080"
  // Prod: can be overridden by VITE_API_BASE (e.g. "http://server:8080/api")
  baseURL: import.meta?.env?.VITE_API_BASE || "/api",
  timeout: 30000
});

http.interceptors.request.use((config) => {
  const token = getToken();
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

http.interceptors.response.use(
  (res) => {
    const payload = res.data;
    if (typeof payload?.code === "number" && payload.code !== 200) {
      ElMessage.error(payload.message || "请求失败");
      if (payload.code === 401) {
        clearAuth();
        window.location.href = "/login";
      }
      return Promise.reject(payload);
    }
    return payload;
  },
  (err) => {
    const status = err?.response?.status;
    if (status === 401) {
      clearAuth();
      window.location.href = "/login";
    }
    ElMessage.error(err?.response?.data?.message || err.message || "网络异常");
    return Promise.reject(err);
  }
);

export default http;
