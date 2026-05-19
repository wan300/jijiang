package com.jijiang.infra;

import java.math.BigDecimal;

public interface PaymentServerClient {
    CreatePaymentResponse createPayment(CreatePaymentRequest request);

    RefundResponse refundPayment(RefundRequest request);

    PaymentStatusResponse queryPaymentStatus(String tradeOrderId);

    record CreatePaymentRequest(Long orderId, String orderNo, BigDecimal amount, String title) {
    }

    record CreatePaymentResponse(String channel, Long orderId, String orderNo, String tradeOrderId,
                                 String payUrl, String qrCodeUrl, Integer expireSeconds) {
    }

    record RefundRequest(String tradeOrderId, BigDecimal refundAmount, String reason) {
    }

    record RefundResponse(boolean success, String refundId, String message) {
    }

    record PaymentStatusResponse(Long orderId, String orderNo, String tradeOrderId, BigDecimal amount,
                                 String status, String channel, String transactionId, String payUrl,
                                 String qrCodeUrl, String paidTime) {
    }
}
