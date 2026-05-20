export interface ApiResult<T> {
  code: number;
  message: string;
  data: T;
  timestamp: number;
}

export interface AdminInfo {
  id: number;
  username: string;
  displayName: string;
  roleCode: string;
}

export interface AdminLoginResult {
  accessToken: string;
  adminInfo: AdminInfo;
}

export interface PageResult<T> {
  items: T[];
  total: number;
  page: number;
  pageSize: number;
}

export interface DashboardOverview {
  metrics: Record<string, number | string>;
  orderStatus: Array<{ status: number; total: number }>;
  topServices: Array<{
    id: number;
    title: string;
    sellerName?: string;
    salesCount: number;
    scoreAvg: number;
  }>;
}

export interface VerifyRecord {
  id: number;
  userId: number;
  nickname?: string;
  campusId: number;
  campusName?: string;
  certType: number;
  certImageUrl: string;
  realName?: string;
  studentNo?: string;
  ocrConfidence?: number;
  createTime?: string;
}

export interface ServiceRecord {
  id: number;
  sellerId: number;
  sellerName?: string;
  campusId: number;
  categoryId: number;
  categoryName?: string;
  title: string;
  description?: string;
  price: number;
  coverUrl?: string;
  stock: number;
  usedStock: number;
  status: number;
  scoreAvg?: number;
  salesCount?: number;
  createTime?: string;
}

export interface OrderRecord {
  id: number;
  orderNo: string;
  buyerId: number;
  buyerName?: string;
  sellerId: number;
  sellerName?: string;
  serviceId: number;
  serviceTitle?: string;
  serviceCoverUrl?: string;
  amount: number;
  status: number;
  remark?: string;
  deliverText?: string;
  createTime?: string;
  expireTime?: string;
  payTime?: string;
  acceptTime?: string;
  deliverTime?: string;
  confirmTime?: string;
}

export interface PaymentRecord {
  id: number;
  outTradeNo: string;
  transactionId?: string;
  amount: number;
  status: number;
  payChannel: string;
  createTime?: string;
}

export interface OrderLogRecord {
  id: number;
  fromStatus?: number;
  toStatus: number;
  operatorId?: number;
  remark?: string;
  createTime?: string;
}

export interface MessageRecord {
  id: number;
  senderId: number;
  receiverId: number;
  content: string;
  isRead: number;
  createTime?: string;
}

export interface OrderDetailResult {
  order: OrderRecord;
  payments: PaymentRecord[];
  logs: OrderLogRecord[];
  messages: MessageRecord[];
}

export interface RefundRecord {
  id: number;
  orderId: number;
  userId: number;
  buyerName?: string;
  sellerId: number;
  sellerName?: string;
  reason: string;
  amount: number;
  status: number;
  reviewRemark?: string;
  reviewTime?: string;
  deductDeposit: number;
  createTime?: string;
}
