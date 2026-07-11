// 【档案API】上传、分页、详情、更新、删除、审核、指标绑定、指标查询、解绑。

import http from "../http";

export function archiveUploadApi(formData) {
  return http.post("/archive/upload", formData);
}

export function archivePageApi(params) {
  return http.get("/archive/page", { params });
}

export function archiveDetailApi(id) {
  return http.get(`/archive/${id}`);
}

export function archiveUpdateApi(id, data) {
  return http.put(`/archive/${id}`, data);
}

export function archiveDeleteApi(id) {
  return http.delete(`/archive/${id}`);
}

export function archiveSubmitAuditApi(id) {
  return http.post(`/archive/${id}/submit-audit`);
}

export function archiveAuditApi(id, data) {
  return http.post(`/archive/${id}/audit`, data);
}

export function archiveBindIndicatorApi(id, data) {
  return http.post(`/archive/${id}/bind-indicator`, data);
}

export function archiveIndicatorBindingsApi(id) {
  return http.get(`/archive/${id}/indicator-bindings`);
}

export function archiveIndicatorRelDeleteApi(archiveId, relId) {
  return http.delete(`/archive/${archiveId}/indicator-rel/${relId}`);
}
