package com.jijiang.modules;

import com.jijiang.common.AdminAuthSupport;
import com.jijiang.common.AdminContext;
import com.jijiang.common.Result;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/admin")
public class AdminController {
    private final AdminAuthSupport adminAuthSupport;
    private final AdminAuthAppService adminAuthAppService;
    private final AdminAppService adminAppService;

    public AdminController(AdminAuthSupport adminAuthSupport,
                           AdminAuthAppService adminAuthAppService,
                           AdminAppService adminAppService) {
        this.adminAuthSupport = adminAuthSupport;
        this.adminAuthAppService = adminAuthAppService;
        this.adminAppService = adminAppService;
    }

    @PostMapping("/auth/login")
    public Result<Map<String, Object>> login(@RequestBody AdminLoginRequest request) {
        return Result.ok(adminAuthAppService.login(request));
    }

    @GetMapping("/dashboard/overview")
    public Result<Map<String, Object>> dashboard(@RequestHeader(value = "Authorization", required = false) String authorization) {
        adminContext(authorization);
        return Result.ok(adminAppService.dashboardOverview());
    }

    @GetMapping("/verify/pending")
    public Result<Map<String, Object>> pendingVerifies(@RequestHeader(value = "Authorization", required = false) String authorization,
                                                       @RequestParam(defaultValue = "1") Integer page,
                                                       @RequestParam(defaultValue = "20") Integer pageSize,
                                                       @RequestParam(required = false) String keyword) {
        adminContext(authorization);
        return Result.ok(adminAppService.pendingVerifies(page, pageSize, keyword));
    }

    @PostMapping("/verify/review")
    public Result<Void> reviewVerify(@RequestHeader(value = "Authorization", required = false) String authorization,
                                     @RequestBody AdminReviewRequest request) {
        AdminContext ctx = adminContext(authorization);
        adminAppService.reviewVerify(ctx, request.id(), Boolean.TRUE.equals(request.passed()), request.reason());
        return Result.ok();
    }

    @GetMapping("/service/pending")
    public Result<Map<String, Object>> pendingServices(@RequestHeader(value = "Authorization", required = false) String authorization,
                                                       @RequestParam(defaultValue = "1") Integer page,
                                                       @RequestParam(defaultValue = "20") Integer pageSize,
                                                       @RequestParam(required = false) String keyword) {
        adminContext(authorization);
        return Result.ok(adminAppService.pendingServices(page, pageSize, keyword));
    }

    @GetMapping("/service/list")
    public Result<Map<String, Object>> serviceList(@RequestHeader(value = "Authorization", required = false) String authorization,
                                                   @RequestParam(required = false) Integer status,
                                                   @RequestParam(required = false) String keyword,
                                                   @RequestParam(defaultValue = "1") Integer page,
                                                   @RequestParam(defaultValue = "20") Integer pageSize) {
        adminContext(authorization);
        return Result.ok(adminAppService.serviceList(status, keyword, page, pageSize));
    }

    @PostMapping("/service/review")
    public Result<Void> reviewService(@RequestHeader(value = "Authorization", required = false) String authorization,
                                      @RequestBody AdminReviewRequest request) {
        AdminContext ctx = adminContext(authorization);
        adminAppService.reviewService(ctx, request.id(), Boolean.TRUE.equals(request.passed()), request.reason());
        return Result.ok();
    }

    @PostMapping("/service/offline")
    public Result<Void> offlineService(@RequestHeader(value = "Authorization", required = false) String authorization,
                                       @RequestBody AdminOfflineRequest request) {
        AdminContext ctx = adminContext(authorization);
        adminAppService.offlineService(ctx, request.id(), request.reason());
        return Result.ok();
    }

    @GetMapping("/order/list")
    public Result<Map<String, Object>> orderList(@RequestHeader(value = "Authorization", required = false) String authorization,
                                                 @RequestParam(required = false) Integer status,
                                                 @RequestParam(required = false) String keyword,
                                                 @RequestParam(defaultValue = "1") Integer page,
                                                 @RequestParam(defaultValue = "20") Integer pageSize) {
        adminContext(authorization);
        return Result.ok(adminAppService.orderList(status, keyword, page, pageSize));
    }

    @GetMapping("/order/detail")
    public Result<Map<String, Object>> orderDetail(@RequestHeader(value = "Authorization", required = false) String authorization,
                                                   @RequestParam Long orderId) {
        adminContext(authorization);
        return Result.ok(adminAppService.orderDetail(orderId));
    }

    @GetMapping("/refund/list")
    public Result<Map<String, Object>> refundList(@RequestHeader(value = "Authorization", required = false) String authorization,
                                                  @RequestParam(required = false) Integer status,
                                                  @RequestParam(required = false) String keyword,
                                                  @RequestParam(defaultValue = "1") Integer page,
                                                  @RequestParam(defaultValue = "20") Integer pageSize) {
        adminContext(authorization);
        return Result.ok(adminAppService.refundList(status, keyword, page, pageSize));
    }

    @GetMapping("/refund/detail")
    public Result<Map<String, Object>> refundDetail(@RequestHeader(value = "Authorization", required = false) String authorization,
                                                    @RequestParam Long refundId) {
        adminContext(authorization);
        return Result.ok(adminAppService.refundDetail(refundId));
    }

    @PostMapping("/refund/review")
    public Result<Void> reviewRefund(@RequestHeader(value = "Authorization", required = false) String authorization,
                                     @RequestBody AdminRefundReviewRequest request) {
        AdminContext ctx = adminContext(authorization);
        adminAppService.reviewRefund(ctx, request.refundId(), Boolean.TRUE.equals(request.passed()),
                request.reason(), request.deductDeposit());
        return Result.ok();
    }

    private AdminContext adminContext(String authorization) {
        return adminAuthSupport.parseBearer(authorization);
    }
}

record AdminLoginRequest(String username, String password) {
}

record AdminReviewRequest(Long id, Boolean passed, String reason) {
}

record AdminOfflineRequest(Long id, String reason) {
}

record AdminRefundReviewRequest(Long refundId, Boolean passed, String reason, java.math.BigDecimal deductDeposit) {
}
