import { resolveApiUrl } from "@/utils/env";

// ── admin request (separate token) ──
function adminToken(): string {
  return uni.getStorageSync("adminToken") || "";
}

/** H2 returns lowercase keys (userid), MySQL returns camelCase (userId). Normalize both to camelCase. */
function camelKeys(obj: unknown): unknown {
  if (Array.isArray(obj)) return obj.map(camelKeys);
  if (obj !== null && typeof obj === "object") {
    const out: Record<string, unknown> = {};
    for (const key of Object.keys(obj as Record<string, unknown>)) {
      const cc = key.replace(/_([a-z])/g, (_, c: string) => c.toUpperCase());
      out[cc] = camelKeys((obj as Record<string, unknown>)[key]);
    }
    return out;
  }
  return obj;
}

type RequestMethod = "GET" | "POST" | "PUT" | "DELETE" | "OPTIONS" | "HEAD" | "TRACE" | "CONNECT";

async function adminRequest<T>(url: string, options?: { method?: RequestMethod; data?: Record<string, unknown> }): Promise<T> {
  const header: Record<string, string> = { "Content-Type": "application/json;charset=UTF-8" };
  const token = adminToken();
  if (token) header.Authorization = `Bearer ${token}`;

  const baseUrl = resolveApiUrl(url);
  const method: RequestMethod = options?.method || "GET";
  const rawData = options?.data;

  // GET: build query string manually, filter undefined
  let fullUrl = baseUrl;
  let body: Record<string, unknown> | undefined = undefined;
  if (method === "GET" && rawData) {
    const entries = Object.entries(rawData).filter(([, v]) => v !== undefined && v !== null && v !== "");
    if (entries.length) {
      fullUrl += "?" + entries.map(([k, v]) => `${k}=${encodeURIComponent(String(v))}`).join("&");
    }
  } else {
    body = rawData;
  }

  const res = await uni.request({
    url: fullUrl,
    method,
    data: body,
    header,
    timeout: 15000,
  });
  const rbody = res.data as { code: number; message: string; data: T };
  if (rbody.code === 0) return camelKeys(rbody.data) as T;
  throw new Error(rbody.message || "请求失败");
}

// ── auth ──
export interface AdminInfo {
  id: number;
  username: string;
  displayName: string;
  roleCode: string;
}

export function adminLogin(username: string, password: string) {
  return adminRequest<{ accessToken: string; adminInfo: AdminInfo }>(`/admin/auth/login`, {
    method: "POST",
    data: { username, password },
  });
}

// ── dashboard ──
export interface AdminMetrics {
  userTotal: number;
  verifiedUserTotal: number;
  pendingVerifyTotal: number;
  pendingServiceTotal: number;
  onlineServiceTotal: number;
  orderTotal: number;
  paidGmv: number;
  todayOrderTotal: number;
}

export function getDashboard() {
  return adminRequest<{
    metrics: AdminMetrics;
    orderStatus: { status: number; total: number }[];
    topServices: { id: number; title: string; salesCount: number; scoreAvg: number; sellerName: string }[];
  }>(`/admin/dashboard/overview`);
}

// ── verify ──
export interface VerifyItem {
  id: number;
  userId: number;
  nickname: string;
  campusId: number;
  campusName: string;
  certType: number;
  certImageUrl: string;
  realName: string;
  studentNo: string;
  ocrConfidence: number;
  createTime: string;
}

export interface PendingVerifiesResult {
  items: VerifyItem[];
  total: number;
  page: number;
  pageSize: number;
}

export function getPendingVerifies(params: { page?: number; pageSize?: number; keyword?: string }) {
  return adminRequest<PendingVerifiesResult>(`/admin/verify/pending`, { data: params as unknown as Record<string, unknown> });
}

export function reviewVerify(recordId: number, passed: boolean, reason?: string) {
  return adminRequest<void>(`/admin/verify/review`, {
    method: "POST",
    data: { id: recordId, passed, reason: reason || "" },
  });
}

// ── service ──
export interface AdminServiceItem {
  id: number;
  sellerId: number;
  sellerName: string;
  campusId: number;
  categoryId: number;
  categoryName: string;
  title: string;
  description: string;
  price: number;
  coverUrl: string;
  stock: number;
  usedStock: number;
  status: number;
  scoreAvg: number;
  salesCount: number;
  createTime: string;
}

export function getPendingServices(params: { page?: number; pageSize?: number; keyword?: string }) {
  return adminRequest<{ items: AdminServiceItem[]; total: number; page: number; pageSize: number }>(`/admin/service/pending`, { data: params as unknown as Record<string, unknown> });
}

export function getServiceList(params: { status?: number; page?: number; pageSize?: number; keyword?: string }) {
  return adminRequest<{ items: AdminServiceItem[]; total: number; page: number; pageSize: number }>(`/admin/service/list`, { data: params as unknown as Record<string, unknown> });
}

export function reviewService(serviceId: number, passed: boolean, reason?: string) {
  return adminRequest<void>(`/admin/service/review`, {
    method: "POST",
    data: { id: serviceId, passed, reason: reason || "" },
  });
}

export function offlineService(serviceId: number, reason?: string) {
  return adminRequest<void>(`/admin/service/offline`, {
    method: "POST",
    data: { id: serviceId, reason: reason || "" },
  });
}

// ── order ──
export interface AdminOrderItem {
  id: number;
  orderNo: string;
  buyerId: number;
  buyerName: string;
  sellerId: number;
  sellerName: string;
  serviceId: number;
  serviceTitle: string;
  amount: number;
  status: number;
  createTime: string;
  payTime: string;
  confirmTime: string;
}

export interface AdminOrderDetail {
  order: {
    id: number; orderNo: string; buyerId: number; buyerName: string;
    sellerId: number; sellerName: string; serviceId: number;
    serviceTitle: string; serviceCoverUrl: string; amount: number;
    status: number; remark: string; deliverText: string;
    createTime: string; expireTime: string; payTime: string;
    acceptTime: string; deliverTime: string; confirmTime: string;
  };
  payments: { id: number; outTradeNo: string; transactionId: string; amount: number; status: number; payChannel: string; createTime: string }[];
  logs: { id: number; fromStatus: number; toStatus: number; operatorId: number; remark: string; createTime: string }[];
  messages: { id: number; senderId: number; receiverId: number; content: string; isRead: number; createTime: string }[];
}

export function getAdminOrderList(params: { status?: number; page?: number; pageSize?: number; keyword?: string }) {
  return adminRequest<{ items: AdminOrderItem[]; total: number; page: number; pageSize: number }>(`/admin/order/list`, { data: params as unknown as Record<string, unknown> });
}

export function getAdminOrderDetail(orderId: number) {
  return adminRequest<AdminOrderDetail>(`/admin/order/detail`, { data: { orderId } as unknown as Record<string, unknown> });
}
