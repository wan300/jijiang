import { API_BASE } from "@/utils/env";
import { toast } from "@/utils/toast";

export interface ApiResult<T> {
  code: number;
  message: string;
  data: T;
  timestamp: number;
}

type Method = "GET" | "POST" | "PUT" | "DELETE";

interface RequestOptions {
  url: string;
  method?: Method;
  data?: Record<string, unknown>;
  params?: Record<string, unknown>;
  header?: Record<string, string>;
  skipAuth?: boolean;
  skipToast?: boolean;
  idempotent?: boolean;
}

export class BizError extends Error {
  constructor(public code: number, message: string) {
    super(message);
  }
}

export async function request<T>(options: RequestOptions): Promise<T> {
  const method = options.method || "GET";
  const header: Record<string, string> = {
    "Content-Type": "application/json;charset=UTF-8",
    "X-Client-Version": "miniapp/1.0.0",
    ...options.header,
  };
  const token = uni.getStorageSync("token");
  if (!options.skipAuth && token) header.Authorization = `Bearer ${token}`;
  if (method !== "GET" || options.idempotent) header["X-Idempotency-Key"] = createId();

  const url = buildUrl(API_BASE + options.url, options.params);
  try {
    const res = (await uni.request({
      url,
      method,
      data: options.data,
      header,
      timeout: 15000,
    })) as UniApp.RequestSuccessCallbackResult;
    const body = res.data as ApiResult<T>;
    if (body?.code === 0) return body.data;
    if (body?.code === 10001 || body?.code === 10002) {
      uni.removeStorageSync("token");
      uni.removeStorageSync("refreshToken");
      uni.removeStorageSync("userInfo");
      uni.navigateTo({ url: "/pages/login/index" });
    }
    const message = body?.message || "请求失败";
    if (!options.skipToast) toast(message);
    throw new BizError(body?.code || 90001, message);
  } catch (error) {
    if (!(error instanceof BizError) && !options.skipToast) {
      toast("网络异常，请稍后重试");
    }
    throw error;
  }
}

function buildUrl(base: string, params?: Record<string, unknown>) {
  if (!params) return base;
  const query = Object.entries(params)
    .filter(([, value]) => value !== undefined && value !== null && value !== "")
    .map(([key, value]) => `${key}=${encodeURIComponent(String(value))}`)
    .join("&");
  if (!query) return base;
  return `${base}${base.includes("?") ? "&" : "?"}${query}`;
}

function createId() {
  return `jj-${Date.now()}-${Math.random().toString(16).slice(2)}`;
}
