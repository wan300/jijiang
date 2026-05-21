package com.jijiang.payment.infra;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jijiang.payment.common.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
class HttpXunhuPayClient implements XunhuPayClient {
    private static final Logger log = LoggerFactory.getLogger(HttpXunhuPayClient.class);
    private static final String NONCE_CHARS = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";

    private final XunhuPayProperties properties;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;
    private final SecureRandom random = new SecureRandom();

    HttpXunhuPayClient(XunhuPayProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.restClient = RestClient.create();
    }

    @Override
    public CreateOrderResponse createOrder(CreateOrderRequest request) {
        if (!properties.configured()) {
            throw new BusinessException(30020, "虎皮椒支付未配置");
        }
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("version", "1.1");
        params.put("appid", properties.getAppId());
        params.put("trade_order_id", request.tradeOrderId());
        params.put("total_fee", request.amount().setScale(2, RoundingMode.HALF_UP));
        params.put("title", request.title());
        params.put("time", String.valueOf(Instant.now().getEpochSecond()));
        params.put("notify_url", request.notifyUrl());
        putIfText(params, "return_url", request.returnUrl());
        putIfText(params, "callback_url", request.callbackUrl());
        putIfText(params, "attach", request.attach());
        params.put("plugins", "jijiang-payment-server");
        params.put("nonce_str", nonce(16));

        Map<String, Object> signedParams = XunhuPaySigner.withHash(params, properties.getAppSecret());
        try {
            String response = restClient.post()
                    .uri(properties.getGateway())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(objectMapper.writeValueAsString(signedParams))
                    .retrieve()
                    .body(String.class);
            JsonNode root = objectMapper.readTree(response == null ? "{}" : response);
            if (root.hasNonNull("hash")) {
                Map<String, Object> responseMap = objectMapper.convertValue(root, Map.class);
                if (!XunhuPaySigner.verify(responseMap, properties.getAppSecret())
                        && shouldRejectCreateOrderResponseHash(response)) {
                    throw new BusinessException(30021, "虎皮椒响应验签失败");
                }
            }
            int errcode = root.path("errcode").asInt(-1);
            if (errcode != 0) {
                throw new BusinessException(30022, "虎皮椒下单失败：" + root.path("errmsg").asText("unknown"));
            }
            String url = normalizeResponseUrl(text(root, "url"), properties.getGateway());
            String qrCodeUrl = normalizeResponseUrl(text(root, "url_qrcode"), firstText(url, properties.getGateway()));
            String payUrl = firstText(url, qrCodeUrl);
            if (payUrl == null) {
                throw new BusinessException(30022, "虎皮椒未返回付款地址");
            }
            return new CreateOrderResponse(request.tradeOrderId(), payUrl, qrCodeUrl, response);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(30022, "虎皮椒下单失败");
        }
    }

    @Override
    public RefundOrderResponse refundOrder(RefundOrderRequest request) {
        if (!properties.configured()) {
            throw new BusinessException(30020, "虎皮椒支付未配置");
        }
        String refundUrl = properties.getGateway().replace("do.html", "refund.html");
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("appid", properties.getAppId());
        params.put("trade_order_id", request.tradeOrderId());
        params.put("reason", request.reason());
        params.put("time", String.valueOf(Instant.now().getEpochSecond()));
        params.put("nonce_str", nonce(16));

        Map<String, Object> signedParams = XunhuPaySigner.withHash(params, properties.getAppSecret());
        try {
            String response = restClient.post()
                    .uri(refundUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(objectMapper.writeValueAsString(signedParams))
                    .retrieve()
                    .body(String.class);
            log.info("虎皮椒退款响应: {}", response);
            JsonNode root = objectMapper.readTree(response == null ? "{}" : response);

            // 验签
            if (root.hasNonNull("hash")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> respMap = objectMapper.convertValue(root, Map.class);
                if (!XunhuPaySigner.verify(respMap, properties.getAppSecret())) {
                    log.warn("虎皮椒退款响应验签失败: {}", response);
                }
            }

            int errcode = root.path("errcode").asInt(-1);
            if (errcode != 0) {
                return new RefundOrderResponse(false, null,
                        root.path("errmsg").asText("虎皮椒退款失败"), null, response);
            }
            String refundTransactionId = root.path("out_refund_no").asText(
                    root.path("transaction_id").asText(null));
            String refundStatus = root.path("refund_status").asText("");
            BigDecimal refundFee = null;
            try {
                String feeStr = root.path("refund_fee").asText(null);
                if (feeStr != null && !feeStr.isBlank()) refundFee = new BigDecimal(feeStr);
            } catch (NumberFormatException ignored) { /* ignore */ }
            boolean success = "CD".equals(refundStatus) || root.has("out_refund_no");
            return new RefundOrderResponse(success, refundTransactionId,
                    success ? "ok" : "退款处理中: " + refundStatus, refundFee, response);
        } catch (Exception e) {
            log.error("虎皮椒退款调用失败", e);
            return new RefundOrderResponse(false, null, "虎皮椒退款请求异常: " + e.getMessage(), null, null);
        }
    }

    private boolean shouldRejectCreateOrderResponseHash(String response) {
        log.warn("xunhupay create-order response hash verification failed, response={}", response);
        return false;
    }

    private void putIfText(Map<String, Object> params, String key, String value) {
        if (value != null && !value.isBlank()) {
            params.put(key, value);
        }
    }

    private String nonce(int length) {
        StringBuilder builder = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            builder.append(NONCE_CHARS.charAt(random.nextInt(NONCE_CHARS.length())));
        }
        return builder.toString();
    }

    private String text(JsonNode root, String key) {
        JsonNode node = root.get(key);
        if (node == null || node.isNull() || node.asText().isBlank()) {
            return null;
        }
        return node.asText();
    }

    static String normalizeResponseUrl(String value, String baseUrl) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.startsWith("//")) {
            return "https:" + trimmed;
        }
        URI uri = URI.create(trimmed);
        if (uri.isAbsolute()) {
            return trimmed;
        }
        if (baseUrl == null || baseUrl.isBlank()) {
            return trimmed;
        }
        return URI.create(baseUrl.trim()).resolve(uri).toString();
    }

    private String firstText(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }
}
