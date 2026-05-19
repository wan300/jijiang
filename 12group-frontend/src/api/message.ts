import { request } from "@/api/request";
import type { MessageItem } from "@/types/domain";

export function sendMessage(orderId: number, content: string) {
  return request<{ messageId: number }>({
    url: "/api/message/send",
    method: "POST",
    data: { orderId, content },
  });
}

export function listMessages(orderId: number) {
  return request<MessageItem[]>({ url: "/api/message/list", params: { orderId } });
}
