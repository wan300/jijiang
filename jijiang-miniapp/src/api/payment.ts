import { request } from "@/api/request";
import type { PaymentCreateResult } from "@/types/domain";

export function payOrder(orderId: number) {
  return request<PaymentCreateResult>({
    url: "/api/payment/create",
    method: "POST",
    data: { orderId },
  });
}
