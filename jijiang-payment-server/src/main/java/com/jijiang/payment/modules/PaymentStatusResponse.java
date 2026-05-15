package com.jijiang.payment.modules;

import java.math.BigDecimal;

public record PaymentStatusResponse(Long orderId, String orderNo, String tradeOrderId, BigDecimal amount,
                                    String status, String channel, String transactionId, String payUrl,
                                    String qrCodeUrl, String paidTime) {
}
