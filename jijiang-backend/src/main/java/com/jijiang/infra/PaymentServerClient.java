package com.jijiang.infra;

import java.math.BigDecimal;

public interface PaymentServerClient {
    CreatePaymentResponse createPayment(CreatePaymentRequest request);

    record CreatePaymentRequest(Long orderId, String orderNo, BigDecimal amount, String title) {
    }

    record CreatePaymentResponse(String channel, Long orderId, String orderNo, String tradeOrderId,
                                 String payUrl, String qrCodeUrl, Integer expireSeconds) {
    }
}
