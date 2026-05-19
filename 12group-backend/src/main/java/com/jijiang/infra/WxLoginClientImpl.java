package com.jijiang.infra;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

@Component
class WxLoginClientImpl implements ExternalClients.WxLoginClient {
    private static final Logger log = LoggerFactory.getLogger(WxLoginClientImpl.class);
    private static final String CODE2SESSION_URL = "https://api.weixin.qq.com/sns/jscode2session?appid={appid}&secret={secret}&js_code={code}&grant_type=authorization_code";
    private static final String SESSION_KEY_PREFIX = "wx:session:";

    private final WxProperties wxProperties;
    private final StringRedisTemplate redisTemplate;
    private final RestClient restClient;

    WxLoginClientImpl(WxProperties wxProperties, StringRedisTemplate redisTemplate, RestClient.Builder restClientBuilder) {
        this.wxProperties = wxProperties;
        this.redisTemplate = redisTemplate;
        this.restClient = restClientBuilder.build();
    }

    @Override
    public ExternalClients.WxSession code2Session(String code) {
        if (!wxProperties.configured()) {
            return mockCode2Session(code);
        }
        String stableCode = code == null || code.isBlank() ? UUID.randomUUID().toString() : code.trim();
        try {
            @SuppressWarnings("unchecked")
            var result = restClient.get()
                    .uri(CODE2SESSION_URL, wxProperties.getAppId(), wxProperties.getAppSecret(), stableCode)
                    .retrieve()
                    .body(Map.class);
            if (result == null) {
                log.warn("微信 code2Session 返回空, code={}", stableCode);
                return mockCode2Session(code);
            }
            Object errcode = result.get("errcode");
            if (errcode != null && errcode instanceof Number && ((Number) errcode).intValue() != 0) {
                log.warn("微信 code2Session 错误: errcode={}, errmsg={}", errcode, result.get("errmsg"));
                return mockCode2Session(code);
            }
            String openid = (String) result.get("openid");
            String unionid = (String) result.get("unionid");
            String sessionKey = (String) result.get("session_key");
            if (openid != null && sessionKey != null) {
                redisTemplate.opsForValue().set(SESSION_KEY_PREFIX + openid, sessionKey, Duration.ofHours(2));
            }
            if (openid == null) {
                log.warn("微信 code2Session 未返回openid, 使用mock降级");
                return mockCode2Session(code);
            }
            return new ExternalClients.WxSession(openid, unionid);
        } catch (Exception e) {
            log.error("微信 code2Session 调用失败, 使用mock降级", e);
            return mockCode2Session(code);
        }
    }

    String getSessionKey(String openid) {
        if (openid == null) {
            return null;
        }
        return redisTemplate.opsForValue().get(SESSION_KEY_PREFIX + openid);
    }

    private ExternalClients.WxSession mockCode2Session(String code) {
        String stableCode = code == null || code.isBlank() ? UUID.randomUUID().toString() : code.trim();
        return new ExternalClients.WxSession("mock-openid-" + stableCode, null);
    }
}
