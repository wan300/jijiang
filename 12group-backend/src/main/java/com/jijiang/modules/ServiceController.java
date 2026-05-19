package com.jijiang.modules;

import com.jijiang.common.AuthSupport;
import com.jijiang.common.Result;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/service")
public class ServiceController {
    private final AuthSupport authSupport;
    private final ServiceItemAppService serviceItemAppService;

    public ServiceController(AuthSupport authSupport, ServiceItemAppService serviceItemAppService) {
        this.authSupport = authSupport;
        this.serviceItemAppService = serviceItemAppService;
    }

    @GetMapping("/categories")
    public Result<List<Map<String, Object>>> categories() {
        return Result.ok(serviceItemAppService.categories());
    }

    @GetMapping("/search")
    public Result<List<Map<String, Object>>> search(@RequestParam(defaultValue = "1") Long campusId,
                                                    @RequestParam(required = false) String keyword,
                                                    @RequestParam(required = false) Long categoryId) {
        return Result.ok(serviceItemAppService.search(campusId, keyword, categoryId));
    }

    @GetMapping("/detail")
    public Result<Map<String, Object>> detail(@RequestParam Long id) {
        return Result.ok(serviceItemAppService.detail(id));
    }

    @PostMapping("/publish")
    public Result<Map<String, Object>> publish(@RequestHeader("Authorization") String authorization,
                                               @RequestBody ServicePublishRequest request) {
        var ctx = authSupport.parseBearer(authorization);
        Long serviceId = serviceItemAppService.publish(ctx, request);
        return Result.ok(Map.of("serviceId", serviceId, "status", 0, "message", "已提交后台审核"));
    }
}

record ServicePublishRequest(Long categoryId, String title, String description, java.math.BigDecimal price,
                             String priceConfig, String coverUrl, Integer stock) {
}
