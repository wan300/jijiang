package com.jijiang.modules;

import com.jijiang.common.AuthSupport;
import com.jijiang.common.BusinessException;
import com.jijiang.common.UserContext;
import com.jijiang.infra.ExternalClients;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
class AuthAppService {
    private final JdbcTemplate jdbc;
    private final AuthSupport authSupport;
    private final ExternalClients.WxLoginClient wxLoginClient;

    AuthAppService(JdbcTemplate jdbc, AuthSupport authSupport, ExternalClients.WxLoginClient wxLoginClient) {
        this.jdbc = jdbc;
        this.authSupport = authSupport;
        this.wxLoginClient = wxLoginClient;
    }

    Map<String, Object> wxLogin(String code) {
        var session = wxLoginClient.code2Session(code);
        Long userId = queryLong("SELECT id FROM `user` WHERE openid = ? AND is_deleted = 0", session.openid());
        if (userId == null) {
            userId = JdbcHelper.insertAndReturnId(jdbc, """
                INSERT INTO `user` (openid, unionid, nickname, campus_id, last_login_time)
                VALUES (?, ?, ?, 1, CURRENT_TIMESTAMP)
                """, session.openid(), session.unionid(), "技匠用户");
            jdbc.update("""
                INSERT INTO `user_role` (user_id, role_id)
                SELECT ?, id FROM `role` WHERE code = 'ROLE_USER'
                """, userId);
        } else {
            Integer deletionStatus = jdbc.queryForObject(
                    "SELECT deletion_status FROM `user` WHERE id = ?", Integer.class, userId);
            if (deletionStatus != null && deletionStatus == 2) {
                throw new BusinessException(10031, "账号已注销");
            }
            jdbc.update("UPDATE `user` SET last_login_time = CURRENT_TIMESTAMP WHERE id = ?", userId);
        }
        Map<String, Object> userInfo = userInfo(userId);
        String accessToken = authSupport.issue(userId, ((Number) userInfo.get("currentRole")).intValue(),
                ((Number) userInfo.get("campusId")).longValue());
        String refreshToken = authSupport.issue(userId, ((Number) userInfo.get("currentRole")).intValue(),
                ((Number) userInfo.get("campusId")).longValue());
        return Map.of("accessToken", accessToken, "refreshToken", refreshToken, "userInfo", userInfo);
    }

    Map<String, Object> userInfo(Long userId) {
        return jdbc.queryForMap("""
            SELECT u.id, u.nickname, u.avatar_url AS avatarUrl, u.verify_status AS verifyStatus,
                   u.`current_role` AS currentRole, u.campus_id AS campusId, c.name AS campusName,
                   u.credit_score AS creditScore, u.is_seller_verified AS isSellerVerified,
                   u.deposit_paid AS depositPaid
            FROM `user` u LEFT JOIN campus c ON c.id = u.campus_id
            WHERE u.id = ? AND u.is_deleted = 0
            """, userId);
    }

    void switchRole(UserContext ctx, Integer targetRole) {
        if (targetRole == null || (targetRole != 1 && targetRole != 2)) {
            throw new BusinessException(400001, "目标身份不合法");
        }
        if (targetRole == 2) {
            Integer verified = jdbc.queryForObject("SELECT is_seller_verified FROM `user` WHERE id = ?", Integer.class, ctx.userId());
            if (verified == null || verified != 1) {
                throw new BusinessException(10010, "请先完成讲师认证");
            }
        }
        jdbc.update("UPDATE `user` SET `current_role` = ? WHERE id = ?", targetRole, ctx.userId());
    }

    private Long queryLong(String sql, Object... args) {
        try {
            return jdbc.queryForObject(sql, Long.class, args);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }
}
