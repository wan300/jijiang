package com.jijiang.payment.modules;

import java.math.BigDecimal;

public record CreatePaymentRequest(Long orderId, String orderNo, BigDecimal amount, String title) {
}
