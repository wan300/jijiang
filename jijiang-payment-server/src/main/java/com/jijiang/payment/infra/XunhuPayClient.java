package com.jijiang.payment.infra;

import java.math.BigDecimal;

public interface XunhuPayClient {
    CreateOrderResponse createOrder(CreateOrderRequest request);

    RefundOrderResponse refundOrder(RefundOrderRequest request);

    record CreateOrderRequest(String tradeOrderId, BigDecimal amount, String title, String notifyUrl,
                              String returnUrl, String callbackUrl, String attach) {
    }

    record CreateOrderResponse(String tradeOrderId, String payUrl, String qrCodeUrl, String rawBody) {
    }

    record RefundOrderRequest(String tradeOrderId, String refundOrderId, BigDecimal refundAmount, String reason) {
    }

    record RefundOrderResponse(boolean success, String refundTransactionId, String message,
                               BigDecimal refundFee, String rawBody) {
    }
}
