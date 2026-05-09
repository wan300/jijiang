import { request } from "@/api/request";
import type {
  DashboardOverview,
  OrderDetailResult,
  OrderRecord,
  PageResult,
  ServiceRecord,
  VerifyRecord,
} from "@/types/admin";

export function getDashboardOverview() {
  return request<DashboardOverview>("/admin/dashboard/overview");
}

export function getPendingVerifies(params: { page: number; pageSize: number; keyword?: string }) {
  return request<PageResult<VerifyRecord>>("/admin/verify/pending", { params });
}

export function reviewVerify(data: { id: number; passed: boolean; reason?: string }) {
  return request<void>("/admin/verify/review", { method: "POST", data });
}

export function getServices(params: { page: number; pageSize: number; status?: number; keyword?: string }) {
  return request<PageResult<ServiceRecord>>("/admin/service/list", { params });
}

export function reviewService(data: { id: number; passed: boolean; reason?: string }) {
  return request<void>("/admin/service/review", { method: "POST", data });
}

export function offlineService(data: { id: number; reason?: string }) {
  return request<void>("/admin/service/offline", { method: "POST", data });
}

export function getOrders(params: { page: number; pageSize: number; status?: number; keyword?: string }) {
  return request<PageResult<OrderRecord>>("/admin/order/list", { params });
}

export function getOrderDetail(orderId: number) {
  return request<OrderDetailResult>("/admin/order/detail", { params: { orderId } });
}
