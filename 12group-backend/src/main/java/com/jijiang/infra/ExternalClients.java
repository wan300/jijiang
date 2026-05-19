package com.jijiang.infra;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;

public interface ExternalClients {
    interface WxLoginClient {
        WxSession code2Session(String code);
    }

    interface OcrClient {
        OcrResult recognize(String imageUrl, String realName, String studentNo);
    }

    interface ContentSafetyClient {
        void checkText(String text);
    }

    interface StorageClient {
        UploadToken generateUploadToken(String fileName, long maxSizeBytes);
    }

    record WxSession(String openid, String unionid) {
    }

    record WxPhone(String phoneNumber, String countryCode) {
    }

    record OcrResult(BigDecimal confidence, boolean passed, String raw) {
    }

    record UploadToken(String url, Map<String, String> fields, String fileKey, long expiresAt) {
    }
}

@Component
class MockContentSafetyClient implements ExternalClients.ContentSafetyClient {
    @Override
    public void checkText(String text) {
        // Real providers can be plugged in behind this interface later.
    }
}
