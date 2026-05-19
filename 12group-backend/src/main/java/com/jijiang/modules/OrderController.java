package com.jijiang.modules;

import com.jijiang.common.AuthSupport;
import com.jijiang.common.Result;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/order")
public class OrderController {
    private final AuthSupport authSupport;
    private final OrderAppService orderAppService;

    public OrderController(AuthSupport authSupport, OrderAppService orderAppService) {
        this.authSupport = authSupport;
        this.orderAppService = orderAppService;
    }

    @PostMapping("/create")
    public Result<Map<String, Object>> create(@RequestHeader("Authorization") String authorization,
                                              @RequestBody OrderCreateRequest request) {
        var ctx = authSupport.parseBearer(authorization);
        return Result.ok(orderAppService.create(ctx, request));
    }

    @GetMapping("/list")
    public Result<List<Map<String, Object>>> list(@RequestHeader("Authorization") String authorization,
                                                  @RequestParam(defaultValue = "buyer") String role) {
        var ctx = authSupport.parseBearer(authorization);
        return Result.ok(orderAppService.list(ctx, role));
    }

    @GetMapping("/detail")
    public Result<Map<String, Object>> detail(@RequestHeader("Authorization") String authorization,
                                              @RequestParam Long orderId) {
        var ctx = authSupport.parseBearer(authorization);
        return Result.ok(orderAppService.detail(ctx, orderId));
    }

    @PostMapping("/accept")
    public Result<Void> accept(@RequestHeader("Authorization") String authorization,
                               @RequestBody OrderActionRequest request) {
        var ctx = authSupport.parseBearer(authorization);
        orderAppService.accept(ctx, request.orderId());
        return Result.ok();
    }

    @PostMapping("/deliver")
    public Result<Void> deliver(@RequestHeader("Authorization") String authorization,
                                @RequestBody DeliverRequest request) {
        var ctx = authSupport.parseBearer(authorization);
        orderAppService.deliver(ctx, request.orderId(), request.deliverText());
        return Result.ok();
    }

    @PostMapping("/confirm")
    public Result<Void> confirm(@RequestHeader("Authorization") String authorization,
                                @RequestBody OrderActionRequest request) {
        var ctx = authSupport.parseBearer(authorization);
        orderAppService.confirm(ctx, request.orderId());
        return Result.ok();
    }
}

record OrderActionRequest(Long orderId) {
}

record DeliverRequest(Long orderId, String deliverText) {
}

record OrderCreateRequest(Long serviceId, String remark) {
}
