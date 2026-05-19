package com.jijiang.infra;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "jijiang.cos")
public class CosProperties {
    private String secretId;
    private String secretKey;
    private String region = "ap-guangzhou";
    private String bucket;
    private int uploadExpireSeconds = 300;

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

    public String getBucket() {
        return bucket;
    }

    public void setBucket(String bucket) {
        this.bucket = bucket;
    }

    public int getUploadExpireSeconds() {
        return uploadExpireSeconds;
    }

    public void setUploadExpireSeconds(int uploadExpireSeconds) {
        this.uploadExpireSeconds = uploadExpireSeconds;
    }

    boolean configured() {
        return secretId != null && !secretId.isBlank()
                && secretKey != null && !secretKey.isBlank()
                && bucket != null && !bucket.isBlank();
    }
}
