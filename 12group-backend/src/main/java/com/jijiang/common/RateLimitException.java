package com.jijiang.common;

public class RateLimitException extends BusinessException {
    public RateLimitException() {
        super(10004, "请求过于频繁，请稍后再试");
    }
}
