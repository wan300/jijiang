package com.jijiang.payment.modules;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jijiang.payment.common.BusinessException;
import com.jijiang.payment.infra.AppServerClient;
import com.jijiang.payment.infra.AppServerProperties;
import com.jijiang.payment.infra.InternalSignatureSupport;
import com.jijiang.payment.infra.XunhuPayClient;
import com.jijiang.payment.infra.XunhuPayProperties;
import com.jijiang.payment.infra.XunhuPaySigner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
class PaymentService {
    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);
    private static final String CREATE_ORDER_PATH = "/internal/payment/orders";
    private static final String QUERY_ORDER_PATH_PREFIX = "/internal/payment/orders/";

    private final JdbcTemplate jdbc;
    private final TransactionTemplate tx;
    private final ObjectMapper objectMapper;
    private final XunhuPayClient xunhuPayClient;
    private final XunhuPayProperties xunhuPayProperties;
    private final AppServerClient appServerClient;
    private final AppServerProperties appServerProperties;
    private final StringRedisTemplate redisTemplate;

    PaymentService(JdbcTemplate jdbc, TransactionTemplate tx, ObjectMapper objectMapper,
                   XunhuPayClient xunhuPayClient, XunhuPayProperties xunhuPayProperties,
                   AppServerClient appServerClient, AppServerProperties appServerProperties,
                   StringRedisTemplate redisTemplate) {
        this.jdbc = jdbc;
        this.tx = tx;
        this.objectMapper = objectMapper;
        this.xunhuPayClient = xunhuPayClient;
        this.xunhuPayProperties = xunhuPayProperties;
        this.appServerClient = appServerClient;
        this.appServerProperties = appServerProperties;
        this.redisTemplate = redisTemplate;
    }

    CreatePaymentResponse create(String rawBody, HttpHeaders headers) {
        InternalSignatureSupport.verify(appServerProperties.getClientId(), appServerProperties.getSharedSecret(),
                "POST", CREATE_ORDER_PATH,
                rawBody, headers, redisTemplate, Duration.ofMinutes(5));
        CreatePaymentRequest request = parseCreateRequest(rawBody);
        validateCreateRequest(request);
        Map<String, Object> existing = queryByOrderNo(request.orderNo());
        if (existing != null && existing.get("pay_url") != null) {
            return toResponse(existing);
        }
        String tradeOrderId = request.orderNo();
        var response = xunhuPayClient.createOrder(new XunhuPayClient.CreateOrderRequest(
                tradeOrderId,
                request.amount(),
                request.title(),
                xunhuPayProperties.getNotifyUrl(),
                xunhuPayProperties.getReturnUrl(),
                xunhuPayProperties.getCallbackUrl(),
                String.valueOf(request.orderId())
        ));
        tx.executeWithoutResult(status -> upsertPaymentOrder(request, response));
        return new CreatePaymentResponse("XUNHUPAY", request.orderId(), request.orderNo(), response.tradeOrderId(),
                response.payUrl(), response.qrCodeUrl(), 300);
    }

    PaymentStatusResponse status(String tradeOrderId, HttpHeaders headers) {
        if (tradeOrderId == null || tradeOrderId.isBlank()) {
            throw new BusinessException(30008, "tradeOrderId is required");
        }
        String normalizedTradeOrderId = tradeOrderId.trim();
        InternalSignatureSupport.verify(appServerProperties.getClientId(), appServerProperties.getSharedSecret(),
                "GET", QUERY_ORDER_PATH_PREFIX + normalizedTradeOrderId,
                "", headers, redisTemplate, Duration.ofMinutes(5));
        Map<String, Object> payment = queryByTradeOrderId(normalizedTradeOrderId);
        if (payment == null) {
            throw new BusinessException(30009, "payment order not found");
        }
        return toStatusResponse(payment);
    }

    void handleXunhuNotify(Map<String, String> params) {
        if (!XunhuPaySigner.verify(params, xunhuPayProperties.getAppSecret())) {
            throw new BusinessException(30006, "虎皮椒回调验签失败");
        }
        if (!xunhuPayProperties.getAppId().equals(params.get("appid"))) {
            throw new BusinessException(30007, "虎皮椒回调 appid 不匹配");
        }
        if (!"OD".equals(params.get("status"))) {
            return;
        }
        String tradeOrderId = params.get("trade_order_id");
        if (tradeOrderId == null || tradeOrderId.isBlank()) {
            throw new BusinessException(30008, "虎皮椒回调缺少商户单号");
        }
        BigDecimal paidAmount = parseAmount(params.get("total_fee"));
        tx.executeWithoutResult(status -> markPaid(tradeOrderId, paidAmount, params));
        Map<String, Object> payment = queryByTradeOrderId(tradeOrderId);
        if (appServerProperties.isCallbackEnabled()) {
            tryNotifyApp(payment);
        }
    }

    @Scheduled(fixedDelay = 60000)
    void retryAppCallbacks() {
        if (!appServerProperties.isCallbackEnabled()) {
            return;
        }
        List<Map<String, Object>> rows = jdbc.queryForList("""
            SELECT * FROM payment_order
            WHERE status = 2 AND app_callback_status <> 1 AND is_deleted = 0
              AND (callback_next_time IS NULL OR callback_next_time <= CURRENT_TIMESTAMP)
              AND app_callback_attempts < 10
            ORDER BY id ASC LIMIT 20
            """);
        for (Map<String, Object> row : rows) {
            tryNotifyApp(row);
        }
    }

    private void markPaid(String tradeOrderId, BigDecimal paidAmount, Map<String, String> params) {
        Map<String, Object> payment = queryByTradeOrderId(tradeOrderId);
        if (payment == null) {
            throw new BusinessException(30009, "支付流水不存在");
        }
        BigDecimal expectedAmount = (BigDecimal) payment.get("amount");
        if (expectedAmount.compareTo(paidAmount) != 0) {
            throw new BusinessException(30010, "虎皮椒回调金额不匹配");
        }
        if (((Number) payment.get("status")).intValue() == 2) {
            return;
        }
        jdbc.update("""
            UPDATE payment_order
            SET status = 2, transaction_id = ?, notify_body = ?, paid_time = CURRENT_TIMESTAMP,
                app_callback_status = 0, callback_next_time = CURRENT_TIMESTAMP, update_time = CURRENT_TIMESTAMP
            WHERE trade_order_id = ? AND status <> 2 AND is_deleted = 0
            """, transactionId(params), params.toString(), tradeOrderId);
    }

    private void tryNotifyApp(Map<String, Object> payment) {
        PaymentCallbackRequest callback = toCallback(payment);
        try {
            appServerClient.notifyPaid(callback);
            jdbc.update("""
                UPDATE payment_order
                SET app_callback_status = 1, app_callback_body = ?, last_callback_error = NULL,
                    update_time = CURRENT_TIMESTAMP
                WHERE id = ?
                """, writeJson(callback), payment.get("id"));
        } catch (Exception e) {
            jdbc.update("""
                UPDATE payment_order
                SET app_callback_status = 2, app_callback_attempts = app_callback_attempts + 1,
                    last_callback_error = ?, callback_next_time = ?, update_time = CURRENT_TIMESTAMP
                WHERE id = ?
                """, truncate(e.getMessage()), Timestamp.valueOf(LocalDateTime.now().plusMinutes(1)), payment.get("id"));
            log.warn("notify app server failed orderNo={}", payment.get("order_no"), e);
        }
    }

    private void upsertPaymentOrder(CreatePaymentRequest request, XunhuPayClient.CreateOrderResponse response) {
        Integer exists = jdbc.queryForObject("SELECT COUNT(*) FROM payment_order WHERE order_no = ? AND is_deleted = 0",
                Integer.class, request.orderNo());
        if (exists == null || exists == 0) {
            jdbc.update("""
                INSERT INTO payment_order
                (order_id, order_no, trade_order_id, amount, title, status, channel, pay_url, qr_code_url)
                VALUES (?, ?, ?, ?, ?, 1, 'XUNHUPAY', ?, ?)
                """, request.orderId(), request.orderNo(), response.tradeOrderId(), request.amount(), request.title(),
                    response.payUrl(), response.qrCodeUrl());
        } else {
            jdbc.update("""
                UPDATE payment_order
                SET amount = ?, title = ?, pay_url = ?, qr_code_url = ?, update_time = CURRENT_TIMESTAMP
                WHERE order_no = ? AND status = 1 AND is_deleted = 0
                """, request.amount(), request.title(), response.payUrl(), response.qrCodeUrl(), request.orderNo());
        }
    }

    private CreatePaymentRequest parseCreateRequest(String rawBody) {
        try {
            return objectMapper.readValue(rawBody, CreatePaymentRequest.class);
        } catch (Exception e) {
            throw new BusinessException(30038, "支付创建报文不合法");
        }
    }

    private void validateCreateRequest(CreatePaymentRequest request) {
        if (request.orderId() == null || request.orderNo() == null || request.orderNo().isBlank()) {
            throw new BusinessException(30039, "支付创建缺少订单信息");
        }
        if (request.amount() == null || request.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(30005, "订单金额不合法");
        }
    }

    private Map<String, Object> queryByOrderNo(String orderNo) {
        try {
            return jdbc.queryForMap("SELECT * FROM payment_order WHERE order_no = ? AND is_deleted = 0", orderNo);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    private Map<String, Object> queryByTradeOrderId(String tradeOrderId) {
        try {
            return jdbc.queryForMap("SELECT * FROM payment_order WHERE trade_order_id = ? AND is_deleted = 0", tradeOrderId);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    private CreatePaymentResponse toResponse(Map<String, Object> row) {
        return new CreatePaymentResponse("XUNHUPAY",
                ((Number) row.get("order_id")).longValue(),
                (String) row.get("order_no"),
                (String) row.get("trade_order_id"),
                (String) row.get("pay_url"),
                (String) row.get("qr_code_url"),
                300);
    }

    private PaymentCallbackRequest toCallback(Map<String, Object> row) {
        return new PaymentCallbackRequest(
                ((Number) row.get("order_id")).longValue(),
                (String) row.get("order_no"),
                (String) row.get("trade_order_id"),
                (String) row.get("transaction_id"),
                (BigDecimal) row.get("amount"),
                "SUCCESS",
                "XUNHUPAY",
                (String) row.get("pay_url"),
                (String) row.get("qr_code_url")
        );
    }

    private PaymentStatusResponse toStatusResponse(Map<String, Object> row) {
        return new PaymentStatusResponse(
                ((Number) row.get("order_id")).longValue(),
                (String) row.get("order_no"),
                (String) row.get("trade_order_id"),
                (BigDecimal) row.get("amount"),
                ((Number) row.get("status")).intValue() == 2 ? "SUCCESS" : "PENDING",
                (String) row.get("channel"),
                (String) row.get("transaction_id"),
                (String) row.get("pay_url"),
                (String) row.get("qr_code_url"),
                row.get("paid_time") == null ? null : String.valueOf(row.get("paid_time"))
        );
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            return String.valueOf(value);
        }
    }

    private BigDecimal parseAmount(String amount) {
        try {
            return new BigDecimal(amount);
        } catch (Exception e) {
            throw new BusinessException(30011, "虎皮椒回调金额不合法");
        }
    }

    private String transactionId(Map<String, String> params) {
        String transactionId = params.get("transaction_id");
        if (transactionId != null && !transactionId.isBlank()) {
            return transactionId;
        }
        return params.get("open_order_id");
    }

    private String truncate(String value) {
        if (value == null) {
            return null;
        }
        return value.length() > 500 ? value.substring(0, 500) : value;
    }
}
