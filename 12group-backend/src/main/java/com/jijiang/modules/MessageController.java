package com.jijiang.modules;

import com.jijiang.common.AuthSupport;
import com.jijiang.common.Result;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/message")
public class MessageController {
    private final AuthSupport authSupport;
    private final MessageAppService messageAppService;

    public MessageController(AuthSupport authSupport, MessageAppService messageAppService) {
        this.authSupport = authSupport;
        this.messageAppService = messageAppService;
    }

    @PostMapping("/send")
    public Result<Map<String, Object>> send(@RequestHeader("Authorization") String authorization,
                                            @RequestBody MessageSendRequest request) {
        var ctx = authSupport.parseBearer(authorization);
        Long messageId = messageAppService.send(ctx, request.orderId(), request.content());
        return Result.ok(Map.of("messageId", messageId));
    }

    @GetMapping("/list")
    public Result<List<Map<String, Object>>> list(@RequestHeader("Authorization") String authorization,
                                                  @RequestParam Long orderId) {
        var ctx = authSupport.parseBearer(authorization);
        return Result.ok(messageAppService.list(ctx, orderId));
    }
}

record MessageSendRequest(Long orderId, String content) {
}
