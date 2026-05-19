import { request } from "@/api/request";
import type { DepositStatus } from "@/types/domain";

export function createDeposit() {
  return request<{ recordId: number; amount: number; status: number; outTradeNo?: string; message: string }>({
    url: "/api/deposit/create",
    method: "POST",
  });
}

export function getDepositStatus() {
  return request<DepositStatus>({
    url: "/api/deposit/status",
  });
}
