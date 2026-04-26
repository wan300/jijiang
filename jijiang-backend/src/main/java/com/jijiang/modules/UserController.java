package com.jijiang.modules;

import com.jijiang.common.AuthSupport;
import com.jijiang.common.Result;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/user")
public class UserController {
    private final AuthSupport authSupport;
    private final AuthAppService authAppService;
    private final UserAppService userAppService;

    public UserController(AuthSupport authSupport, AuthAppService authAppService, UserAppService userAppService) {
        this.authSupport = authSupport;
        this.authAppService = authAppService;
        this.userAppService = userAppService;
    }

    @GetMapping("/me")
    public Result<Map<String, Object>> me(@RequestHeader("Authorization") String authorization) {
        var ctx = authSupport.parseBearer(authorization);
        return Result.ok(authAppService.userInfo(ctx.userId()));
    }

    @PostMapping("/verify/submit")
    public Result<Map<String, Object>> submitVerify(@RequestHeader("Authorization") String authorization,
                                                    @RequestBody VerifySubmitRequest request) {
        var ctx = authSupport.parseBearer(authorization);
        return Result.ok(userAppService.submitVerify(ctx, request));
    }

    @GetMapping("/verify/status")
    public Result<Map<String, Object>> verifyStatus(@RequestHeader("Authorization") String authorization) {
        var ctx = authSupport.parseBearer(authorization);
        return Result.ok(userAppService.verifyStatus(ctx));
    }
}
