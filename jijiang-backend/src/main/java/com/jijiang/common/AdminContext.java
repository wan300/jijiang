package com.jijiang.common;

public record AdminContext(Long adminId, String roleCode) {
    public boolean isAdmin() {
        return "ROLE_ADMIN".equals(roleCode);
    }
}
