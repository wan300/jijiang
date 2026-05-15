package com.jijiang.payment.infra;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "jijiang.app-server")
public class AppServerProperties {
    private boolean callbackEnabled = true;
    private String callbackUrl = "";
    private String clientId = "jijiang-app";
    private String sharedSecret = "";

    public boolean isCallbackEnabled() {
        return callbackEnabled;
    }

    public void setCallbackEnabled(boolean callbackEnabled) {
        this.callbackEnabled = callbackEnabled;
    }

    public String getCallbackUrl() {
        return callbackUrl;
    }

    public void setCallbackUrl(String callbackUrl) {
        this.callbackUrl = callbackUrl;
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
        return hasText(callbackUrl) && hasText(clientId) && hasText(sharedSecret);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
