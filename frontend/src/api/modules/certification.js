// 【认证特色API】认证要素绑定、自动筛选、导出URL拼接、课程目标关联清单。

import http from "../http";

export function certificationBindApi(data) {
  return http.post("/certification/bind", data);
}

export function certificationAutoFilterApi(params) {
  return http.get("/certification/materials/auto-filter", { params });
}

export function certificationObjectiveListApi(objectiveId) {
  return http.get(`/certification/objective/${objectiveId}/archive-list`);
}

export function certificationExportUrl(params = {}) {
  const search = new URLSearchParams();
  Object.entries(params).forEach(([k, v]) => {
    if (v !== null && v !== undefined && String(v) !== "") search.append(k, v);
  });
  return `/api/certification/export?${search.toString()}`;
}
