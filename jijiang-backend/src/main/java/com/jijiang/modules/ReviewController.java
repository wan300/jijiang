package com.jijiang.modules;

import com.jijiang.common.AuthSupport;
import com.jijiang.common.Result;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/review")
public class ReviewController {
    private final AuthSupport authSupport;
    private final ReviewAppService reviewAppService;

    public ReviewController(AuthSupport authSupport, ReviewAppService reviewAppService) {
        this.authSupport = authSupport;
        this.reviewAppService = reviewAppService;
    }

    @PostMapping("/submit")
    public Result<Map<String, Object>> submit(@RequestHeader("Authorization") String authorization,
                                              @RequestBody ReviewSubmitRequest request) {
        var ctx = authSupport.parseBearer(authorization);
        Long reviewId = reviewAppService.submit(ctx, request);
        return Result.ok(Map.of("reviewId", reviewId));
    }
}
