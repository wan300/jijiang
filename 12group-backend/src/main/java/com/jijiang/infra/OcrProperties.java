package com.jijiang.infra;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "jijiang.ocr")
public class OcrProperties {
    private String secretId;
    private String secretKey;
    private String region = "ap-guangzhou";

    public String getSecretId() {
        return secretId;
    }

    public void setSecretId(String secretId) {
        this.secretId = secretId;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    boolean configured() {
        return secretId != null && !secretId.isBlank() && secretKey != null && !secretKey.isBlank();
    }
}
