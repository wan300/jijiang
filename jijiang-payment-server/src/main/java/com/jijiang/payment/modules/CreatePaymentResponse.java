package com.jijiang.payment.modules;

public record CreatePaymentResponse(String channel, Long orderId, String orderNo, String tradeOrderId,
                                    String payUrl, String qrCodeUrl, Integer expireSeconds) {
}
