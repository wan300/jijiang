package com.jijiang.payment.infra;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

public final class XunhuPaySigner {
    private XunhuPaySigner() {
    }

    public static Map<String, Object> withHash(Map<String, ?> params, String appSecret) {
        Map<String, Object> signed = new LinkedHashMap<>();
        for (Map.Entry<String, ?> entry : params.entrySet()) {
            if (entry.getValue() != null) {
                signed.put(entry.getKey(), entry.getValue());
            }
        }
        signed.put("hash", sign(signed, appSecret));
        return signed;
    }

    public static String sign(Map<String, ?> params, String appSecret) {
        return md5(canonicalString(params) + appSecret);
    }

    public static boolean verify(Map<String, ?> params, String appSecret) {
        Object provided = params.get("hash");
        if (provided == null || String.valueOf(provided).isBlank()) {
            return false;
        }
        return sign(params, appSecret).equalsIgnoreCase(String.valueOf(provided));
    }

    public static String canonicalString(Map<String, ?> params) {
        TreeMap<String, String> sorted = new TreeMap<>();
        for (Map.Entry<String, ?> entry : params.entrySet()) {
            String key = entry.getKey();
            Object rawValue = entry.getValue();
            if (key == null || "hash".equals(key) || rawValue == null) {
                continue;
            }
            String value = String.valueOf(rawValue);
            if (!value.isBlank()) {
                sorted.put(key, value);
            }
        }
        StringBuilder builder = new StringBuilder();
        for (Map.Entry<String, String> entry : sorted.entrySet()) {
            if (builder.length() > 0) {
                builder.append('&');
            }
            builder.append(entry.getKey()).append('=').append(entry.getValue());
        }
        return builder.toString();
    }

    private static String md5(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            byte[] bytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (Exception e) {
            throw new IllegalStateException("MD5 algorithm is not available", e);
        }
    }
}
