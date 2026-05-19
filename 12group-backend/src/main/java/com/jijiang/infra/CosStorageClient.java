package com.jijiang.infra;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.http.HttpMethodName;
import com.qcloud.cos.http.HttpProtocol;
import com.qcloud.cos.model.GeneratePresignedUrlRequest;
import com.qcloud.cos.region.Region;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

@Component
public class CosStorageClient {
    private static final Logger log = LoggerFactory.getLogger(CosStorageClient.class);

    private final CosProperties cosProperties;

    public CosStorageClient(CosProperties cosProperties) {
        this.cosProperties = cosProperties;
    }

    public ExternalClients.UploadToken generateUploadToken(String fileName, long maxSizeBytes) {
        if (!cosProperties.configured()) {
            return mockToken(fileName);
        }
        try {
            COSCredentials cred = new BasicCOSCredentials(cosProperties.getSecretId(), cosProperties.getSecretKey());
            ClientConfig clientConfig = new ClientConfig(new Region(cosProperties.getRegion()));
            clientConfig.setHttpProtocol(HttpProtocol.https);
            COSClient cosClient = new COSClient(cred, clientConfig);

            String fileKey = "verify/" + UUID.randomUUID().toString().replace("-", "") + "/" + sanitize(fileName);
            Date expiration = Date.from(Instant.now().plusSeconds(cosProperties.getUploadExpireSeconds()));

            GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(
                    cosProperties.getBucket(), fileKey, HttpMethodName.PUT);
            request.setExpiration(expiration);
            request.addRequestParameter("x-oss-content-type", "image/jpeg");
            if (maxSizeBytes > 0) {
                request.addRequestParameter("x-oss-content-length", String.valueOf(maxSizeBytes));
            }

            URL url = cosClient.generatePresignedUrl(request);
            cosClient.shutdown();

            long expiresAt = Instant.now().plusSeconds(cosProperties.getUploadExpireSeconds()).getEpochSecond();
            return new ExternalClients.UploadToken(url.toString(), Map.of("key", fileKey), fileKey, expiresAt);
        } catch (Exception e) {
            log.error("COS预签名URL生成失败, 降级mock", e);
            return mockToken(fileName);
        }
    }

    private ExternalClients.UploadToken mockToken(String fileName) {
        String fileKey = "verify/" + UUID.randomUUID().toString().replace("-", "") + "/" + sanitize(fileName);
        return new ExternalClients.UploadToken(
                "https://mock-cos.example.com/" + fileKey,
                Map.of("key", fileKey),
                fileKey,
                System.currentTimeMillis() / 1000 + cosProperties.getUploadExpireSeconds());
    }

    private String sanitize(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return "upload.jpg";
        }
        return fileName.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
