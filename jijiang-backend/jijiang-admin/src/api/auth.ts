import { request } from "@/api/request";
import type { AdminLoginResult } from "@/types/admin";

export function login(username: string, password: string) {
  return request<AdminLoginResult>("/admin/auth/login", {
    method: "POST",
    data: { username, password },
    skipAuth: true,
  });
}
