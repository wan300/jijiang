export type Role = 1 | 2;

export interface UserInfo {
  id: number;
  nickname: string;
  avatarUrl?: string;
  verifyStatus: number;
  currentRole: Role;
  campusId: number;
  campusName?: string;
  creditScore: number;
  isSellerVerified: number;
  depositPaid: number;
}

export interface LoginResult {
  accessToken: string;
  refreshToken: string;
  userInfo: UserInfo;
}

export interface Category {
  id: number;
  name: string;
  icon?: string;
}

export interface ServiceItem {
  id: number;
  sellerId: number;
  sellerName?: string;
  categoryId: number;
  title: string;
  description: string;
  price: number;
  priceConfig?: string;
  coverUrl?: string;
  stock: number;
  usedStock: number;
  status?: number;
  scoreAvg?: number;
  salesCount?: number;
}

export interface OrderItem {
  id: number;
  orderId?: number;
  orderNo: string;
  buyerId: number;
  sellerId: number;
  serviceId: number;
  amount: number;
  status: number;
  remark?: string;
  deliverText?: string;
  createTime?: string;
}

export interface OrderCreateResult {
  orderId: number;
  orderNo: string;
  amount: number;
  status: number;
}

export interface MessageItem {
  id: number;
  orderId: number;
  senderId: number;
  receiverId: number;
  content: string;
  isRead: number;
  createTime: string;
}

export interface VerifyResult {
  recordId: number;
  status: number;
  reviewMode: number;
  message: string;
}

export interface PaymentCreateResult {
  channel: "XUNHUPAY";
  orderId: number;
  orderNo: string;
  tradeOrderId: string;
  payUrl: string;
  qrCodeUrl?: string;
  expireSeconds: number;
}

export interface UploadToken {
  url: string;
  fields: Record<string, string>;
  fileKey: string;
  expiresAt: number;
}

export interface DepositRecord {
  id: number;
  amount: number;
  status: number;
  depositType: string;
  outTradeNo?: string;
  transactionId?: string;
  payTime?: string;
  refundTime?: string;
  createTime?: string;
}

export interface DepositStatus {
  depositPaid: number;
  records: DepositRecord[];
}

export interface DeletionStatus {
  status: number;
  requestTime?: string;
  coolingUntil?: string;
  completedTime?: string;
  cancelledTime?: string;
  deletionStatus?: number;
  message?: string;
}

export interface RefundSubmitRequest {
  orderId: number;
  reason: string;
  evidenceUrls?: string[];
}

export interface RefundRequestItem {
  id: number;
  orderId: number;
  userId: number;
  sellerId: number;
  reason: string;
  evidenceUrls?: string[];
  amount: number;
  status: number;
  reviewerId?: number;
  reviewRemark?: string;
  reviewTime?: string;
  deductDeposit: number;
  createTime?: string;
  orderNo?: string;
  orderAmount?: number;
  orderStatus?: number;
}

export interface PaymentSyncResult {
  orderId: number;
  orderNo: string;
  status: number;
  paid: boolean;
  paymentStatus: "PENDING" | "SUCCESS" | string;
  tradeOrderId?: string;
  paymentRecordStatus?: number;
  payTime?: string;
}
