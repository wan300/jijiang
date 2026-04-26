package com.jijiang.modules;

import com.jijiang.common.AuthSupport;
import com.jijiang.common.Result;
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

    @PostMapping("/mock-pay")
    public Result<Map<String, Object>> mockPay(@RequestHeader("Authorization") String authorization,
                                               @RequestBody OrderActionRequest request) {
        var ctx = authSupport.parseBearer(authorization);
        return Result.ok(paymentAppService.mockPay(ctx, request.orderId()));
    }
}
