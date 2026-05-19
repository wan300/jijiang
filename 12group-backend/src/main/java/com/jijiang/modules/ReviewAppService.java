package com.jijiang.modules;

import com.jijiang.common.BusinessException;
import com.jijiang.common.UserContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
class ReviewAppService {
    private final JdbcTemplate jdbc;
    private final RiskAppService riskAppService;

    ReviewAppService(JdbcTemplate jdbc, RiskAppService riskAppService) {
        this.jdbc = jdbc;
        this.riskAppService = riskAppService;
    }

    Long submit(UserContext ctx, ReviewSubmitRequest request) {
        riskAppService.checkText(request.content());
        Map<String, Object> order = jdbc.queryForMap("SELECT * FROM order_main WHERE id = ? AND is_deleted = 0", request.orderId());
        if (!ctx.userId().equals(((Number) order.get("buyer_id")).longValue())) {
            throw new BusinessException(60002, "仅买家可评价");
        }
        if (((Number) order.get("status")).intValue() != 50) {
            throw new BusinessException(60003, "订单完成后才能评价");
        }
        Long reviewId = JdbcHelper.insertAndReturnId(jdbc, """
            INSERT INTO review (order_id, service_id, reviewer_id, seller_id, score, content)
            VALUES (?, ?, ?, ?, ?, ?)
            """, request.orderId(), ((Number) order.get("service_id")).longValue(), ctx.userId(),
                ((Number) order.get("seller_id")).longValue(), request.score(), request.content());
        int delta = switch (request.score()) {
            case 5 -> 2;
            case 4 -> 1;
            case 2 -> -3;
            case 1 -> -5;
            default -> 0;
        };
        Long sellerId = ((Number) order.get("seller_id")).longValue();
        jdbc.update("UPDATE `user` SET credit_score = credit_score + ? WHERE id = ?", delta, sellerId);
        Integer scoreAfter = jdbc.queryForObject("SELECT credit_score FROM `user` WHERE id = ?", Integer.class, sellerId);
        jdbc.update("""
            INSERT INTO credit_log (user_id, order_id, delta, score_after, reason)
            VALUES (?, ?, ?, ?, '订单评价')
            """, sellerId, request.orderId(), delta, scoreAfter);
        return reviewId;
    }
}
