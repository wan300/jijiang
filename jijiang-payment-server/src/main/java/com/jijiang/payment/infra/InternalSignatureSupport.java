package com.jijiang.payment.infra;

import com.jijiang.payment.common.BusinessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpHeaders;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public final class InternalSignatureSupport {
    public static final String CLIENT_ID = "X-JJ-Client-Id";
    public static final String TIMESTAMP = "X-JJ-Timestamp";
    public static final String NONCE = "X-JJ-Nonce";
    public static final String SIGNATURE = "X-JJ-Signature";

    private static final String HMAC_SHA256 = "HmacSHA256";
    private static final ConcurrentHashMap<String, Long> LOCAL_NONCES = new ConcurrentHashMap<>();

    private InternalSignatureSupport() {
    }

    public static Map<String, String> signedHeaders(String clientId, String secret, String method, String path, String body) {
        String timestamp = String.valueOf(Instant.now().getEpochSecond());
        String nonce = java.util.UUID.randomUUID().toString().replace("-", "");
        return Map.of(
                CLIENT_ID, clientId,
                TIMESTAMP, timestamp,
                NONCE, nonce,
                SIGNATURE, sign(secret, timestamp, nonce, method, path, body)
        );
    }

    public static void verify(String expectedClientId, String secret, String method, String path, String body,
                              HttpHeaders headers, StringRedisTemplate redisTemplate, Duration allowedSkew) {
        String clientId = headers.getFirst(CLIENT_ID);
        String timestamp = headers.getFirst(TIMESTAMP);
        String nonce = headers.getFirst(NONCE);
        String providedSignature = headers.getFirst(SIGNATURE);
        if (!Objects.equals(expectedClientId, clientId)) {
            throw new BusinessException(30031, "内部支付请求来源不可信");
        }
        if (secret == null || secret.isBlank()) {
            throw new BusinessException(30032, "内部支付签名密钥未配置");
        }
        if (timestamp == null || nonce == null || nonce.isBlank() || providedSignature == null || providedSignature.isBlank()) {
            throw new BusinessException(30033, "内部支付签名头不完整");
        }
        long requestEpoch;
        try {
            requestEpoch = Long.parseLong(timestamp);
        } catch (NumberFormatException e) {
            throw new BusinessException(30034, "内部支付时间戳不合法");
        }
        if (Math.abs(Instant.now().getEpochSecond() - requestEpoch) > allowedSkew.toSeconds()) {
            throw new BusinessException(30035, "内部支付请求已过期");
        }
        String expectedSignature = sign(secret, timestamp, nonce, method, path, body);
        if (!MessageDigest.isEqual(expectedSignature.getBytes(StandardCharsets.UTF_8),
                providedSignature.getBytes(StandardCharsets.UTF_8))) {
            throw new BusinessException(30036, "内部支付签名错误");
        }
        assertFreshNonce(clientId, nonce, redisTemplate, allowedSkew);
    }

    public static String sign(String secret, String timestamp, String nonce, String method, String path, String body) {
        return hmacSha256(secret, canonical(timestamp, nonce, method, path, body));
    }

    public static String canonical(String timestamp, String nonce, String method, String path, String body) {
        return timestamp + "\n" + nonce + "\n" + method.toUpperCase() + "\n" + path + "\n" + sha256Hex(body == null ? "" : body);
    }

    public static String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 algorithm is not available", e);
        }
    }

    private static String hmacSha256(String secret, String value) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256));
            return HexFormat.of().formatHex(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("HmacSHA256 algorithm is not available", e);
        }
    }

    private static void assertFreshNonce(String clientId, String nonce, StringRedisTemplate redisTemplate, Duration ttl) {
        String key = "internal:pay:nonce:" + clientId + ":" + nonce;
        if (redisTemplate != null) {
            try {
                Boolean inserted = redisTemplate.opsForValue().setIfAbsent(key, "1", ttl.plusSeconds(60));
                if (Boolean.FALSE.equals(inserted)) {
                    throw new BusinessException(30037, "内部支付请求重复");
                }
                return;
            } catch (BusinessException e) {
                throw e;
            } catch (Exception ignored) {
                throw new BusinessException(30038, "internal payment nonce store is unavailable");
            }
        }
        long expiresAt = Instant.now().plus(ttl).plusSeconds(60).getEpochSecond();
        LOCAL_NONCES.entrySet().removeIf(entry -> entry.getValue() < Instant.now().getEpochSecond());
        Long previous = LOCAL_NONCES.putIfAbsent(key, expiresAt);
        if (previous != null) {
            throw new BusinessException(30037, "内部支付请求重复");
        }
    }
}
