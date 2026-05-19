package com.jijiang.common;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "jijiang.encryption")
public class EncryptionProperties {
    private String key;
    private String pepper;

    public String key() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String pepper() {
        return pepper;
    }

    public void setPepper(String pepper) {
        this.pepper = pepper;
    }
}
