import { request } from "@/api/request";

export function submitReview(data: { orderId: number; score: number; content: string }) {
  return request<{ reviewId: number }>({
    url: "/api/review/submit",
    method: "POST",
    data: data as Record<string, unknown>,
  });
}
