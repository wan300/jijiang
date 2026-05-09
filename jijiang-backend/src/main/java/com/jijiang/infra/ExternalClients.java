package com.jijiang.infra;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

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

    record WxSession(String openid, String unionid) {
    }

    record OcrResult(BigDecimal confidence, boolean passed, String raw) {
    }

}

@Component
class MockWxLoginClient implements ExternalClients.WxLoginClient {
    @Override
    public ExternalClients.WxSession code2Session(String code) {
        String stableCode = code == null || code.isBlank() ? UUID.randomUUID().toString() : code.trim();
        return new ExternalClients.WxSession("mock-openid-" + stableCode, null);
    }
}

@Component
class MockOcrClient implements ExternalClients.OcrClient {
    @Override
    public ExternalClients.OcrResult recognize(String imageUrl, String realName, String studentNo) {
        boolean passed = realName != null && !realName.isBlank() && studentNo != null && !studentNo.isBlank();
        BigDecimal confidence = passed ? new BigDecimal("0.9000") : new BigDecimal("0.5000");
        return new ExternalClients.OcrResult(confidence, passed, "{\"provider\":\"mock\"}");
    }
}

@Component
class MockContentSafetyClient implements ExternalClients.ContentSafetyClient {
    @Override
    public void checkText(String text) {
        // Real providers can be plugged in behind this interface later.
    }
}
