import { request } from "@/api/request";
import type { UploadToken } from "@/types/domain";

export interface UploadTokenPayload {
  fileName: string;
  maxSizeBytes: number;
}

export function getUploadToken(data: UploadTokenPayload) {
  return request<UploadToken>({
    url: "/api/user/verify/upload-token",
    method: "POST",
    data: data as unknown as Record<string, unknown>,
  });
}
