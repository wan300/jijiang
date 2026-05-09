package com.jijiang.infra;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "jijiang.payment-server")
public class PaymentServerProperties {
    private String baseUrl = "";
    private String clientId = "jijiang-app";
    private String sharedSecret = "";

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getSharedSecret() {
        return sharedSecret;
    }

    public void setSharedSecret(String sharedSecret) {
        this.sharedSecret = sharedSecret;
    }

    public boolean configured() {
        return hasText(baseUrl) && hasText(clientId) && hasText(sharedSecret);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
