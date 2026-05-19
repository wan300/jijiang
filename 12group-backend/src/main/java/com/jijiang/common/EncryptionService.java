package com.jijiang.common;

import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Base64;

@Component
public class EncryptionService {
    private static final String AES_GCM = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH = 128;
    private static final int GCM_IV_LENGTH = 12;
    private static final int AES_KEY_SIZE = 32;

    private final SecretKey aesKey;
    private final byte[] hmacKey;
    private final SecureRandom secureRandom;

    public EncryptionService(EncryptionProperties properties) {
        if (properties.key() == null || properties.key().isBlank()) {
            this.aesKey = null;
            this.hmacKey = null;
            this.secureRandom = null;
            return;
        }
        byte[] rawKey = Base64.getDecoder().decode(properties.key().trim());
        if (rawKey.length < AES_KEY_SIZE) {
            throw new IllegalArgumentException("ENCRYPTION_KEY must be at least 32 bytes (base64 encoded)");
        }
        this.aesKey = new SecretKeySpec(rawKey, 0, AES_KEY_SIZE, "AES");
        this.hmacKey = properties.pepper() != null && !properties.pepper().isBlank()
                ? properties.pepper().getBytes(java.nio.charset.StandardCharsets.UTF_8)
                : rawKey;
        this.secureRandom = new SecureRandom();
    }

    public String encrypt(String plaintext) {
        if (plaintext == null || plaintext.isBlank()) {
            return plaintext;
        }
        if (aesKey == null) {
            return plaintext;
        }
        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            secureRandom.nextBytes(iv);
            Cipher cipher = Cipher.getInstance(AES_GCM);
            cipher.init(Cipher.ENCRYPT_MODE, aesKey, new GCMParameterSpec(GCM_TAG_LENGTH, iv));
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            byte[] combined = new byte[iv.length + ciphertext.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(ciphertext, 0, combined, iv.length, ciphertext.length);
            return "ENC:" + Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            throw new RuntimeException("encrypt failed", e);
        }
    }

    public String decrypt(String ciphertext) {
        if (ciphertext == null || ciphertext.isBlank()) {
            return ciphertext;
        }
        if (!ciphertext.startsWith("ENC:")) {
            return ciphertext;
        }
        if (aesKey == null) {
            return ciphertext.substring(4);
        }
        try {
            byte[] combined = Base64.getDecoder().decode(ciphertext.substring(4));
            byte[] iv = new byte[GCM_IV_LENGTH];
            byte[] encrypted = new byte[combined.length - GCM_IV_LENGTH];
            System.arraycopy(combined, 0, iv, 0, GCM_IV_LENGTH);
            System.arraycopy(combined, GCM_IV_LENGTH, encrypted, 0, encrypted.length);
            Cipher cipher = Cipher.getInstance(AES_GCM);
            cipher.init(Cipher.DECRYPT_MODE, aesKey, new GCMParameterSpec(GCM_TAG_LENGTH, iv));
            return new String(cipher.doFinal(encrypted), java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("decrypt failed", e);
        }
    }

    public String hashForLookup(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        if (hmacKey == null) {
            return value;
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(hmacKey, "HmacSHA256"));
            byte[] hash = mac.doFinal(value.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException("hash failed", e);
        }
    }
}
