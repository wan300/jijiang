package com.jijiang.payment.common;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.Map;

@RestControllerAdvice
class GlobalExceptionHandler {
    @ExceptionHandler(BusinessException.class)
    ResponseEntity<Map<String, Object>> business(BusinessException e) {
        return ResponseEntity.badRequest().body(Map.of(
                "code", e.getCode(),
                "message", e.getMessage(),
                "timestamp", Instant.now().toEpochMilli()
        ));
    }
}
