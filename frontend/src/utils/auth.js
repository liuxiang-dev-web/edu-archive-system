// 【认证工具】Token存取、Profile存取、hasPerm权限判断、clearAuth清除。

const TOKEN_KEY = "token";
const PROFILE_KEY = "profile";

export function getToken() {
  return localStorage.getItem(TOKEN_KEY) || "";
}

export function setToken(token) {
  localStorage.setItem(TOKEN_KEY, token);
}

export function clearAuth() {
  localStorage.removeItem(TOKEN_KEY);
  localStorage.removeItem(PROFILE_KEY);
}

export function setProfile(profile) {
  localStorage.setItem(PROFILE_KEY, JSON.stringify(profile || {}));
}

export function getProfile() {
  try {
    return JSON.parse(localStorage.getItem(PROFILE_KEY) || "{}");
  } catch {
    return {};
  }
}

export function hasPerm(permCode) {
  const profile = getProfile();
  const perms = profile.permissions || [];
  return perms.includes(permCode);
}
