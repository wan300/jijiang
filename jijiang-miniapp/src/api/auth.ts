import { request } from "@/api/request";
import type { LoginResult } from "@/types/domain";

export function loginByCode(code: string) {
  return request<LoginResult>({
    url: "/api/auth/wx-login",
    method: "POST",
    data: { code },
    skipAuth: true,
  });
}

export function refreshToken() {
  return request<LoginResult>({
    url: "/api/auth/refresh",
    method: "POST",
  });
}

export function switchRole(targetRole: 1 | 2) {
  return request<LoginResult>({
    url: "/api/auth/switch-role",
    method: "POST",
    data: { targetRole },
  });
}

export function logout() {
  return request<void>({ url: "/api/auth/logout", method: "POST", skipToast: true });
}
