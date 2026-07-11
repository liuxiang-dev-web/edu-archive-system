// 【系统管理API】日志查询、配置查询/保存/删除、数据备份。

import http from "../http";

export function logsApi(params) {
  return http.get("/system/logs", { params });
}

export function configsApi() {
  return http.get("/system/configs");
}

export function saveConfigApi(data) {
  return http.post("/system/configs", data);
}

export function deleteConfigApi(id) {
  return http.delete(`/system/configs/${id}`);
}

export function backupApi() {
  return http.post("/system/backup");
}
