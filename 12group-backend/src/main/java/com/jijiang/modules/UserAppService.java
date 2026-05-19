package com.jijiang.modules;

import com.jijiang.common.BusinessException;
import com.jijiang.common.UserContext;
import com.jijiang.common.EncryptionService;
import com.jijiang.infra.CosStorageClient;
import com.jijiang.infra.ExternalClients;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
class UserAppService {
    private final JdbcTemplate jdbc;
    private final ExternalClients.OcrClient ocrClient;
    private final EncryptionService encryptionService;
    private final CosStorageClient cosStorageClient;
    private final VerifyAuditService verifyAuditService;

    UserAppService(JdbcTemplate jdbc, ExternalClients.OcrClient ocrClient, EncryptionService encryptionService,
                   CosStorageClient cosStorageClient, VerifyAuditService verifyAuditService) {
        this.jdbc = jdbc;
        this.ocrClient = ocrClient;
        this.encryptionService = encryptionService;
        this.cosStorageClient = cosStorageClient;
        this.verifyAuditService = verifyAuditService;
    }

    ExternalClients.UploadToken generateUploadToken(String fileName, long maxSizeBytes) {
        return cosStorageClient.generateUploadToken(fileName, maxSizeBytes);
    }

    Map<String, Object> submitVerify(UserContext ctx, VerifySubmitRequest request) {
        String nameHash = encryptionService.hashForLookup(request.realName());
        String certNoHash = encryptionService.hashForLookup(request.studentNo());
        checkDuplicateCertificate(nameHash, certNoHash, ctx.userId());

        var ocr = ocrClient.recognize(request.certImageUrl(), request.realName(), request.studentNo());
        double threshold = 0.95;
        boolean autoPass = ocr.passed() && ocr.confidence() != null
                && ocr.confidence().doubleValue() >= threshold;
        int status = autoPass ? 2 : 1;
        int reviewMode = autoPass ? 1 : 2;
        String encryptedName = encryptionService.encrypt(request.realName());
        String encryptedNo = encryptionService.encrypt(request.studentNo());
        Long recordId = JdbcHelper.insertAndReturnId(jdbc, """
            INSERT INTO user_verify_record
            (user_id, campus_id, cert_type, cert_image_url, real_name, student_no,
             real_name_hash, cert_no_hash, ocr_result, ocr_confidence, status, review_mode)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """, ctx.userId(), request.campusId(), request.certType(), request.certImageUrl(), encryptedName,
            encryptedNo, nameHash, certNoHash, ocr.raw(), ocr.confidence(), status, reviewMode);

        verifyAuditService.log(ctx.userId(), recordId, "SUBMIT", "提交实名认证", null);
        verifyAuditService.log(ctx.userId(), recordId, "OCR_COMPLETE",
                "OCR识别完成, 置信度=" + ocr.confidence() + ", 通过=" + ocr.passed(), null);

        if (status == 2) {
            verifyAuditService.log(ctx.userId(), recordId, "AUTO_PASS",
                    "自动通过 (置信度 >= " + threshold + ")", null);
            jdbc.update("""
                UPDATE `user`
                SET verify_status = 2, is_seller_verified = 1, campus_id = ?, real_name_encrypted = ?, student_no_encrypted = ?
                WHERE id = ?
                """, request.campusId(), encryptedName, encryptedNo, ctx.userId());
            ensureSellerAccount(ctx.userId());
        } else {
            verifyAuditService.log(ctx.userId(), recordId, "PENDING_MANUAL",
                    "进入人工审核", null);
            jdbc.update("UPDATE `user` SET verify_status = 1, campus_id = ? WHERE id = ?", request.campusId(), ctx.userId());
        }
        return Map.of("recordId", recordId, "status", status, "reviewMode", reviewMode,
                "message", status == 2 ? "自动认证通过" : "已进入人工审核");
    }

    Map<String, Object> verifyStatus(UserContext ctx) {
        return jdbc.queryForMap("SELECT verify_status AS status, is_seller_verified AS isSellerVerified FROM `user` WHERE id = ?", ctx.userId());
    }

    void ensureSellerAccount(Long sellerId) {
        Integer exists = jdbc.queryForObject("SELECT COUNT(*) FROM seller_account WHERE seller_id = ?", Integer.class, sellerId);
        if (exists == null || exists == 0) {
            jdbc.update("INSERT INTO seller_account (seller_id) VALUES (?)", sellerId);
        }
    }

    private void checkDuplicateCertificate(String nameHash, String certNoHash, Long userId) {
        if (nameHash == null || certNoHash == null) {
            return;
        }
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM user_verify_record WHERE real_name_hash = ? AND cert_no_hash = ? AND status = 2 AND user_id <> ? AND is_deleted = 0",
                Integer.class, nameHash, certNoHash, userId);
        if (count != null && count > 0) {
            throw new BusinessException(10011, "该证件已被其他账号使用");
        }
    }
}
