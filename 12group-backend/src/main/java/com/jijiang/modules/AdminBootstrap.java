package com.jijiang.modules;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.Map;

@Component
public class AdminBootstrap implements ApplicationRunner {
    private final JdbcTemplate jdbc;
    private final PasswordEncoder passwordEncoder;
    private final Environment environment;
    private final String username;
    private final String password;
    private final String displayName;

    public AdminBootstrap(JdbcTemplate jdbc,
                          PasswordEncoder passwordEncoder,
                          Environment environment,
                          @Value("${jijiang.admin.username}") String username,
                          @Value("${jijiang.admin.password}") String password,
                          @Value("${jijiang.admin.display-name}") String displayName) {
        this.jdbc = jdbc;
        this.passwordEncoder = passwordEncoder;
        this.environment = environment;
        this.username = username;
        this.password = password;
        this.displayName = displayName;
    }

    @Override
    public void run(ApplicationArguments args) {
        boolean prod = Arrays.asList(environment.getActiveProfiles()).contains("prod");
        if (prod && (!StringUtils.hasText(environment.getProperty("ADMIN_USERNAME")) ||
                !StringUtils.hasText(environment.getProperty("ADMIN_PASSWORD")))) {
            throw new IllegalStateException("生产环境必须配置 ADMIN_USERNAME 和 ADMIN_PASSWORD");
        }
        if (!StringUtils.hasText(username) || !StringUtils.hasText(password)) {
            throw new IllegalStateException("管理员初始化账号或密码不能为空");
        }
        Map<String, Object> existing = findExistingAdmin();
        if (existing != null) {
            if (!prod && !passwordEncoder.matches(password, String.valueOf(existing.get("password_hash")))) {
                jdbc.update("""
                    UPDATE admin_user
                    SET password_hash = ?, display_name = ?, status = 1, update_time = CURRENT_TIMESTAMP
                    WHERE id = ?
                    """, passwordEncoder.encode(password), displayName, existing.get("id"));
            }
            return;
        }
        jdbc.update("""
            INSERT INTO admin_user (username, password_hash, display_name, role_code, status)
            VALUES (?, ?, ?, 'ROLE_ADMIN', 1)
            """, username, passwordEncoder.encode(password), displayName);
    }

    private Map<String, Object> findExistingAdmin() {
        try {
            return jdbc.queryForMap(
                    "SELECT id, password_hash FROM admin_user WHERE username = ? AND is_deleted = 0",
                    username);
        } catch (Exception e) {
            return null;
        }
    }
}
