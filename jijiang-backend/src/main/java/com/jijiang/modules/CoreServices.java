package com.jijiang.modules;

import com.jijiang.common.AuthSupport;
import com.jijiang.common.BusinessException;
import com.jijiang.common.UserContext;
import com.jijiang.infra.ExternalClients;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
class AuthAppService {
    private final JdbcTemplate jdbc;
    private final AuthSupport authSupport;
    private final ExternalClients.WxLoginClient wxLoginClient;

    AuthAppService(JdbcTemplate jdbc, AuthSupport authSupport, ExternalClients.WxLoginClient wxLoginClient) {
        this.jdbc = jdbc;
        this.authSupport = authSupport;
        this.wxLoginClient = wxLoginClient;
    }

    Map<String, Object> wxLogin(String code) {
        var session = wxLoginClient.code2Session(code);
        Long userId = queryLong("SELECT id FROM `user` WHERE openid = ? AND is_deleted = 0", session.openid());
        if (userId == null) {
            userId = insertAndReturnId("""
                INSERT INTO `user` (openid, unionid, nickname, campus_id, last_login_time)
                VALUES (?, ?, ?, 1, CURRENT_TIMESTAMP)
                """, session.openid(), session.unionid(), "技匠用户");
            jdbc.update("""
                INSERT INTO `user_role` (user_id, role_id)
                SELECT ?, id FROM `role` WHERE code = 'ROLE_USER'
                """, userId);
        } else {
            jdbc.update("UPDATE `user` SET last_login_time = CURRENT_TIMESTAMP WHERE id = ?", userId);
        }
        Map<String, Object> userInfo = userInfo(userId);
        String accessToken = authSupport.issue(userId, ((Number) userInfo.get("currentRole")).intValue(),
                ((Number) userInfo.get("campusId")).longValue());
        String refreshToken = authSupport.issue(userId, ((Number) userInfo.get("currentRole")).intValue(),
                ((Number) userInfo.get("campusId")).longValue());
        return Map.of("accessToken", accessToken, "refreshToken", refreshToken, "userInfo", userInfo);
    }

    Map<String, Object> userInfo(Long userId) {
        return jdbc.queryForMap("""
            SELECT u.id, u.nickname, u.avatar_url AS avatarUrl, u.verify_status AS verifyStatus,
                   u.current_role AS currentRole, u.campus_id AS campusId, c.name AS campusName,
                   u.credit_score AS creditScore, u.is_seller_verified AS isSellerVerified,
                   u.deposit_paid AS depositPaid
            FROM `user` u LEFT JOIN campus c ON c.id = u.campus_id
            WHERE u.id = ? AND u.is_deleted = 0
            """, userId);
    }

    void switchRole(UserContext ctx, Integer targetRole) {
        if (targetRole == null || (targetRole != 1 && targetRole != 2)) {
            throw new BusinessException(400001, "目标身份不合法");
        }
        if (targetRole == 2) {
            Integer verified = jdbc.queryForObject("SELECT is_seller_verified FROM `user` WHERE id = ?", Integer.class, ctx.userId());
            if (verified == null || verified != 1) {
                throw new BusinessException(10010, "请先完成讲师认证");
            }
        }
        jdbc.update("UPDATE `user` SET current_role = ? WHERE id = ?", targetRole, ctx.userId());
    }

    private Long queryLong(String sql, Object... args) {
        try {
            return jdbc.queryForObject(sql, Long.class, args);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    private Long insertAndReturnId(String sql, Object... args) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            for (int i = 0; i < args.length; i++) {
                ps.setObject(i + 1, args[i]);
            }
            return ps;
        }, keyHolder);
        return Objects.requireNonNull(keyHolder.getKey()).longValue();
    }
}

@Service
class UserAppService {
    private final JdbcTemplate jdbc;
    private final ExternalClients.OcrClient ocrClient;

    UserAppService(JdbcTemplate jdbc, ExternalClients.OcrClient ocrClient) {
        this.jdbc = jdbc;
        this.ocrClient = ocrClient;
    }

    Map<String, Object> submitVerify(UserContext ctx, VerifySubmitRequest request) {
        var ocr = ocrClient.recognize(request.certImageUrl(), request.realName(), request.studentNo());
        int status = ocr.passed() ? 2 : 1;
        int reviewMode = ocr.passed() ? 1 : 2;
        Long recordId = insertAndReturnId("""
            INSERT INTO user_verify_record
            (user_id, campus_id, cert_type, cert_image_url, real_name, student_no, ocr_result, ocr_confidence, status, review_mode)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """, ctx.userId(), request.campusId(), request.certType(), request.certImageUrl(), request.realName(),
            request.studentNo(), ocr.raw(), ocr.confidence(), status, reviewMode);
        if (status == 2) {
            jdbc.update("""
                UPDATE `user`
                SET verify_status = 2, is_seller_verified = 1, campus_id = ?, real_name_encrypted = ?, student_no_encrypted = ?
                WHERE id = ?
                """, request.campusId(), request.realName(), request.studentNo(), ctx.userId());
            ensureSellerAccount(ctx.userId());
        } else {
            jdbc.update("UPDATE `user` SET verify_status = 1, campus_id = ? WHERE id = ?", request.campusId(), ctx.userId());
        }
        return Map.of("recordId", recordId, "status", status, "reviewMode", reviewMode,
                "message", status == 2 ? "mock OCR 自动通过" : "已进入人工审核");
    }

    Map<String, Object> verifyStatus(UserContext ctx) {
        return jdbc.queryForMap("SELECT verify_status AS status, is_seller_verified AS isSellerVerified FROM `user` WHERE id = ?", ctx.userId());
    }

    void ensureSellerAccount(Long sellerId) {
        Integer exists = jdbc.queryForObject("SELECT COUNT(*) FROM seller_account WHERE seller_id = ?", Integer.class, sellerId);
        if (exists == null || exists == 0) {
            jdbc.update("INSERT INTO seller_account (seller_id) VALUES (?)", sellerId);
        }
    }

    private Long insertAndReturnId(String sql, Object... args) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            for (int i = 0; i < args.length; i++) {
                ps.setObject(i + 1, args[i]);
            }
            return ps;
        }, keyHolder);
        return Objects.requireNonNull(keyHolder.getKey()).longValue();
    }
}

@Service
class RiskAppService {
    private final JdbcTemplate jdbc;
    private final ExternalClients.ContentSafetyClient contentSafetyClient;

    RiskAppService(JdbcTemplate jdbc, ExternalClients.ContentSafetyClient contentSafetyClient) {
        this.jdbc = jdbc;
        this.contentSafetyClient = contentSafetyClient;
    }

    void checkText(String text) {
        if (text == null || text.isBlank()) {
            return;
        }
        List<String> words = jdbc.query("SELECT word FROM sensitive_word WHERE is_deleted = 0",
                (rs, rowNum) -> rs.getString("word"));
        String lower = text.toLowerCase();
        for (String word : words) {
            if (lower.contains(word.toLowerCase())) {
                throw new BusinessException(40001, "内容命中敏感词：" + word);
            }
        }
        contentSafetyClient.checkText(text);
    }
}

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
        return insertAndReturnId("""
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

    private Long insertAndReturnId(String sql, Object... args) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            for (int i = 0; i < args.length; i++) {
                ps.setObject(i + 1, args[i]);
            }
            return ps;
        }, keyHolder);
        return Objects.requireNonNull(keyHolder.getKey()).longValue();
    }
}

@Service
class OrderAppService {
    private final JdbcTemplate jdbc;
    private final TransactionTemplate tx;
    private final UserAppService userAppService;

    OrderAppService(JdbcTemplate jdbc, TransactionTemplate tx, UserAppService userAppService) {
        this.jdbc = jdbc;
        this.tx = tx;
        this.userAppService = userAppService;
    }

    Map<String, Object> create(UserContext ctx, OrderCreateRequest request) {
        return tx.execute(status -> {
            Map<String, Object> service = jdbc.queryForMap("""
                SELECT id, seller_id, campus_id, price, status FROM service_item WHERE id = ? AND is_deleted = 0
                """, request.serviceId());
            if (((Number) service.get("status")).intValue() != 1) {
                throw new BusinessException(40004, "服务未上架");
            }
            Long sellerId = ((Number) service.get("seller_id")).longValue();
            if (sellerId.equals(ctx.userId())) {
                throw new BusinessException(20009, "不能购买自己的服务");
            }
            int updated = jdbc.update("""
                UPDATE service_item SET used_stock = used_stock + 1
                WHERE id = ? AND stock > used_stock AND status = 1
                """, request.serviceId());
            if (updated == 0) {
                throw new BusinessException(20005, "库存不足");
            }
            String orderNo = "JJ" + DateTimeFormatter.ofPattern("yyyyMMddHHmmss").format(LocalDateTime.now()) + ctx.userId();
            BigDecimal amount = (BigDecimal) service.get("price");
            Long orderId = insertAndReturnId("""
                INSERT INTO order_main
                (order_no, buyer_id, seller_id, service_id, campus_id, amount, status, remark, expire_time)
                VALUES (?, ?, ?, ?, ?, ?, 10, ?, ?)
                """, orderNo, ctx.userId(), sellerId, request.serviceId(),
                    ((Number) service.get("campus_id")).longValue(), amount, request.remark(),
                    Timestamp.valueOf(LocalDateTime.now().plusMinutes(15)));
            insertOrderLog(orderId, null, 10, ctx.userId(), "创建订单");
            userAppService.ensureSellerAccount(sellerId);
            return Map.of("orderId", orderId, "orderNo", orderNo, "amount", amount, "status", 10);
        });
    }

    List<Map<String, Object>> list(UserContext ctx, String role) {
        if ("seller".equals(role)) {
            return jdbc.queryForList("""
                SELECT id, order_no AS orderNo, buyer_id AS buyerId, seller_id AS sellerId, service_id AS serviceId,
                       amount, status, create_time AS createTime
                FROM order_main WHERE seller_id = ? AND is_deleted = 0 ORDER BY id DESC
                """, ctx.userId());
        }
        return jdbc.queryForList("""
            SELECT id, order_no AS orderNo, buyer_id AS buyerId, seller_id AS sellerId, service_id AS serviceId,
                   amount, status, create_time AS createTime
            FROM order_main WHERE buyer_id = ? AND is_deleted = 0 ORDER BY id DESC
            """, ctx.userId());
    }

    Map<String, Object> detail(UserContext ctx, Long orderId) {
        Map<String, Object> order = jdbc.queryForMap("""
            SELECT id, order_no AS orderNo, buyer_id AS buyerId, seller_id AS sellerId, service_id AS serviceId,
                   amount, status, remark, deliver_text AS deliverText, create_time AS createTime
            FROM order_main WHERE id = ? AND is_deleted = 0
            """, orderId);
        assertOrderParticipant(ctx, order);
        return order;
    }

    void accept(UserContext ctx, Long orderId) {
        changeBySeller(ctx, orderId, 20, 30, "讲师接单",
                "UPDATE order_main SET status = 30, accept_time = CURRENT_TIMESTAMP WHERE id = ? AND status = 20");
    }

    void deliver(UserContext ctx, Long orderId, String deliverText) {
        changeBySeller(ctx, orderId, 30, 40, "讲师提交交付凭证",
                "UPDATE order_main SET status = 40, deliver_text = ?, deliver_time = CURRENT_TIMESTAMP, expire_time = ? WHERE id = ? AND status = 30",
                deliverText, Timestamp.valueOf(LocalDateTime.now().plusDays(7)));
    }

    void confirm(UserContext ctx, Long orderId) {
        tx.executeWithoutResult(status -> {
            Map<String, Object> order = lockedOrder(orderId);
            if (!ctx.userId().equals(((Number) order.get("buyer_id")).longValue())) {
                throw new BusinessException(20010, "无权操作该订单");
            }
            int oldStatus = ((Number) order.get("status")).intValue();
            if (oldStatus != 40) {
                throw new BusinessException(20008, "订单状态不允许确认");
            }
            jdbc.update("UPDATE order_main SET status = 50, confirm_time = CURRENT_TIMESTAMP WHERE id = ? AND status = 40", orderId);
            Long sellerId = ((Number) order.get("seller_id")).longValue();
            BigDecimal amount = (BigDecimal) order.get("amount");
            BigDecimal settleAmount = amount.multiply(new BigDecimal("0.90"));
            jdbc.update("""
                UPDATE seller_account
                SET frozen_balance = frozen_balance + ?, total_income = total_income + ?
                WHERE seller_id = ?
                """, settleAmount, settleAmount, sellerId);
            jdbc.update("UPDATE service_item SET sales_count = sales_count + 1 WHERE id = ?", ((Number) order.get("service_id")).longValue());
            jdbc.update("""
                INSERT INTO account_flow (seller_id, order_id, flow_type, amount, balance_after, remark)
                SELECT seller_id, ?, 'FROZEN_ADD', ?, frozen_balance, '订单完成入冻结余额'
                FROM seller_account WHERE seller_id = ?
                """, orderId, settleAmount, sellerId);
            insertOrderLog(orderId, oldStatus, 50, ctx.userId(), "买家确认完成");
        });
    }

    void insertOrderLog(Long orderId, Integer fromStatus, Integer toStatus, Long operatorId, String remark) {
        jdbc.update("""
            INSERT INTO order_log (order_id, from_status, to_status, operator_id, remark)
            VALUES (?, ?, ?, ?, ?)
            """, orderId, fromStatus, toStatus, operatorId, remark);
    }

    void assertOrderParticipant(UserContext ctx, Map<String, Object> order) {
        Long buyerId = ((Number) order.get("buyerId")).longValue();
        Long sellerId = ((Number) order.get("sellerId")).longValue();
        if (!ctx.userId().equals(buyerId) && !ctx.userId().equals(sellerId)) {
            throw new BusinessException(20010, "无权查看该订单");
        }
    }

    private void changeBySeller(UserContext ctx, Long orderId, int from, int to, String remark, String sql, Object... extraArgs) {
        tx.executeWithoutResult(status -> {
            Map<String, Object> order = lockedOrder(orderId);
            if (!ctx.userId().equals(((Number) order.get("seller_id")).longValue())) {
                throw new BusinessException(20010, "无权操作该订单");
            }
            if (((Number) order.get("status")).intValue() != from) {
                throw new BusinessException(20008, "订单状态不允许流转");
            }
            Object[] args;
            if (extraArgs.length == 0) {
                args = new Object[]{orderId};
            } else {
                args = new Object[extraArgs.length + 1];
                System.arraycopy(extraArgs, 0, args, 0, extraArgs.length);
                args[extraArgs.length] = orderId;
            }
            jdbc.update(sql, args);
            insertOrderLog(orderId, from, to, ctx.userId(), remark);
        });
    }

    private Map<String, Object> lockedOrder(Long orderId) {
        return jdbc.queryForMap("SELECT * FROM order_main WHERE id = ? AND is_deleted = 0", orderId);
    }

    private Long insertAndReturnId(String sql, Object... args) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            for (int i = 0; i < args.length; i++) {
                ps.setObject(i + 1, args[i]);
            }
            return ps;
        }, keyHolder);
        return Objects.requireNonNull(keyHolder.getKey()).longValue();
    }
}

@Service
class PaymentAppService {
    private final JdbcTemplate jdbc;
    private final TransactionTemplate tx;
    private final ExternalClients.PaymentClient paymentClient;
    private final OrderAppService orderAppService;

    PaymentAppService(JdbcTemplate jdbc, TransactionTemplate tx, ExternalClients.PaymentClient paymentClient,
                      OrderAppService orderAppService) {
        this.jdbc = jdbc;
        this.tx = tx;
        this.paymentClient = paymentClient;
        this.orderAppService = orderAppService;
    }

    Map<String, Object> mockPay(UserContext ctx, Long orderId) {
        return tx.execute(status -> {
            Map<String, Object> order = jdbc.queryForMap("SELECT * FROM order_main WHERE id = ? AND is_deleted = 0", orderId);
            if (!ctx.userId().equals(((Number) order.get("buyer_id")).longValue())) {
                throw new BusinessException(30003, "无权支付该订单");
            }
            if (((Number) order.get("status")).intValue() != 10) {
                throw new BusinessException(30004, "订单状态不允许支付");
            }
            var pay = paymentClient.createPay(orderId, (String) order.get("order_no"), (BigDecimal) order.get("amount"));
            Integer exists = jdbc.queryForObject("SELECT COUNT(*) FROM payment_record WHERE order_id = ?", Integer.class, orderId);
            if (exists == null || exists == 0) {
                jdbc.update("""
                    INSERT INTO payment_record (order_id, out_trade_no, transaction_id, amount, status, pay_channel)
                    VALUES (?, ?, ?, ?, 2, ?)
                    """, orderId, pay.outTradeNo(), pay.transactionId(), pay.amount(), pay.payChannel());
            }
            jdbc.update("UPDATE order_main SET status = 20, pay_time = CURRENT_TIMESTAMP, expire_time = ? WHERE id = ? AND status = 10",
                    Timestamp.valueOf(LocalDateTime.now().plusHours(24)), orderId);
            orderAppService.insertOrderLog(orderId, 10, 20, ctx.userId(), "mock 支付成功");
            return Map.of("orderId", orderId, "status", 20, "transactionId", pay.transactionId(), "payChannel", pay.payChannel());
        });
    }
}

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
        Long reviewId = insertAndReturnId("""
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

    private Long insertAndReturnId(String sql, Object... args) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            for (int i = 0; i < args.length; i++) {
                ps.setObject(i + 1, args[i]);
            }
            return ps;
        }, keyHolder);
        return Objects.requireNonNull(keyHolder.getKey()).longValue();
    }
}

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
        return insertAndReturnId("""
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

    private Long insertAndReturnId(String sql, Object... args) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            for (int i = 0; i < args.length; i++) {
                ps.setObject(i + 1, args[i]);
            }
            return ps;
        }, keyHolder);
        return Objects.requireNonNull(keyHolder.getKey()).longValue();
    }
}

@Service
class AdminAppService {
    private final JdbcTemplate jdbc;
    private final UserAppService userAppService;

    AdminAppService(JdbcTemplate jdbc, UserAppService userAppService) {
        this.jdbc = jdbc;
        this.userAppService = userAppService;
    }

    List<Map<String, Object>> pendingVerifies() {
        return jdbc.queryForList("""
            SELECT id, user_id AS userId, campus_id AS campusId, cert_type AS certType, cert_image_url AS certImageUrl,
                   real_name AS realName, student_no AS studentNo, ocr_confidence AS ocrConfidence, create_time AS createTime
            FROM user_verify_record
            WHERE status = 1 AND is_deleted = 0
            ORDER BY id ASC
            """);
    }

    void reviewVerify(Long recordId, boolean passed, String reason) {
        Map<String, Object> record = jdbc.queryForMap("SELECT * FROM user_verify_record WHERE id = ? AND is_deleted = 0", recordId);
        int status = passed ? 2 : 3;
        jdbc.update("""
            UPDATE user_verify_record
            SET status = ?, reject_reason = ?, review_time = CURRENT_TIMESTAMP
            WHERE id = ?
            """, status, reason, recordId);
        jdbc.update("""
            UPDATE `user`
            SET verify_status = ?, is_seller_verified = ?, campus_id = ?, real_name_encrypted = ?, student_no_encrypted = ?
            WHERE id = ?
            """, status, passed ? 1 : 0, ((Number) record.get("campus_id")).longValue(),
            record.get("real_name"), record.get("student_no"), ((Number) record.get("user_id")).longValue());
        if (passed) {
            userAppService.ensureSellerAccount(((Number) record.get("user_id")).longValue());
        }
        jdbc.update("""
            INSERT INTO operation_log (operation, target_type, target_id, detail)
            VALUES ('VERIFY_REVIEW', 'user_verify_record', ?, ?)
            """, recordId, passed ? "通过" : "驳回：" + reason);
    }

    List<Map<String, Object>> pendingServices() {
        return jdbc.queryForList("""
            SELECT id, seller_id AS sellerId, campus_id AS campusId, category_id AS categoryId,
                   title, description, price, stock, create_time AS createTime
            FROM service_item
            WHERE status = 0 AND is_deleted = 0
            ORDER BY id ASC
            """);
    }

    void reviewService(Long serviceId, boolean passed, String reason) {
        jdbc.update("UPDATE service_item SET status = ? WHERE id = ?", passed ? 1 : 2, serviceId);
        jdbc.update("""
            INSERT INTO operation_log (operation, target_type, target_id, detail)
            VALUES ('SERVICE_REVIEW', 'service_item', ?, ?)
            """, serviceId, passed ? "上架" : "驳回：" + reason);
    }
}

record VerifySubmitRequest(Long campusId, Integer certType, String certImageUrl, String realName, String studentNo) {
}

record ServicePublishRequest(Long categoryId, String title, String description, BigDecimal price, String priceConfig,
                             String coverUrl, Integer stock) {
}

record OrderCreateRequest(Long serviceId, String remark) {
}

record ReviewSubmitRequest(Long orderId, Integer score, String content) {
}

record MessageSendRequest(Long orderId, String content) {
}

record AdminReviewRequest(Long id, Boolean passed, String reason) {
}
