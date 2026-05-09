package com.jijiang.payment.modules;

import java.math.BigDecimal;

public record PaymentCallbackRequest(Long orderId, String orderNo, String tradeOrderId, String transactionId,
                                     BigDecimal amount, String status, String channel, String payUrl, String qrCodeUrl) {
}
