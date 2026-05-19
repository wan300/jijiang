package com.jijiang.modules;

import com.jijiang.common.AuthSupport;
import com.jijiang.common.Result;
import com.jijiang.infra.ExternalClients;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/user")
public class UserController {
    private final AuthSupport authSupport;
    private final AuthAppService authAppService;
    private final UserAppService userAppService;
    private final AccountDeletionService accountDeletionService;

    public UserController(AuthSupport authSupport, AuthAppService authAppService, UserAppService userAppService,
                          AccountDeletionService accountDeletionService) {
        this.authSupport = authSupport;
        this.authAppService = authAppService;
        this.userAppService = userAppService;
        this.accountDeletionService = accountDeletionService;
    }

    @GetMapping("/me")
    public Result<Map<String, Object>> me(@RequestHeader("Authorization") String authorization) {
        var ctx = authSupport.parseBearer(authorization);
        return Result.ok(authAppService.userInfo(ctx.userId()));
    }

    @PostMapping("/verify/upload-token")
    public Result<ExternalClients.UploadToken> uploadToken(@RequestHeader("Authorization") String authorization,
                                                            @RequestBody UploadTokenRequest request) {
        authSupport.parseBearer(authorization);
        return Result.ok(userAppService.generateUploadToken(request.fileName(), request.maxSizeBytes()));
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

    @PostMapping("/deletion/request")
    public Result<Map<String, Object>> requestDeletion(@RequestHeader("Authorization") String authorization) {
        var ctx = authSupport.parseBearer(authorization);
        return Result.ok(accountDeletionService.requestDeletion(ctx));
    }

    @PostMapping("/deletion/cancel")
    public Result<Map<String, Object>> cancelDeletion(@RequestHeader("Authorization") String authorization) {
        var ctx = authSupport.parseBearer(authorization);
        return Result.ok(accountDeletionService.cancelDeletion(ctx));
    }

    @GetMapping("/deletion/status")
    public Result<Map<String, Object>> deletionStatus(@RequestHeader("Authorization") String authorization) {
        var ctx = authSupport.parseBearer(authorization);
        return Result.ok(accountDeletionService.deletionStatus(ctx));
    }
}

record UploadTokenRequest(String fileName, long maxSizeBytes) {
}

record VerifySubmitRequest(Long campusId, Integer certType, String certImageUrl, String realName, String studentNo) {
}
