package com.jijiang.common;

public record UserContext(Long userId, Integer role, Long campusId) {
    public boolean isSeller() {
        return role != null && role == 2;
    }
}
