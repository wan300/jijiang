package com.jijiang.modules;

import com.jijiang.common.BusinessException;
import com.jijiang.common.UserContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.Map;

@Service
class AccountDeletionService {
    private static final Logger log = LoggerFactory.getLogger(AccountDeletionService.class);
    private static final int COOLING_DAYS = 7;

    private final JdbcTemplate jdbc;
    private final TransactionTemplate tx;

    AccountDeletionService(JdbcTemplate jdbc, TransactionTemplate tx) {
        this.jdbc = jdbc;
        this.tx = tx;
    }

    Map<String, Object> requestDeletion(UserContext ctx) {
        // Check if already has a pending deletion
        try {
            Map<String, Object> existing = jdbc.queryForMap(
                    "SELECT id, status, cooling_until AS coolingUntil FROM account_deletion WHERE user_id = ? AND status = 0",
                    ctx.userId());
            return Map.of("message", "已有待处理的注销申请", "coolingUntil", existing.get("coolingUntil"));
        } catch (org.springframework.dao.EmptyResultDataAccessException e) {
            // No existing request, proceed
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime coolingUntil = now.plusDays(COOLING_DAYS);
        jdbc.update("""
            INSERT INTO account_deletion (user_id, status, request_time, cooling_until)
            VALUES (?, 0, ?, ?)
            """, ctx.userId(), now, coolingUntil);
        jdbc.update("UPDATE `user` SET deletion_status = 1, update_time = CURRENT_TIMESTAMP WHERE id = ?", ctx.userId());
        return Map.of("message", "注销申请已提交，7天冷静期后可完成注销", "coolingUntil", coolingUntil);
    }

    Map<String, Object> cancelDeletion(UserContext ctx) {
        int updated = jdbc.update("""
            UPDATE account_deletion SET status = 1, cancelled_time = CURRENT_TIMESTAMP, update_time = CURRENT_TIMESTAMP
            WHERE user_id = ? AND status = 0
            """, ctx.userId());
        if (updated > 0) {
            jdbc.update("UPDATE `user` SET deletion_status = 0, update_time = CURRENT_TIMESTAMP WHERE id = ?", ctx.userId());
            return Map.of("message", "注销申请已取消");
        }
        throw new BusinessException(10030, "没有待处理的注销申请");
    }

    Map<String, Object> deletionStatus(UserContext ctx) {
        try {
            return jdbc.queryForMap("""
                SELECT d.status, d.request_time AS requestTime, d.cooling_until AS coolingUntil,
                       d.completed_time AS completedTime, d.cancelled_time AS cancelledTime,
                       u.deletion_status AS deletionStatus
                FROM account_deletion d JOIN `user` u ON u.id = d.user_id
                WHERE d.user_id = ?
                ORDER BY d.id DESC LIMIT 1
                """, ctx.userId());
        } catch (org.springframework.dao.EmptyResultDataAccessException e) {
            return Map.of("status", -1, "message", "无注销记录");
        }
    }

    void processExpiredDeletions() {
        LocalDateTime now = LocalDateTime.now();
        var rows = jdbc.queryForList("""
            SELECT id, user_id FROM account_deletion
            WHERE status = 0 AND cooling_until <= ? AND is_deleted = 0
            """, now);
        for (var row : rows) {
            Long deletionId = ((Number) row.get("id")).longValue();
            Long userId = ((Number) row.get("user_id")).longValue();
            try {
                tx.executeWithoutResult(status -> {
                    anonymizeUser(userId);
                    jdbc.update("""
                        UPDATE account_deletion SET status = 2, completed_time = CURRENT_TIMESTAMP, update_time = CURRENT_TIMESTAMP
                        WHERE id = ?
                        """, deletionId);
                });
                log.info("账号注销完成: userId={}", userId);
            } catch (Exception e) {
                log.error("账号注销失败: userId={}", userId, e);
            }
        }
    }

    private void anonymizeUser(Long userId) {
        String redacted = "[已注销]";
        jdbc.update("""
            UPDATE `user`
            SET nickname = ?, avatar_url = NULL, real_name_encrypted = NULL, student_no_encrypted = NULL,
                openid = CONCAT('deleted-', openid), unionid = NULL,
                deletion_status = 2, update_time = CURRENT_TIMESTAMP
            WHERE id = ?
            """, redacted, userId);
    }
}
