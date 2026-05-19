import type { ApiResult } from "@/types/admin";

const API_BASE = import.meta.env.VITE_API_BASE || "";

type Method = "GET" | "POST" | "PUT" | "DELETE";

interface RequestOptions {
  method?: Method;
  data?: unknown;
  params?: Record<string, unknown>;
  skipAuth?: boolean;
}

export class BizError extends Error {
  constructor(public code: number, message: string) {
    super(message);
  }
}

export async function request<T>(url: string, options: RequestOptions = {}): Promise<T> {
  const headers: Record<string, string> = {
    "Content-Type": "application/json;charset=UTF-8",
    "X-Client-Version": "admin/1.0.0",
  };
  const token = localStorage.getItem("adminToken");
  if (!options.skipAuth && token) {
    headers.Authorization = `Bearer ${token}`;
  }

  const response = await fetch(buildUrl(API_BASE + url, options.params), {
    method: options.method || "GET",
    headers,
    body: options.data === undefined ? undefined : JSON.stringify(options.data),
  });
  const body = (await response.json()) as ApiResult<T>;
  if (body.code === 0) {
    return body.data;
  }
  if (body.code === 80001) {
    localStorage.removeItem("adminToken");
    localStorage.removeItem("adminInfo");
    if (location.pathname !== "/login") {
      location.href = "/login";
    }
  }
  throw new BizError(body.code || 90001, body.message || "请求失败");
}

function buildUrl(base: string, params?: Record<string, unknown>) {
  if (!params) return base;
  const query = Object.entries(params)
    .filter(([, value]) => value !== undefined && value !== null && value !== "")
    .map(([key, value]) => `${encodeURIComponent(key)}=${encodeURIComponent(String(value))}`)
    .join("&");
  return query ? `${base}${base.includes("?") ? "&" : "?"}${query}` : base;
}
