package com.jijiang.payment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class PaymentServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(PaymentServerApplication.class, args);
    }
}
