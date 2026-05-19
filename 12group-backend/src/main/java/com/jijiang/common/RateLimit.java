package com.jijiang.common;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface RateLimit {
    String key() default "";

    int maxRequests() default 60;

    int windowSeconds() default 60;

    Scope scope() default Scope.IP;

    enum Scope {
        IP, USER, IP_USER
    }
}
