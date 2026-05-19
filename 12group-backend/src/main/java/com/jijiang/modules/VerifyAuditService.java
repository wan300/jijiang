package com.jijiang.modules;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
class VerifyAuditService {
    private final JdbcTemplate jdbc;

    VerifyAuditService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    void log(Long userId, Long recordId, String action, String detail, Long operatorId) {
        jdbc.update("""
            INSERT INTO verify_audit_log (user_id, record_id, action, detail, operator_id, create_time)
            VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
            """, userId, recordId, action, detail, operatorId);
    }
}
