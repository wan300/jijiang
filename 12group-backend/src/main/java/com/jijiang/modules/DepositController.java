package com.jijiang.modules;

import com.jijiang.common.AuthSupport;
import com.jijiang.common.Result;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/deposit")
public class DepositController {
    private final AuthSupport authSupport;
    private final DepositAppService depositAppService;

    public DepositController(AuthSupport authSupport, DepositAppService depositAppService) {
        this.authSupport = authSupport;
        this.depositAppService = depositAppService;
    }

    @PostMapping("/create")
    public Result<Map<String, Object>> create(@RequestHeader("Authorization") String authorization) {
        var ctx = authSupport.parseBearer(authorization);
        return Result.ok(depositAppService.createDeposit(ctx));
    }

    @GetMapping("/status")
    public Result<Map<String, Object>> status(@RequestHeader("Authorization") String authorization) {
        var ctx = authSupport.parseBearer(authorization);
        return Result.ok(depositAppService.getDepositStatus(ctx));
    }
}
