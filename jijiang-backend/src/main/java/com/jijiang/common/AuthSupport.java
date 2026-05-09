package com.jijiang.common;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;

@Component
public class AuthSupport {
    private final String secret;
    private final long ttlSeconds;

    public AuthSupport(@Value("${jijiang.jwt.secret}") String secret,
                       @Value("${jijiang.jwt.access-token-ttl-seconds}") long ttlSeconds) {
        this.secret = secret;
        this.ttlSeconds = ttlSeconds;
    }

    public String issue(Long userId, Integer role, Long campusId) {
        long expiresAt = Instant.now().plusSeconds(ttlSeconds).getEpochSecond();
        String payload = userId + ":" + role + ":" + campusId + ":" + expiresAt;
        return base64(payload) + "." + sign(payload);
    }

    public UserContext parseBearer(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new BusinessException(10001, "请先登录");
        }
        try {
            String token = authorization.substring("Bearer ".length());
            String[] parts = token.split("\\.");
            if (parts.length != 2) {
                throw new BusinessException(10001, "登录已失效");
            }
            String payload = new String(Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8);
            if (!sign(payload).equals(parts[1])) {
                throw new BusinessException(10001, "登录已失效");
            }
            String[] fields = payload.split(":");
            long expiresAt = Long.parseLong(fields[3]);
            if (Instant.now().getEpochSecond() > expiresAt) {
                throw new BusinessException(10001, "登录已过期");
            }
            return new UserContext(Long.parseLong(fields[0]), Integer.parseInt(fields[1]), Long.parseLong(fields[2]));
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(10001, "登录已失效");
        }
    }

    private String base64(String value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private String sign(String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("Cannot sign token", e);
        }
    }
}
