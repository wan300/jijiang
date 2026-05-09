package com.jijiang.payment.modules;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
public class PaymentController {
    private final PaymentService paymentService;

    PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/internal/payment/orders")
    public CreatePaymentResponse create(@RequestBody String rawBody, @RequestHeader HttpHeaders headers) {
        return paymentService.create(rawBody, headers);
    }

    @PostMapping(value = "/api/payment/xunhu-notify", produces = MediaType.TEXT_PLAIN_VALUE)
    public String xunhuNotify(@RequestParam Map<String, String> params) {
        try {
            paymentService.handleXunhuNotify(params);
            return "success";
        } catch (Exception e) {
            return "fail";
        }
    }
}
