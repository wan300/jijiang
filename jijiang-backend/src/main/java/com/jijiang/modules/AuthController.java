package com.jijiang.modules;

import com.jijiang.common.AuthSupport;
import com.jijiang.common.Result;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthAppService authAppService;
    private final AuthSupport authSupport;

    public AuthController(AuthAppService authAppService, AuthSupport authSupport) {
        this.authAppService = authAppService;
        this.authSupport = authSupport;
    }

    @PostMapping("/wx-login")
    public Result<Map<String, Object>> wxLogin(@RequestBody WxLoginRequest request) {
        return Result.ok(authAppService.wxLogin(request.code()));
    }

    @PostMapping("/refresh")
    public Result<Map<String, Object>> refresh(@RequestHeader("Authorization") String authorization) {
        var ctx = authSupport.parseBearer(authorization);
        var userInfo = authAppService.userInfo(ctx.userId());
        String token = authSupport.issue(ctx.userId(), ((Number) userInfo.get("currentRole")).intValue(),
                ((Number) userInfo.get("campusId")).longValue());
        return Result.ok(Map.of("accessToken", token, "refreshToken", token, "userInfo", userInfo));
    }

    @PostMapping("/switch-role")
    public Result<Map<String, Object>> switchRole(@RequestHeader("Authorization") String authorization,
                                                  @RequestBody SwitchRoleRequest request) {
        var ctx = authSupport.parseBearer(authorization);
        authAppService.switchRole(ctx, request.targetRole());
        var userInfo = authAppService.userInfo(ctx.userId());
        String token = authSupport.issue(ctx.userId(), request.targetRole(), ((Number) userInfo.get("campusId")).longValue());
        return Result.ok(Map.of("accessToken", token, "refreshToken", token, "userInfo", userInfo));
    }

    @PostMapping("/logout")
    public Result<Void> logout() {
        return Result.ok();
    }
}

record WxLoginRequest(String code) {
}

record SwitchRoleRequest(Integer targetRole) {
}
