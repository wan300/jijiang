package com.jijiang.common;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.List;
import java.util.UUID;

@Component
public class RateLimitInterceptor implements HandlerInterceptor {
    private static final Logger log = LoggerFactory.getLogger(RateLimitInterceptor.class);

    private static final String RATE_LIMIT_LUA = """
        local key = KEYS[1]
        local max_requests = tonumber(ARGV[1])
        local window_ms = tonumber(ARGV[2]) * 1000
        local now = tonumber(ARGV[3])
        local member = ARGV[4]
        local window_start = now - window_ms
        redis.call('ZREMRANGEBYSCORE', key, 0, window_start)
        local count = redis.call('ZCARD', key)
        if count >= max_requests then
            return 0
        end
        redis.call('ZADD', key, now, member)
        redis.call('PEXPIRE', key, window_ms + 1000)
        return 1
        """;

    private final StringRedisTemplate redisTemplate;

    public RateLimitInterceptor(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        RateLimit rateLimit = handlerMethod.getMethodAnnotation(RateLimit.class);
        if (rateLimit == null) {
            return true;
        }

        String key = buildKey(rateLimit, request);
        long now = System.currentTimeMillis();
        String member = now + ":" + UUID.randomUUID().toString().replace("-", "");

        try {
            DefaultRedisScript<Long> script = new DefaultRedisScript<>(RATE_LIMIT_LUA, Long.class);
            Long result = redisTemplate.execute(script,
                    List.of("rate:" + key),
                    String.valueOf(rateLimit.maxRequests()),
                    String.valueOf(rateLimit.windowSeconds()),
                    String.valueOf(now),
                    member);
            if (result != null && result == 1) {
                return true;
            }
        } catch (Exception e) {
            log.warn("限流检查失败, 降级放行: key={}", key, e);
            return true;
        }

        throw new RateLimitException();
    }

    private String buildKey(RateLimit rateLimit, HttpServletRequest request) {
        String prefix = rateLimit.key().isBlank()
                ? request.getMethod() + ":" + request.getRequestURI()
                : rateLimit.key();
        return switch (rateLimit.scope()) {
            case USER -> {
                String auth = request.getHeader("Authorization");
                yield prefix + ":user:" + (auth != null ? Math.abs(auth.hashCode()) : "anon");
            }
            case IP_USER -> {
                String ip = clientIp(request);
                String auth = request.getHeader("Authorization");
                yield prefix + ":ip:" + ip + ":user:" + (auth != null ? Math.abs(auth.hashCode()) : "anon");
            }
            default -> prefix + ":ip:" + clientIp(request);
        };
    }

    private String clientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }
}
