package com.jijiang.modules;

import com.jijiang.common.AdminAuthSupport;
import com.jijiang.common.BusinessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
class AdminAuthAppService {
    private final JdbcTemplate jdbc;
    private final AdminAuthSupport adminAuthSupport;
    private final PasswordEncoder passwordEncoder;

    AdminAuthAppService(JdbcTemplate jdbc, AdminAuthSupport adminAuthSupport, PasswordEncoder passwordEncoder) {
        this.jdbc = jdbc;
        this.adminAuthSupport = adminAuthSupport;
        this.passwordEncoder = passwordEncoder;
    }

    Map<String, Object> login(AdminLoginRequest request) {
        if (request.username() == null || request.username().isBlank() ||
                request.password() == null || request.password().isBlank()) {
            throw new BusinessException(80002, "请输入管理员账号和密码");
        }
        Map<String, Object> admin = queryAdmin(request.username().trim());
        if (admin == null || !passwordEncoder.matches(request.password(), String.valueOf(admin.get("passwordHash")))) {
            throw new BusinessException(80002, "管理员账号或密码错误");
        }
        if (((Number) admin.get("status")).intValue() != 1) {
            throw new BusinessException(80003, "管理员账号已禁用");
        }
        Long adminId = ((Number) admin.get("id")).longValue();
        jdbc.update("UPDATE admin_user SET last_login_time = CURRENT_TIMESTAMP WHERE id = ?", adminId);
        String token = adminAuthSupport.issue(adminId, String.valueOf(admin.get("roleCode")));
        return Map.of("accessToken", token, "adminInfo", adminInfo(admin));
    }

    private Map<String, Object> queryAdmin(String username) {
        try {
            return jdbc.queryForMap("""
                SELECT id, username, password_hash AS passwordHash, display_name AS displayName,
                       role_code AS roleCode, status
                FROM admin_user
                WHERE username = ? AND is_deleted = 0
                """, username);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    private Map<String, Object> adminInfo(Map<String, Object> admin) {
        return Map.of(
                "id", admin.get("id"),
                "username", admin.get("username"),
                "displayName", admin.get("displayName"),
                "roleCode", admin.get("roleCode")
        );
    }
}
