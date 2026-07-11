// 【用户API】userPageApi(列表)、userStatusApi(启用禁用)、userDeleteApi(删除)、userRolesApi(角色)。

import http from "../http";

export function userPageApi() {
  return http.get("/users");
}

export function userStatusApi(id, status) {
  return http.put(`/users/${id}/status`, null, { params: { status } });
}

export function userRolesApi(userId) {
  return http.get(`/users/${userId}/roles`);
}

export function userDeleteApi(id) {
  return http.delete(`/users/${id}`);
}
