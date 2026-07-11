// 【指标API】指标/毕业要求/课程目标CRUD、关联关系查询/绑定/解绑。

import http from "../http";

export function indicatorListApi() {
  return http.get("/indicators/list");
}

export function indicatorCreateApi(data) {
  return http.post("/indicators/create", data);
}

export function requirementListApi() {
  return http.get("/indicators/requirements");
}

export function requirementCreateApi(data) {
  return http.post("/indicators/requirements", data);
}

export function objectiveListApi() {
  return http.get("/indicators/objectives");
}

export function objectiveCreateApi(data) {
  return http.post("/indicators/objectives", data);
}

export function indicatorRelationsApi() {
  return http.get("/indicators/relations");
}

export function bindRequirementApi(indicatorId, requirementId) {
  return http.post(`/indicators/${indicatorId}/bind-requirement/${requirementId}`);
}

export function unbindRequirementApi(indicatorId, requirementId) {
  return http.delete(`/indicators/${indicatorId}/unbind-requirement/${requirementId}`);
}

export function bindObjectiveApi(requirementId, objectiveId) {
  return http.post(`/indicators/requirements/${requirementId}/bind-objective/${objectiveId}`);
}

export function unbindObjectiveApi(requirementId, objectiveId) {
  return http.delete(`/indicators/requirements/${requirementId}/unbind-objective/${objectiveId}`);
}

export function deleteIndicatorApi(id) {
  return http.delete(`/indicators/${id}`);
}

export function deleteRequirementApi(id) {
  return http.delete(`/indicators/requirements/${id}`);
}

export function deleteObjectiveApi(id) {
  return http.delete(`/indicators/objectives/${id}`);
}

export function updateIndicatorApi(id, data) {
  return http.put(`/indicators/${id}`, data);
}

export function updateRequirementApi(id, data) {
  return http.put(`/indicators/requirements/${id}`, data);
}

export function updateObjectiveApi(id, data) {
  return http.put(`/indicators/objectives/${id}`, data);
}
