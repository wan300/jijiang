import { request } from "@/api/request";
import type { MockPayResult } from "@/types/domain";

export function mockPay(orderId: number) {
  return request<MockPayResult>({
    url: "/api/payment/mock-pay",
    method: "POST",
    data: { orderId },
  });
}
