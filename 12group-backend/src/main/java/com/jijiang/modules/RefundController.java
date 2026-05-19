package com.jijiang.modules;

import com.jijiang.common.AuthSupport;
import com.jijiang.common.Result;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/refund")
public class RefundController {
    private final AuthSupport authSupport;
    private final RefundAppService refundAppService;

    public RefundController(AuthSupport authSupport, RefundAppService refundAppService) {
        this.authSupport = authSupport;
        this.refundAppService = refundAppService;
    }

    @PostMapping("/submit")
    public Result<Map<String, Object>> submit(@RequestHeader("Authorization") String authorization,
                                              @RequestBody RefundSubmitRequest request) {
        var ctx = authSupport.parseBearer(authorization);
        return Result.ok(refundAppService.submitRefund(ctx, request));
    }

    @GetMapping("/list")
    public Result<List<Map<String, Object>>> list(@RequestHeader("Authorization") String authorization) {
        var ctx = authSupport.parseBearer(authorization);
        return Result.ok(refundAppService.listMyRefunds(ctx));
    }

    @GetMapping("/detail")
    public Result<Map<String, Object>> detail(@RequestHeader("Authorization") String authorization,
                                              @RequestParam Long refundId) {
        authSupport.parseBearer(authorization);
        return Result.ok(refundAppService.refundDetail(refundId));
    }
}
