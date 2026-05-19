package com.jijiang.common;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class AuthorizationSupport {
    private static final String PERM_CACHE_PREFIX = "perm:user:";
    private static final Duration CACHE_TTL = Duration.ofMinutes(5);

    private final JdbcTemplate jdbc;
    private final StringRedisTemplate redisTemplate;

    public AuthorizationSupport(JdbcTemplate jdbc, StringRedisTemplate redisTemplate) {
        this.jdbc = jdbc;
        this.redisTemplate = redisTemplate;
    }

    public void requirePermission(UserContext ctx, String permissionCode) {
        if (!hasPermission(ctx.userId(), permissionCode)) {
            throw new BusinessException(10003, "无操作权限");
        }
    }

    public void requireAnyPermission(UserContext ctx, String... permissionCodes) {
        Set<String> perms = getEffectivePermissions(ctx.userId());
        for (String code : permissionCodes) {
            if (perms.contains(code)) {
                return;
            }
        }
        throw new BusinessException(10003, "无操作权限");
    }

    public boolean hasPermission(Long userId, String permissionCode) {
        return getEffectivePermissions(userId).contains(permissionCode);
    }

    Set<String> getEffectivePermissions(Long userId) {
        if (redisTemplate != null) {
            String cacheKey = PERM_CACHE_PREFIX + userId;
            Set<String> cached = redisTemplate.opsForSet().members(cacheKey);
            if (cached != null && !cached.isEmpty()) {
                return cached;
            }
        }
        Set<String> permissions = loadPermissions(userId);
        if (redisTemplate != null && !permissions.isEmpty()) {
            String cacheKey = PERM_CACHE_PREFIX + userId;
            redisTemplate.opsForSet().add(cacheKey, permissions.toArray(new String[0]));
            redisTemplate.expire(cacheKey, CACHE_TTL);
        }
        return permissions;
    }

    void invalidateCache(Long userId) {
        if (redisTemplate != null) {
            redisTemplate.delete(PERM_CACHE_PREFIX + userId);
        }
    }

    private Set<String> loadPermissions(Long userId) {
        Integer roleLevel = jdbc.queryForObject("""
            SELECT COALESCE(r.level, 0)
            FROM user_role ur JOIN `role` r ON r.id = ur.role_id
            WHERE ur.user_id = ? AND ur.is_deleted = 0
            ORDER BY r.level DESC LIMIT 1
            """, Integer.class, userId);
        if (roleLevel == null) {
            return Collections.emptySet();
        }
        List<String> perms = jdbc.queryForList("""
            SELECT DISTINCT p.code
            FROM role_permission rp
            JOIN permission p ON p.code = rp.permission_code
            JOIN `role` r ON r.id = rp.role_id
            JOIN user_role ur ON ur.role_id = r.id
            WHERE ur.user_id = ? AND r.level <= ? AND ur.is_deleted = 0
            """, String.class, userId, roleLevel);
        return new HashSet<>(perms);
    }
}
