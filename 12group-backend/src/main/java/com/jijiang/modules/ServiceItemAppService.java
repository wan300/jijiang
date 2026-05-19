package com.jijiang.modules;

import com.jijiang.common.BusinessException;
import com.jijiang.common.UserContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
class ServiceItemAppService {
    private final JdbcTemplate jdbc;
    private final RiskAppService riskAppService;

    ServiceItemAppService(JdbcTemplate jdbc, RiskAppService riskAppService) {
        this.jdbc = jdbc;
        this.riskAppService = riskAppService;
    }

    Long publish(UserContext ctx, ServicePublishRequest request) {
        ensureSeller(ctx.userId());
        riskAppService.checkText(request.title() + " " + request.description());
        return JdbcHelper.insertAndReturnId(jdbc, """
            INSERT INTO service_item
            (seller_id, campus_id, category_id, title, description, price, price_config, cover_url, stock, status)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 0)
            """, ctx.userId(), ctx.campusId(), request.categoryId(), request.title(), request.description(),
            request.price(), request.priceConfig(), request.coverUrl(), request.stock() == null ? 1 : request.stock());
    }

    List<Map<String, Object>> search(Long campusId, String keyword, Long categoryId) {
        String kw = keyword == null ? "" : keyword.trim();
        if (categoryId != null) {
            return jdbc.queryForList("""
                SELECT id, seller_id AS sellerId, category_id AS categoryId, title, description, price, cover_url AS coverUrl,
                       stock, used_stock AS usedStock, score_avg AS scoreAvg, sales_count AS salesCount
                FROM service_item
                WHERE campus_id = ? AND category_id = ? AND status = 1 AND is_deleted = 0
                  AND (? = '' OR title LIKE ? OR description LIKE ?)
                ORDER BY sales_count DESC, publish_time DESC
                """, campusId == null ? 1L : campusId, categoryId, kw, "%" + kw + "%", "%" + kw + "%");
        }
        return jdbc.queryForList("""
            SELECT id, seller_id AS sellerId, category_id AS categoryId, title, description, price, cover_url AS coverUrl,
                   stock, used_stock AS usedStock, score_avg AS scoreAvg, sales_count AS salesCount
            FROM service_item
            WHERE campus_id = ? AND status = 1 AND is_deleted = 0
              AND (? = '' OR title LIKE ? OR description LIKE ?)
            ORDER BY sales_count DESC, publish_time DESC
            """, campusId == null ? 1L : campusId, kw, "%" + kw + "%", "%" + kw + "%");
    }

    Map<String, Object> detail(Long id) {
        return jdbc.queryForMap("""
            SELECT s.id, s.seller_id AS sellerId, u.nickname AS sellerName, s.category_id AS categoryId,
                   s.title, s.description, s.price, s.price_config AS priceConfig, s.cover_url AS coverUrl,
                   s.stock, s.used_stock AS usedStock, s.status, s.score_avg AS scoreAvg, s.sales_count AS salesCount
            FROM service_item s JOIN `user` u ON u.id = s.seller_id
            WHERE s.id = ? AND s.is_deleted = 0
            """, id);
    }

    List<Map<String, Object>> categories() {
        return jdbc.queryForList("SELECT id, name FROM category WHERE status = 1 AND is_deleted = 0 ORDER BY sort_order ASC");
    }

    private void ensureSeller(Long userId) {
        Map<String, Object> user = jdbc.queryForMap("SELECT verify_status, is_seller_verified, deposit_paid FROM `user` WHERE id = ?", userId);
        if (((Number) user.get("verify_status")).intValue() != 2 ||
                ((Number) user.get("is_seller_verified")).intValue() != 1 ||
                ((Number) user.get("deposit_paid")).intValue() != 1) {
            throw new BusinessException(10010, "请先完成实名认证和讲师认证");
        }
    }
}
