package com.jijiang.modules;

import com.jijiang.common.BusinessException;
import com.jijiang.common.UserContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
class MessageAppService {
    private final JdbcTemplate jdbc;
    private final RiskAppService riskAppService;

    MessageAppService(JdbcTemplate jdbc, RiskAppService riskAppService) {
        this.jdbc = jdbc;
        this.riskAppService = riskAppService;
    }

    Long send(UserContext ctx, Long orderId, String content) {
        riskAppService.checkText(content);
        Map<String, Object> order = jdbc.queryForMap("SELECT * FROM order_main WHERE id = ? AND is_deleted = 0", orderId);
        Long buyerId = ((Number) order.get("buyer_id")).longValue();
        Long sellerId = ((Number) order.get("seller_id")).longValue();
        if (!ctx.userId().equals(buyerId) && !ctx.userId().equals(sellerId)) {
            throw new BusinessException(50001, "无权发送该订单消息");
        }
        Long receiverId = ctx.userId().equals(buyerId) ? sellerId : buyerId;
        return JdbcHelper.insertAndReturnId(jdbc, """
            INSERT INTO message (order_id, sender_id, receiver_id, content)
            VALUES (?, ?, ?, ?)
            """, orderId, ctx.userId(), receiverId, content);
    }

    List<Map<String, Object>> list(UserContext ctx, Long orderId) {
        Map<String, Object> order = jdbc.queryForMap("SELECT * FROM order_main WHERE id = ? AND is_deleted = 0", orderId);
        Long buyerId = ((Number) order.get("buyer_id")).longValue();
        Long sellerId = ((Number) order.get("seller_id")).longValue();
        if (!ctx.userId().equals(buyerId) && !ctx.userId().equals(sellerId)) {
            throw new BusinessException(50001, "无权查看该订单消息");
        }
        return jdbc.queryForList("""
            SELECT id, order_id AS orderId, sender_id AS senderId, receiver_id AS receiverId,
                   content, is_read AS isRead, create_time AS createTime
            FROM message WHERE order_id = ? AND is_deleted = 0 ORDER BY id ASC
            """, orderId);
    }
}
