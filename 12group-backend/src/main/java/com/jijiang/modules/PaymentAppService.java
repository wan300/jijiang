package com.jijiang.modules;

import com.jijiang.common.BusinessException;
import com.jijiang.common.UserContext;
import com.jijiang.infra.InternalSignatureSupport;
import com.jijiang.infra.PaymentServerClient;
import com.jijiang.infra.PaymentServerProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
class PaymentAppService {
    private static final String PAYMENT_CALLBACK_PATH = "/internal/payment/callback";

    private final JdbcTemplate jdbc;
    private final TransactionTemplate tx;
    private final ObjectMapper objectMapper;
    private final PaymentServerClient paymentServerClient;
    private final PaymentServerProperties paymentServerProperties;
    private final StringRedisTemplate redisTemplate;
    private final OrderAppService orderAppService;

    PaymentAppService(JdbcTemplate jdbc, TransactionTemplate tx, ObjectMapper objectMapper,
                      PaymentServerClient paymentServerClient, PaymentServerProperties paymentServerProperties,
                      StringRedisTemplate redisTemplate, OrderAppService orderAppService) {
        this.jdbc = jdbc;
        this.tx = tx;
        this.objectMapper = objectMapper;
        this.paymentServerClient = paymentServerClient;
        this.paymentServerProperties = paymentServerProperties;
        this.redisTemplate = redisTemplate;
        this.orderAppService = orderAppService;
    }

    Map<String, Object> createPayment(UserContext ctx, Long orderId) {
        Map<String, Object> order = jdbc.queryForMap("SELECT * FROM order_main WHERE id = ? AND is_deleted = 0", orderId);
        if (!ctx.userId().equals(((Number) order.get("buyer_id")).longValue())) {
            throw new BusinessException(30003, "无权支付该订单");
        }
        if (((Number) order.get("status")).intValue() != 10) {
            throw new BusinessException(30004, "订单状态不允许支付");
        }
        BigDecimal amount = (BigDecimal) order.get("amount");
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(30005, "订单金额不合法");
        }
        String orderNo = (String) order.get("order_no");
        Map<String, Object> existing = queryPayment(orderNo);
        if (existing != null && ((Number) existing.get("status")).intValue() == 1 && existing.get("pay_url") != null) {
            String tradeOrderId = (String) existing.get("out_trade_no");
            try {
                var remote = paymentServerClient.queryPaymentStatus(tradeOrderId);
                return cashier(orderId, orderNo, tradeOrderId,
                        remote.payUrl() == null || remote.payUrl().isBlank() ? (String) existing.get("pay_url") : remote.payUrl(),
                        remote.qrCodeUrl());
            } catch (Exception ignored) {
                return cashier(orderId, orderNo, tradeOrderId, (String) existing.get("pay_url"), null);
            }
        }
        var response = paymentServerClient.createPayment(new PaymentServerClient.CreatePaymentRequest(
                orderId,
                orderNo,
                amount,
                "技匠订单-" + orderNo
        ));
        tx.executeWithoutResult(status -> {
            Integer exists = jdbc.queryForObject("SELECT COUNT(*) FROM payment_record WHERE out_trade_no = ? AND is_deleted = 0",
                    Integer.class, response.tradeOrderId());
            if (exists == null || exists == 0) {
                jdbc.update("""
                    INSERT INTO payment_record (order_id, out_trade_no, transaction_id, amount, status, pay_channel, pay_url)
                    VALUES (?, ?, NULL, ?, 1, 'XUNHUPAY', ?)
                    """, orderId, response.tradeOrderId(), amount, response.payUrl());
            } else {
                jdbc.update("""
                    UPDATE payment_record
                    SET amount = ?, status = 1, pay_channel = 'XUNHUPAY', pay_url = ?, update_time = CURRENT_TIMESTAMP
                    WHERE out_trade_no = ? AND status = 1 AND is_deleted = 0
                    """, amount, response.payUrl(), response.tradeOrderId());
            }
        });
        return cashier(orderId, orderNo, response.tradeOrderId(), response.payUrl(), response.qrCodeUrl());
    }

    Map<String, Object> syncPayment(UserContext ctx, Long orderId) {
        if (orderId == null) {
            throw new BusinessException(30003, "订单不能为空");
        }
        Map<String, Object> order = jdbc.queryForMap("SELECT * FROM order_main WHERE id = ? AND is_deleted = 0", orderId);
        if (!ctx.userId().equals(((Number) order.get("buyer_id")).longValue())) {
            throw new BusinessException(30003, "无权同步该订单支付状态");
        }

        String orderNo = (String) order.get("order_no");
        int localStatus = ((Number) order.get("status")).intValue();
        Map<String, Object> payment = queryPaymentByOrderId(orderId);
        String paymentStatus = localStatus >= 20 ? "SUCCESS" : "PENDING";
        String tradeOrderId = payment == null ? orderNo : (String) payment.get("out_trade_no");

        if (localStatus < 20 && tradeOrderId != null && !tradeOrderId.isBlank()) {
            var remote = paymentServerClient.queryPaymentStatus(tradeOrderId);
            paymentStatus = remote.status();
            if ("SUCCESS".equals(remote.status())) {
                BigDecimal expectedAmount = payment == null ? (BigDecimal) order.get("amount") : (BigDecimal) payment.get("amount");
                if (remote.amount() == null || expectedAmount.compareTo(remote.amount()) != 0) {
                    throw new BusinessException(30010, "支付服务状态同步金额不匹配");
                }
                tx.executeWithoutResult(status -> {
                    Integer exists = jdbc.queryForObject("SELECT COUNT(*) FROM payment_record WHERE out_trade_no = ? AND is_deleted = 0",
                            Integer.class, remote.tradeOrderId());
                    if (exists == null || exists == 0) {
                        jdbc.update("""
                            INSERT INTO payment_record (order_id, out_trade_no, transaction_id, amount, status, pay_channel, pay_url)
                            VALUES (?, ?, NULL, ?, 1, 'XUNHUPAY', ?)
                            """, orderId, remote.tradeOrderId(), expectedAmount, remote.payUrl());
                    }
                    jdbc.update("""
                        UPDATE payment_record
                        SET status = 2, transaction_id = ?, pay_time = CURRENT_TIMESTAMP,
                            update_time = CURRENT_TIMESTAMP
                        WHERE out_trade_no = ? AND status <> 2 AND is_deleted = 0
                        """, remote.transactionId(), remote.tradeOrderId());
                    int orderUpdated = jdbc.update("""
                        UPDATE order_main
                        SET status = 20, pay_time = CURRENT_TIMESTAMP, expire_time = ?
                        WHERE id = ? AND status = 10 AND is_deleted = 0
                        """, Timestamp.valueOf(LocalDateTime.now().plusHours(24)), orderId);
                    if (orderUpdated > 0) {
                        orderAppService.insertOrderLog(orderId, 10, 20, ctx.userId(), "支付状态同步成功");
                    }
                });
                order = jdbc.queryForMap("SELECT * FROM order_main WHERE id = ? AND is_deleted = 0", orderId);
                payment = queryPayment(remote.tradeOrderId());
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("orderId", orderId);
        result.put("orderNo", orderNo);
        result.put("status", ((Number) order.get("status")).intValue());
        result.put("paid", ((Number) order.get("status")).intValue() >= 20);
        result.put("paymentStatus", paymentStatus);
        result.put("tradeOrderId", tradeOrderId);
        if (payment != null) {
            result.put("paymentRecordStatus", ((Number) payment.get("status")).intValue());
            result.put("payTime", payment.get("pay_time"));
        }
        return result;
    }

    void handlePaymentServerCallback(String rawBody, HttpHeaders headers) {
        InternalSignatureSupport.verify(paymentServerProperties.getClientId(), paymentServerProperties.getSharedSecret(),
                "POST", PAYMENT_CALLBACK_PATH, rawBody, headers, redisTemplate, Duration.ofMinutes(5));
        PaymentCallbackRequest callback = parseCallback(rawBody);
        if (!"SUCCESS".equals(callback.status())) {
            return;
        }
        String tradeOrderId = callback.tradeOrderId();
        if (tradeOrderId == null || tradeOrderId.isBlank()) {
            throw new BusinessException(30008, "支付服务回调缺少商户单号");
        }
        BigDecimal paidAmount = callback.amount();
        if (paidAmount == null || paidAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(30011, "支付服务回调金额不合法");
        }
        tx.executeWithoutResult(status -> {
            Map<String, Object> payment = queryPayment(tradeOrderId);
            if (payment == null) {
                payment = createPaymentMirror(callback);
            }
            BigDecimal expectedAmount = (BigDecimal) payment.get("amount");
            if (expectedAmount.compareTo(paidAmount) != 0) {
                throw new BusinessException(30010, "支付服务回调金额不匹配");
            }
            if (((Number) payment.get("status")).intValue() == 2) {
                return;
            }
            Long orderId = ((Number) payment.get("order_id")).longValue();
            Map<String, Object> order = jdbc.queryForMap("SELECT * FROM order_main WHERE id = ? AND is_deleted = 0", orderId);
            jdbc.update("""
                UPDATE payment_record
                SET status = 2, transaction_id = ?, pay_time = CURRENT_TIMESTAMP,
                    update_time = CURRENT_TIMESTAMP
                WHERE out_trade_no = ? AND status <> 2 AND is_deleted = 0
                """, callback.transactionId(), tradeOrderId);
            int orderUpdated = jdbc.update("""
                UPDATE order_main
                SET status = 20, pay_time = CURRENT_TIMESTAMP, expire_time = ?
                WHERE id = ? AND status = 10 AND is_deleted = 0
                """, Timestamp.valueOf(LocalDateTime.now().plusHours(24)), orderId);
            if (orderUpdated > 0) {
                Long buyerId = ((Number) order.get("buyer_id")).longValue();
                orderAppService.insertOrderLog(orderId, 10, 20, buyerId, "支付服务回调成功");
            }
        });
    }

    private Map<String, Object> cashier(Long orderId, String orderNo, String tradeOrderId, String payUrl, String qrCodeUrl) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("channel", "XUNHUPAY");
        result.put("orderId", orderId);
        result.put("orderNo", orderNo);
        result.put("tradeOrderId", tradeOrderId);
        result.put("payUrl", payUrl);
        result.put("qrCodeUrl", qrCodeUrl);
        result.put("expireSeconds", 300);
        return result;
    }

    private Map<String, Object> queryPayment(String outTradeNo) {
        try {
            return jdbc.queryForMap("SELECT * FROM payment_record WHERE out_trade_no = ? AND is_deleted = 0", outTradeNo);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    private Map<String, Object> queryPaymentByOrderId(Long orderId) {
        try {
            return jdbc.queryForMap("""
                SELECT * FROM payment_record
                WHERE order_id = ? AND is_deleted = 0
                ORDER BY id DESC LIMIT 1
                """, orderId);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    private Map<String, Object> createPaymentMirror(PaymentCallbackRequest callback) {
        Map<String, Object> order = jdbc.queryForMap("SELECT * FROM order_main WHERE order_no = ? AND is_deleted = 0", callback.orderNo());
        Long orderId = ((Number) order.get("id")).longValue();
        BigDecimal amount = (BigDecimal) order.get("amount");
        jdbc.update("""
            INSERT INTO payment_record (order_id, out_trade_no, transaction_id, amount, status, pay_channel, pay_url)
            VALUES (?, ?, NULL, ?, 1, 'XUNHUPAY', ?)
            """, orderId, callback.tradeOrderId(), amount, callback.payUrl());
        return queryPayment(callback.tradeOrderId());
    }

    private PaymentCallbackRequest parseCallback(String rawBody) {
        try {
            return objectMapper.readValue(rawBody, PaymentCallbackRequest.class);
        } catch (Exception e) {
            throw new BusinessException(30038, "支付服务回调报文不合法");
        }
    }

    record PaymentCallbackRequest(Long orderId, String orderNo, String tradeOrderId, String transactionId,
                                  BigDecimal amount, String status, String channel, String payUrl, String qrCodeUrl) {
    }
}
