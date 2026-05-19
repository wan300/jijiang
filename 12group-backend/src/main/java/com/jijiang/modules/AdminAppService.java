package com.jijiang.modules;

import com.jijiang.common.AdminContext;
import com.jijiang.common.BusinessException;
import com.jijiang.common.EncryptionService;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
class AdminAppService {
    private final JdbcTemplate jdbc;
    private final UserAppService userAppService;
    private final EncryptionService encryptionService;
    private final VerifyAuditService verifyAuditService;
    private final RefundAppService refundAppService;

    AdminAppService(JdbcTemplate jdbc, UserAppService userAppService, EncryptionService encryptionService,
                    VerifyAuditService verifyAuditService, RefundAppService refundAppService) {
        this.jdbc = jdbc;
        this.userAppService = userAppService;
        this.encryptionService = encryptionService;
        this.verifyAuditService = verifyAuditService;
        this.refundAppService = refundAppService;
    }

    Map<String, Object> dashboardOverview() {
        Map<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("userTotal", count("SELECT COUNT(*) FROM `user` WHERE is_deleted = 0"));
        metrics.put("verifiedUserTotal", count("SELECT COUNT(*) FROM `user` WHERE verify_status = 2 AND is_deleted = 0"));
        metrics.put("pendingVerifyTotal", count("SELECT COUNT(*) FROM user_verify_record WHERE status = 1 AND is_deleted = 0"));
        metrics.put("pendingServiceTotal", count("SELECT COUNT(*) FROM service_item WHERE status = 0 AND is_deleted = 0"));
        metrics.put("onlineServiceTotal", count("SELECT COUNT(*) FROM service_item WHERE status = 1 AND is_deleted = 0"));
        metrics.put("orderTotal", count("SELECT COUNT(*) FROM order_main WHERE is_deleted = 0"));
        metrics.put("paidGmv", scalar("SELECT COALESCE(SUM(amount), 0) FROM order_main WHERE status >= 20 AND is_deleted = 0"));
        metrics.put("todayOrderTotal", count("SELECT COUNT(*) FROM order_main WHERE DATE(create_time) = CURRENT_DATE AND is_deleted = 0"));

        List<Map<String, Object>> orderStatus = jdbc.queryForList("""
            SELECT status, COUNT(*) AS total
            FROM order_main
            WHERE is_deleted = 0
            GROUP BY status
            ORDER BY status ASC
            """);
        List<Map<String, Object>> topServices = jdbc.queryForList("""
            SELECT s.id, s.title, s.sales_count AS salesCount, s.score_avg AS scoreAvg,
                   u.nickname AS sellerName
            FROM service_item s LEFT JOIN `user` u ON u.id = s.seller_id
            WHERE s.is_deleted = 0
            ORDER BY s.sales_count DESC, s.id DESC
            LIMIT 5
            """);
        return Map.of("metrics", metrics, "orderStatus", orderStatus, "topServices", topServices);
    }

    Map<String, Object> pendingVerifies(Integer page, Integer pageSize, String keyword) {
        int safePage = safePage(page);
        int safePageSize = safePageSize(pageSize);
        List<Object> args = new ArrayList<>();
        StringBuilder where = new StringBuilder("""
            FROM user_verify_record v
            LEFT JOIN `user` u ON u.id = v.user_id
            LEFT JOIN campus c ON c.id = v.campus_id
            WHERE v.status = 1 AND v.is_deleted = 0
            """);
        addKeyword(where, args, keyword, "v.real_name", "v.student_no", "u.nickname", "c.name");
        long total = count("SELECT COUNT(*) " + where, args);
        args.add(safePageSize);
        args.add(offset(safePage, safePageSize));
        List<Map<String, Object>> items = jdbc.queryForList("""
            SELECT v.id, v.user_id AS userId, u.nickname AS nickname, v.campus_id AS campusId,
                   c.name AS campusName, v.cert_type AS certType, v.cert_image_url AS certImageUrl,
                   v.real_name AS realName, v.student_no AS studentNo, v.ocr_confidence AS ocrConfidence,
                   v.create_time AS createTime
            """ + where + " ORDER BY v.id ASC LIMIT ? OFFSET ?", args.toArray());
        return pageOf(items, total, safePage, safePageSize);
    }

    void reviewVerify(AdminContext ctx, Long recordId, boolean passed, String reason) {
        if (recordId == null) {
            throw new BusinessException(80011, "审核记录不能为空");
        }
        Map<String, Object> record = mustQueryForMap(
                "SELECT * FROM user_verify_record WHERE id = ? AND is_deleted = 0",
                "实名审核记录不存在",
                recordId);
        int status = passed ? 2 : 3;
        jdbc.update("""
            UPDATE user_verify_record
            SET status = ?, reviewer_id = ?, reject_reason = ?, review_time = CURRENT_TIMESTAMP, update_time = CURRENT_TIMESTAMP
            WHERE id = ?
            """, status, ctx.adminId(), passed ? null : safeReason(reason), recordId);
        Long userId = ((Number) record.get("user_id")).longValue();
        Long campusId = ((Number) record.get("campus_id")).longValue();
        if (passed) {
            String encryptedName = encryptionService.encrypt((String) record.get("real_name"));
            String encryptedNo = encryptionService.encrypt((String) record.get("student_no"));
            jdbc.update("""
                UPDATE `user`
                SET verify_status = 2, is_seller_verified = 1, campus_id = ?,
                    real_name_encrypted = ?, student_no_encrypted = ?, update_time = CURRENT_TIMESTAMP
                WHERE id = ?
                """, campusId, encryptedName, encryptedNo, userId);
            userAppService.ensureSellerAccount(userId);
            verifyAuditService.log(userId, recordId, "ADMIN_APPROVE", "管理员审核通过", ctx.adminId());
        } else {
            jdbc.update("""
                UPDATE `user`
                SET verify_status = 3, is_seller_verified = 0, campus_id = ?, update_time = CURRENT_TIMESTAMP
                WHERE id = ?
                """, campusId, userId);
            verifyAuditService.log(userId, recordId, "ADMIN_REJECT", "管理员驳回: " + safeReason(reason), ctx.adminId());
        }
        insertOperation(ctx, "VERIFY_REVIEW", "user_verify_record", recordId,
                passed ? "通过" : "驳回：" + safeReason(reason));
    }

    Map<String, Object> pendingServices(Integer page, Integer pageSize, String keyword) {
        return serviceList(0, keyword, page, pageSize);
    }

    Map<String, Object> serviceList(Integer status, String keyword, Integer page, Integer pageSize) {
        int safePage = safePage(page);
        int safePageSize = safePageSize(pageSize);
        List<Object> args = new ArrayList<>();
        StringBuilder where = new StringBuilder("""
            FROM service_item s
            LEFT JOIN `user` u ON u.id = s.seller_id
            LEFT JOIN category c ON c.id = s.category_id
            WHERE s.is_deleted = 0
            """);
        if (status != null) {
            where.append(" AND s.status = ?");
            args.add(status);
        }
        addKeyword(where, args, keyword, "s.title", "s.description", "u.nickname", "c.name");
        long total = count("SELECT COUNT(*) " + where, args);
        args.add(safePageSize);
        args.add(offset(safePage, safePageSize));
        List<Map<String, Object>> items = jdbc.queryForList("""
            SELECT s.id, s.seller_id AS sellerId, u.nickname AS sellerName, s.campus_id AS campusId,
                   s.category_id AS categoryId, c.name AS categoryName, s.title, s.description,
                   s.price, s.cover_url AS coverUrl, s.stock, s.used_stock AS usedStock, s.status,
                   s.score_avg AS scoreAvg, s.sales_count AS salesCount, s.create_time AS createTime
            """ + where + " ORDER BY s.id DESC LIMIT ? OFFSET ?", args.toArray());
        return pageOf(items, total, safePage, safePageSize);
    }

    void reviewService(AdminContext ctx, Long serviceId, boolean passed, String reason) {
        if (serviceId == null) {
            throw new BusinessException(80021, "服务编号不能为空");
        }
        mustQueryForMap("SELECT id FROM service_item WHERE id = ? AND is_deleted = 0", "服务不存在", serviceId);
        jdbc.update("UPDATE service_item SET status = ?, update_time = CURRENT_TIMESTAMP WHERE id = ?",
                passed ? 1 : 2, serviceId);
        insertOperation(ctx, "SERVICE_REVIEW", "service_item", serviceId,
                passed ? "上架" : "驳回：" + safeReason(reason));
    }

    void offlineService(AdminContext ctx, Long serviceId, String reason) {
        if (serviceId == null) {
            throw new BusinessException(80021, "服务编号不能为空");
        }
        mustQueryForMap("SELECT id FROM service_item WHERE id = ? AND is_deleted = 0", "服务不存在", serviceId);
        jdbc.update("UPDATE service_item SET status = 2, update_time = CURRENT_TIMESTAMP WHERE id = ?", serviceId);
        insertOperation(ctx, "SERVICE_OFFLINE", "service_item", serviceId, "下架：" + safeReason(reason));
    }

    Map<String, Object> orderList(Integer status, String keyword, Integer page, Integer pageSize) {
        int safePage = safePage(page);
        int safePageSize = safePageSize(pageSize);
        List<Object> args = new ArrayList<>();
        StringBuilder where = new StringBuilder("""
            FROM order_main o
            LEFT JOIN `user` buyer ON buyer.id = o.buyer_id
            LEFT JOIN `user` seller ON seller.id = o.seller_id
            LEFT JOIN service_item s ON s.id = o.service_id
            WHERE o.is_deleted = 0
            """);
        if (status != null) {
            where.append(" AND o.status = ?");
            args.add(status);
        }
        addKeyword(where, args, keyword, "o.order_no", "buyer.nickname", "seller.nickname", "s.title");
        long total = count("SELECT COUNT(*) " + where, args);
        args.add(safePageSize);
        args.add(offset(safePage, safePageSize));
        List<Map<String, Object>> items = jdbc.queryForList("""
            SELECT o.id, o.order_no AS orderNo, o.buyer_id AS buyerId, buyer.nickname AS buyerName,
                   o.seller_id AS sellerId, seller.nickname AS sellerName, o.service_id AS serviceId,
                   s.title AS serviceTitle, o.amount, o.status, o.create_time AS createTime,
                   o.pay_time AS payTime, o.confirm_time AS confirmTime
            """ + where + " ORDER BY o.id DESC LIMIT ? OFFSET ?", args.toArray());
        return pageOf(items, total, safePage, safePageSize);
    }

    Map<String, Object> orderDetail(Long orderId) {
        if (orderId == null) {
            throw new BusinessException(80031, "订单编号不能为空");
        }
        Map<String, Object> order = mustQueryForMap("""
            SELECT o.id, o.order_no AS orderNo, o.buyer_id AS buyerId, buyer.nickname AS buyerName,
                   o.seller_id AS sellerId, seller.nickname AS sellerName, o.service_id AS serviceId,
                   s.title AS serviceTitle, s.cover_url AS serviceCoverUrl, o.amount, o.status,
                   o.remark, o.deliver_text AS deliverText, o.create_time AS createTime,
                   o.expire_time AS expireTime, o.pay_time AS payTime, o.accept_time AS acceptTime,
                   o.deliver_time AS deliverTime, o.confirm_time AS confirmTime
            FROM order_main o
            LEFT JOIN `user` buyer ON buyer.id = o.buyer_id
            LEFT JOIN `user` seller ON seller.id = o.seller_id
            LEFT JOIN service_item s ON s.id = o.service_id
            WHERE o.id = ? AND o.is_deleted = 0
            """, "订单不存在", orderId);
        List<Map<String, Object>> payments = jdbc.queryForList("""
            SELECT id, out_trade_no AS outTradeNo, transaction_id AS transactionId, amount, status,
                   pay_channel AS payChannel, create_time AS createTime
            FROM payment_record
            WHERE order_id = ? AND is_deleted = 0
            ORDER BY id DESC
            """, orderId);
        List<Map<String, Object>> logs = jdbc.queryForList("""
            SELECT id, from_status AS fromStatus, to_status AS toStatus, operator_id AS operatorId,
                   remark, create_time AS createTime
            FROM order_log
            WHERE order_id = ? AND is_deleted = 0
            ORDER BY id ASC
            """, orderId);
        List<Map<String, Object>> messages = jdbc.queryForList("""
            SELECT id, sender_id AS senderId, receiver_id AS receiverId, content, is_read AS isRead,
                   create_time AS createTime
            FROM message
            WHERE order_id = ? AND is_deleted = 0
            ORDER BY id ASC
            """, orderId);
        return Map.of("order", order, "payments", payments, "logs", logs, "messages", messages);
    }

    private void insertOperation(AdminContext ctx, String operation, String targetType, Long targetId, String detail) {
        jdbc.update("""
            INSERT INTO operation_log (operator_id, operation, target_type, target_id, detail)
            VALUES (?, ?, ?, ?, ?)
            """, ctx.adminId(), operation, targetType, targetId, detail);
    }

    private void addKeyword(StringBuilder where, List<Object> args, String keyword, String... columns) {
        if (keyword == null || keyword.isBlank()) {
            return;
        }
        String like = "%" + keyword.trim() + "%";
        where.append(" AND (");
        for (int i = 0; i < columns.length; i++) {
            if (i > 0) {
                where.append(" OR ");
            }
            where.append(columns[i]).append(" LIKE ?");
            args.add(like);
        }
        where.append(")");
    }

    private Map<String, Object> pageOf(List<Map<String, Object>> items, long total, int page, int pageSize) {
        return Map.of("items", items, "total", total, "page", page, "pageSize", pageSize);
    }

    private int safePage(Integer page) {
        return page == null || page < 1 ? 1 : page;
    }

    private int safePageSize(Integer pageSize) {
        if (pageSize == null || pageSize < 1) {
            return 20;
        }
        return Math.min(pageSize, 100);
    }

    private int offset(int page, int pageSize) {
        return (page - 1) * pageSize;
    }

    private long count(String sql, List<Object> args) {
        Number value = jdbc.queryForObject(sql, Number.class, args.toArray());
        return value == null ? 0L : value.longValue();
    }

    private long count(String sql) {
        Number value = jdbc.queryForObject(sql, Number.class);
        return value == null ? 0L : value.longValue();
    }

    private Object scalar(String sql) {
        return jdbc.queryForObject(sql, Object.class);
    }

    private Map<String, Object> mustQueryForMap(String sql, String missingMessage, Object... args) {
        try {
            return jdbc.queryForMap(sql, args);
        } catch (EmptyResultDataAccessException e) {
            throw new BusinessException(80004, missingMessage);
        }
    }

    private String safeReason(String reason) {
        return reason == null || reason.isBlank() ? "未填写原因" : reason.trim();
    }

    Map<String, Object> refundList(Integer status, String keyword, Integer page, Integer pageSize) {
        return refundAppService.adminRefundList(status, keyword, page, pageSize);
    }

    Map<String, Object> refundDetail(Long refundId) {
        return refundAppService.refundDetail(refundId);
    }

    void reviewRefund(AdminContext ctx, Long refundId, boolean passed, String reason, BigDecimal deductDeposit) {
        refundAppService.reviewRefund(ctx.adminId(), refundId, passed, reason, deductDeposit);
    }
}
