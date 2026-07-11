// 【管理员API】角色CRUD、权限列表、角色权限保存、用户角色保存。

import http from "../http";

// 角色
export function roleListApi() { return http.get("/admin/roles"); }
export function roleCreateApi(data) { return http.post("/admin/roles", data); }
export function roleUpdateApi(id, data) { return http.put(`/admin/roles/${id}`, data); }
export function roleDeleteApi(id) { return http.delete(`/admin/roles/${id}`); }

// 权限
export function permListApi() { return http.get("/admin/permissions"); }

// 角色权限
export function rolePermissionsApi(roleId) { return http.get(`/admin/roles/${roleId}/permissions`); }
export function rolePermissionsSaveApi(roleId, permIds) { return http.post(`/admin/roles/${roleId}/permissions`, { permissionIds: permIds }); }

// 用户角色
export function userListSimpleApi() { return http.get("/admin/users"); }
export function userRolesApi(userId) { return http.get(`/admin/users/${userId}/roles`); }
export function userRolesSaveApi(userId, roleIds) { return http.post(`/admin/users/${userId}/roles`, { roleIds }); }
