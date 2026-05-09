package com.jijiang.payment.infra;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.jijiang.payment.common.BusinessException;
import com.jijiang.payment.modules.PaymentCallbackRequest;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.util.Map;

@Component
public class AppServerClient {
    private final AppServerProperties properties;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;

    AppServerClient(AppServerProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.restClient = RestClient.create();
    }

    public void notifyPaid(PaymentCallbackRequest request) {
        if (!properties.configured()) {
            throw new BusinessException(30040, "应用服务器回调未配置");
        }
        try {
            String body = objectMapper.writeValueAsString(request);
            URI uri = URI.create(properties.getCallbackUrl());
            String path = uri.getRawPath();
            Map<String, String> headers = InternalSignatureSupport.signedHeaders(
                    properties.getClientId(),
                    properties.getSharedSecret(),
                    "POST",
                    path,
                    body
            );
            String response = restClient.post()
                    .uri(properties.getCallbackUrl())
                    .contentType(MediaType.APPLICATION_JSON)
                    .headers(httpHeaders -> headers.forEach(httpHeaders::set))
                    .body(body)
                    .retrieve()
                    .body(String.class);
            JsonNode root = objectMapper.readTree(response == null ? "{}" : response);
            if (root.has("code") && root.path("code").asInt(-1) != 0) {
                throw new BusinessException(30041, "应用服务器回调失败：" + root.path("message").asText("unknown"));
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(30041, "应用服务器回调失败");
        }
    }
}
