package com.jijiang.modules;

import com.jijiang.common.Result;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin")
public class AdminController {
    private final AdminAppService adminAppService;

    public AdminController(AdminAppService adminAppService) {
        this.adminAppService = adminAppService;
    }

    @GetMapping("/verify/pending")
    public Result<List<Map<String, Object>>> pendingVerifies() {
        return Result.ok(adminAppService.pendingVerifies());
    }

    @PostMapping("/verify/review")
    public Result<Void> reviewVerify(@RequestBody AdminReviewRequest request) {
        adminAppService.reviewVerify(request.id(), Boolean.TRUE.equals(request.passed()), request.reason());
        return Result.ok();
    }

    @GetMapping("/service/pending")
    public Result<List<Map<String, Object>>> pendingServices() {
        return Result.ok(adminAppService.pendingServices());
    }

    @PostMapping("/service/review")
    public Result<Void> reviewService(@RequestBody AdminReviewRequest request) {
        adminAppService.reviewService(request.id(), Boolean.TRUE.equals(request.passed()), request.reason());
        return Result.ok();
    }
}
