package com.jijiang.payment.infra;

import java.math.BigDecimal;

public interface XunhuPayClient {
    CreateOrderResponse createOrder(CreateOrderRequest request);

    record CreateOrderRequest(String tradeOrderId, BigDecimal amount, String title, String notifyUrl,
                              String returnUrl, String callbackUrl, String attach) {
    }

    record CreateOrderResponse(String tradeOrderId, String payUrl, String qrCodeUrl, String rawBody) {
    }
}
