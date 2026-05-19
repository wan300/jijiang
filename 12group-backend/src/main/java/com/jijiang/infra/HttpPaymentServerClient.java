package com.jijiang.infra;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jijiang.common.BusinessException;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.Map;

@Component
class HttpPaymentServerClient implements PaymentServerClient {
    private static final String CREATE_ORDER_PATH = "/internal/payment/orders";
    private static final String QUERY_ORDER_PATH_PREFIX = "/internal/payment/orders/";

    private final PaymentServerProperties properties;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;

    HttpPaymentServerClient(PaymentServerProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.restClient = RestClient.create();
    }

    @Override
    public CreatePaymentResponse createPayment(CreatePaymentRequest request) {
        if (!properties.configured()) {
            throw new BusinessException(30020, "支付服务未配置");
        }
        try {
            String body = objectMapper.writeValueAsString(request);
            Map<String, String> headers = InternalSignatureSupport.signedHeaders(
                    properties.getClientId(),
                    properties.getSharedSecret(),
                    "POST",
                    CREATE_ORDER_PATH,
                    body
            );
            String response = restClient.post()
                    .uri(trimRight(properties.getBaseUrl()) + CREATE_ORDER_PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .headers(httpHeaders -> headers.forEach(httpHeaders::set))
                    .body(body)
                    .retrieve()
                    .body(String.class);
            return objectMapper.readValue(response == null ? "{}" : response, CreatePaymentResponse.class);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(30022, "支付服务下单失败");
        }
    }

    @Override
    public RefundResponse refundPayment(RefundRequest request) {
        if (!properties.configured()) {
            throw new BusinessException(30020, "支付服务未配置");
        }
        if (request.tradeOrderId() == null || request.tradeOrderId().isBlank()) {
            throw new BusinessException(30008, "tradeOrderId is required");
        }
        if (request.refundAmount() == null || request.refundAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(30059, "退款金额不合法");
        }
        String normalizedTradeOrderId = request.tradeOrderId().trim();
        String path = "/internal/payment/orders/" + normalizedTradeOrderId + "/refund";
        try {
            String body = objectMapper.writeValueAsString(request);
            Map<String, String> headers = InternalSignatureSupport.signedHeaders(
                    properties.getClientId(),
                    properties.getSharedSecret(),
                    "POST",
                    path,
                    body
            );
            String response = restClient.post()
                    .uri(trimRight(properties.getBaseUrl()) + path)
                    .contentType(MediaType.APPLICATION_JSON)
                    .headers(httpHeaders -> headers.forEach(httpHeaders::set))
                    .body(body)
                    .retrieve()
                    .body(String.class);
            return objectMapper.readValue(response == null ? "{}" : response, RefundResponse.class);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(30060, "退款请求失败");
        }
    }

    @Override
    public PaymentStatusResponse queryPaymentStatus(String tradeOrderId) {
        if (!properties.configured()) {
            throw new BusinessException(30020, "payment server is not configured");
        }
        if (tradeOrderId == null || tradeOrderId.isBlank()) {
            throw new BusinessException(30008, "tradeOrderId is required");
        }
        String normalizedTradeOrderId = tradeOrderId.trim();
        String path = QUERY_ORDER_PATH_PREFIX + normalizedTradeOrderId;
        try {
            Map<String, String> headers = InternalSignatureSupport.signedHeaders(
                    properties.getClientId(),
                    properties.getSharedSecret(),
                    "GET",
                    path,
                    ""
            );
            String response = restClient.get()
                    .uri(trimRight(properties.getBaseUrl()) + path)
                    .headers(httpHeaders -> headers.forEach(httpHeaders::set))
                    .retrieve()
                    .body(String.class);
            return objectMapper.readValue(response == null ? "{}" : response, PaymentStatusResponse.class);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(30023, "query payment server status failed");
        }
    }

    private String trimRight(String value) {
        if (value == null) {
            return "";
        }
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
