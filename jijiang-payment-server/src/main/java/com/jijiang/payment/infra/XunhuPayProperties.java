package com.jijiang.payment.infra;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "jijiang.xunhupay")
public class XunhuPayProperties {
    private String appId = "";
    private String appSecret = "";
    private String gateway = "https://api.xunhupay.com/payment/do.html";
    private String notifyUrl = "";
    private String returnUrl = "";
    private String callbackUrl = "";

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public String getAppSecret() {
        return appSecret;
    }

    public void setAppSecret(String appSecret) {
        this.appSecret = appSecret;
    }

    public String getGateway() {
        return gateway;
    }

    public void setGateway(String gateway) {
        this.gateway = gateway;
    }

    public String getNotifyUrl() {
        return notifyUrl;
    }

    public void setNotifyUrl(String notifyUrl) {
        this.notifyUrl = notifyUrl;
    }

    public String getReturnUrl() {
        return returnUrl;
    }

    public void setReturnUrl(String returnUrl) {
        this.returnUrl = returnUrl;
    }

    public String getCallbackUrl() {
        return callbackUrl;
    }

    public void setCallbackUrl(String callbackUrl) {
        this.callbackUrl = callbackUrl;
    }

    public boolean configured() {
        return hasText(appId) && hasText(appSecret) && hasText(gateway) && hasText(notifyUrl);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
