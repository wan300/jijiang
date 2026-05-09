package com.jijiang.modules;

import com.jijiang.common.AuthSupport;
import com.jijiang.common.Result;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/payment")
public class PaymentController {
    private final AuthSupport authSupport;
    private final PaymentAppService paymentAppService;

    public PaymentController(AuthSupport authSupport, PaymentAppService paymentAppService) {
        this.authSupport = authSupport;
        this.paymentAppService = paymentAppService;
    }

    @PostMapping("/create")
    public Result<Map<String, Object>> create(@RequestHeader("Authorization") String authorization,
                                              @RequestBody OrderActionRequest request) {
        var ctx = authSupport.parseBearer(authorization);
        return Result.ok(paymentAppService.createPayment(ctx, request.orderId()));
    }
}

@RestController
class InternalPaymentController {
    private final PaymentAppService paymentAppService;

    InternalPaymentController(PaymentAppService paymentAppService) {
        this.paymentAppService = paymentAppService;
    }

    @PostMapping("/internal/payment/callback")
    public Result<Void> callback(@RequestBody String rawBody, @RequestHeader HttpHeaders headers) {
        paymentAppService.handlePaymentServerCallback(rawBody, headers);
        return Result.ok();
    }
}
