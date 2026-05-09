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
