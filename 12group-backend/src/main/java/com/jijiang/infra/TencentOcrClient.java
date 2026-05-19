package com.jijiang.infra;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.http.HttpProtocol;
import com.qcloud.cos.model.GetObjectRequest;
import com.qcloud.cos.region.Region;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Map;

@Component
class TencentOcrClient implements ExternalClients.OcrClient {
    private static final Logger log = LoggerFactory.getLogger(TencentOcrClient.class);
    private static final String ENDPOINT = "ocr.tencentcloudapi.com";
    private static final String SERVICE = "ocr";
    private static final String ACTION = "GeneralAccurateOCR";
    private static final String VERSION = "2018-11-19";

    private final OcrProperties ocrProperties;
    private final CosProperties cosProperties;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    TencentOcrClient(OcrProperties ocrProperties, CosProperties cosProperties,
                     RestClient.Builder builder, ObjectMapper objectMapper) {
        this.ocrProperties = ocrProperties;
        this.cosProperties = cosProperties;
        this.restClient = builder.build();
        this.objectMapper = objectMapper;
    }

    @Override
    public ExternalClients.OcrResult recognize(String imageUrl, String realName, String studentNo) {
        if (!ocrProperties.configured()) {
            return mockRecognize(realName, studentNo);
        }
        try {
            // 用COS SDK下载图片再base64传给OCR
            byte[] imageBytes;
            String cosHost = cosProperties.getBucket() + ".cos." + cosProperties.getRegion() + ".myqcloud.com";
            if (imageUrl.contains(cosHost) && cosProperties.configured()) {
                imageBytes = downloadFromCos(imageUrl);
            } else {
                imageBytes = restClient.get().uri(imageUrl).retrieve().body(byte[].class);
            }
            String base64 = java.util.Base64.getEncoder().encodeToString(imageBytes);
            String payload = objectMapper.writeValueAsString(Map.of("ImageBase64", base64));
            String json = callTencentOcr(payload);

            JsonNode root = objectMapper.readTree(json);
            JsonNode resp = root.path("Response");
            if (resp.has("Error")) {
                log.warn("OCR返回错误: {}", resp.path("Error").path("Message").asText());
                return mockRecognize(realName, studentNo);
            }

            JsonNode detections = resp.path("TextDetections");
            if (!detections.isArray() || detections.isEmpty()) {
                return new ExternalClients.OcrResult(
                        new BigDecimal("0"), false, json);
            }

            int count = detections.size();
            long totalConf = 0;
            boolean nameMatched = false;
            boolean noMatched = false;
            StringBuilder raw = new StringBuilder();

            for (int i = 0; i < count; i++) {
                JsonNode det = detections.get(i);
                String text = det.path("DetectedText").asText("");
                int conf = det.path("Confidence").asInt(0);
                totalConf += conf;
                raw.append(text).append("|");
                if (text.contains(realName)) nameMatched = true;
                if (text.contains(studentNo)) noMatched = true;
            }

            BigDecimal avgConf = BigDecimal.valueOf(totalConf)
                    .divide(BigDecimal.valueOf(count), 4, RoundingMode.HALF_UP);
            boolean passed = nameMatched && noMatched && avgConf.compareTo(new BigDecimal("90")) >= 0;

            return new ExternalClients.OcrResult(
                    avgConf.divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP), passed, json);
        } catch (Exception e) {
            log.error("腾讯云OCR调用失败, 降级mock", e);
            return mockRecognize(realName, studentNo);
        }
    }

    private byte[] downloadFromCos(String imageUrl) throws Exception {
        String prefix = "https://" + cosProperties.getBucket() + ".cos." + cosProperties.getRegion() + ".myqcloud.com/";
        if (!imageUrl.startsWith(prefix)) throw new RuntimeException("非COS URL: " + imageUrl);
        String key = imageUrl.substring(prefix.length());
        COSCredentials cred = new BasicCOSCredentials(cosProperties.getSecretId(), cosProperties.getSecretKey());
        ClientConfig config = new ClientConfig(new Region(cosProperties.getRegion()));
        config.setHttpProtocol(HttpProtocol.https);
        COSClient cosClient = new COSClient(cred, config);
        var object = cosClient.getObject(new GetObjectRequest(cosProperties.getBucket(), key));
        byte[] bytes = object.getObjectContent().readAllBytes();
        cosClient.shutdown();
        return bytes;
    }

    private String callTencentOcr(String payload) throws Exception {
        String timestamp = String.valueOf(Instant.now().getEpochSecond());
        String date = DateTimeFormatter.ofPattern("yyyy-MM-dd")
                .withZone(ZoneOffset.UTC).format(Instant.now());

        // TC3-HMAC-SHA256 signing
        String canonicalHeaders = "content-type:application/json\nhost:" + ENDPOINT + "\n";
        String signedHeaders = "content-type;host";
        String hashedPayload = sha256Hex(payload);
        String canonicalRequest = "POST\n/\n\n" + canonicalHeaders + "\n" + signedHeaders + "\n" + hashedPayload;
        String credentialScope = date + "/" + SERVICE + "/tc3_request";
        String stringToSign = "TC3-HMAC-SHA256\n" + timestamp + "\n" + credentialScope + "\n" + sha256Hex(canonicalRequest);

        byte[] secretDate = hmacSha256(("TC3" + ocrProperties.getSecretKey()).getBytes(StandardCharsets.UTF_8), date);
        byte[] secretService = hmacSha256(secretDate, SERVICE);
        byte[] secretSigning = hmacSha256(secretService, "tc3_request");
        String signature = HexFormat.of().formatHex(hmacSha256(secretSigning, stringToSign));

        String authorization = "TC3-HMAC-SHA256 Credential=" + ocrProperties.getSecretId() + "/" + credentialScope
                + ", SignedHeaders=" + signedHeaders + ", Signature=" + signature;

        String result = restClient.post()
                .uri("https://" + ENDPOINT)
                .header("Authorization", authorization)
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .header("Host", ENDPOINT)
                .header("X-TC-Action", ACTION)
                .header("X-TC-Version", VERSION)
                .header("X-TC-Timestamp", timestamp)
                .body(payload)
                .retrieve()
                .body(String.class);
        return result;
    }

    private String sha256Hex(String data) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        return HexFormat.of().formatHex(md.digest(data.getBytes(StandardCharsets.UTF_8)));
    }

    private byte[] hmacSha256(byte[] key, String data) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(key, "HmacSHA256"));
        return mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
    }

    private ExternalClients.OcrResult mockRecognize(String realName, String studentNo) {
        boolean passed = realName != null && !realName.isBlank() && studentNo != null && !studentNo.isBlank();
        BigDecimal confidence = passed ? new BigDecimal("0.9000") : new BigDecimal("0.5000");
        return new ExternalClients.OcrResult(confidence, passed, "{\"provider\":\"mock\"}");
    }
}
