import { request } from "@/api/request";
import type { Category, ServiceItem } from "@/types/domain";

export interface SearchServiceParams {
  campusId?: number;
  keyword?: string;
  categoryId?: number;
}

export interface PublishServicePayload {
  categoryId: number;
  title: string;
  description: string;
  price: number;
  priceConfig: string;
  coverUrl: string;
  stock: number;
}

export function getCategories() {
  return request<Category[]>({ url: "/api/service/categories", skipAuth: true });
}

export function searchServices(params: SearchServiceParams) {
  return request<ServiceItem[]>({ url: "/api/service/search", params: params as Record<string, unknown>, skipAuth: true });
}

export function getServiceDetail(id: number) {
  return request<ServiceItem>({ url: "/api/service/detail", params: { id }, skipAuth: true });
}

export function publishService(data: PublishServicePayload) {
  return request<{ serviceId: number; status: number; message: string }>({
    url: "/api/service/publish",
    method: "POST",
    data: data as unknown as Record<string, unknown>,
  });
}
