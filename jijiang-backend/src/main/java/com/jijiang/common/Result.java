package com.jijiang.common;

public record Result<T>(int code, String message, T data, long timestamp) {
    public static <T> Result<T> ok(T data) {
        return new Result<>(0, "success", data, System.currentTimeMillis());
    }

    public static Result<Void> ok() {
        return ok(null);
    }

    public static Result<Void> fail(int code, String message) {
        return new Result<>(code, message, null, System.currentTimeMillis());
    }
}
