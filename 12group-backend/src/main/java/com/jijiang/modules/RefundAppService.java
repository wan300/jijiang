package com.jijiang.modules;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jijiang.common.BusinessException;
import com.jijiang.common.UserContext;
import com.jijiang.infra.PaymentServerClient;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
class RefundAppService {
    private final JdbcTemplate jdbc;
    private final TransactionTemplate tx;
    private final ObjectMapper objectMapper;
    private final PaymentServerClient paymentServerClient;
    private final OrderAppService orderAppService;

    RefundAppService(JdbcTemplate jdbc, TransactionTemplate tx, ObjectMapper objectMapper,
                     PaymentServerClient paymentServerClient, OrderAppService orderAppService) {
        this.jdbc = jdbc;
        this.tx = tx;
        this.objectMapper = objectMapper;
        this.paymentServerClient = paymentServerClient;
        this.orderAppService = orderAppService;
    }

    Map<String, Object> submitRefund(UserContext ctx, RefundSubmitRequest request) {
        return tx.execute(status -> {
            Map<String, Object> order = jdbc.queryForMap(
                    "SELECT * FROM order_main WHERE id = ? AND is_deleted = 0", request.orderId());
            if (!ctx.userId().equals(((Number) order.get("buyer_id")).longValue())) {
                throw new BusinessException(30057, "无权申请退款");
            }
            int orderStatus = ((Number) order.get("status")).intValue();
            if (orderStatus != 20 && orderStatus != 30 && orderStatus != 40) {
                throw new BusinessException(30058, "当前订单状态不支持退款");
            }
            Integer pendingCount = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM refund_request WHERE order_id = ? AND status = 0 AND is_deleted = 0",
                    Integer.class, request.orderId());
            if (pendingCount != null && pendingCount > 0) {
                throw new BusinessException(30061, "该订单已有待处理的退款申请");
            }
            Long sellerId = ((Number) order.get("seller_id")).longValue();
            BigDecimal amount = (BigDecimal) order.get("amount");
            String evidenceJson = toJsonArray(request.evidenceUrls());
            Long refundId = JdbcHelper.insertAndReturnId(jdbc, """
                INSERT INTO refund_request (order_id, user_id, seller_id, reason, evidence_urls, amount, status)
                VALUES (?, ?, ?, ?, ?, ?, 0)
                """, request.orderId(), ctx.userId(), sellerId, request.reason(), evidenceJson, amount);

            if (orderStatus == 20) {
                Map<String, Object> payment = queryPaymentByOrderId(request.orderId());
                autoRefund(order, payment, refundId);
            }

            Map<String, Object> refund = jdbc.queryForMap(
                    "SELECT * FROM refund_request WHERE id = ? AND is_deleted = 0", refundId);
            return toRefundMap(refund);
        });
    }

    private void autoRefund(Map<String, Object> order, Map<String, Object> payment, Long refundId) {
        if (payment == null || payment.get("out_trade_no") == null) {
            throw new BusinessException(30062, "该订单无支付记录，无法自动退款");
        }
        String tradeOrderId = (String) payment.get("out_trade_no");
        BigDecimal amount = (BigDecimal) order.get("amount");
        try {
            var response = paymentServerClient.refundPayment(
                    new PaymentServerClient.RefundRequest(tradeOrderId, amount, "卖家未接单，自动退款"));
            if (response.success()) {
                completeRefund(refundId, (Long) order.get("id"), null, response.refundId());
            } else {
                jdbc.update("UPDATE refund_request SET review_remark = ? WHERE id = ?",
                        "自动退款失败: " + response.message(), refundId);
            }
        } catch (Exception e) {
            jdbc.update("UPDATE refund_request SET review_remark = ? WHERE id = ?",
                    "自动退款异常: " + e.getMessage(), refundId);
        }
    }

    List<Map<String, Object>> listMyRefunds(UserContext ctx) {
        List<Map<String, Object>> rows = jdbc.queryForList("""
            SELECT r.*, o.order_no AS orderNo, o.amount AS orderAmount
            FROM refund_request r
            LEFT JOIN order_main o ON o.id = r.order_id
            WHERE r.user_id = ? AND r.is_deleted = 0
            ORDER BY r.id DESC
            """, ctx.userId());
        List<Map<String, Object>> result = new ArrayList<>();
        for (var row : rows) {
            result.add(toRefundMap(row));
        }
        return result;
    }

    Map<String, Object> refundDetail(Long refundId) {
        Map<String, Object> refund = jdbc.queryForMap("""
            SELECT r.*, o.order_no AS orderNo, o.amount AS orderAmount, o.status AS orderStatus
            FROM refund_request r
            LEFT JOIN order_main o ON o.id = r.order_id
            WHERE r.id = ? AND r.is_deleted = 0
            """, refundId);
        return toRefundMap(refund);
    }

    Map<String, Object> adminRefundList(Integer status, String keyword, Integer page, Integer pageSize) {
        int safePage = page == null || page < 1 ? 1 : page;
        int safePageSize = pageSize == null || pageSize < 1 ? 20 : Math.min(pageSize, 100);
        List<Object> args = new ArrayList<>();
        StringBuilder where = new StringBuilder("""
            FROM refund_request r
            LEFT JOIN `user` buyer ON buyer.id = r.user_id
            LEFT JOIN `user` seller ON seller.id = r.seller_id
            WHERE r.is_deleted = 0
            """);
        if (status != null) {
            where.append(" AND r.status = ?");
            args.add(status);
        }
        if (keyword != null && !keyword.isBlank()) {
            String like = "%" + keyword.trim() + "%";
            where.append(" AND (r.reason LIKE ? OR buyer.nickname LIKE ? OR seller.nickname LIKE ?)");
            args.add(like);
            args.add(like);
            args.add(like);
        }
        long total = jdbc.queryForObject("SELECT COUNT(*) " + where, Long.class, args.toArray());
        args.add(safePageSize);
        args.add((safePage - 1) * safePageSize);
        List<Map<String, Object>> items = jdbc.queryForList("""
            SELECT r.id, r.order_id AS orderId, r.user_id AS userId, buyer.nickname AS buyerName,
                   r.seller_id AS sellerId, seller.nickname AS sellerName, r.reason,
                   r.amount, r.status, r.review_remark AS reviewRemark,
                   r.review_time AS reviewTime, r.deduct_deposit AS deductDeposit,
                   r.create_time AS createTime
            """ + where + " ORDER BY r.id DESC LIMIT ? OFFSET ?", args.toArray());
        return Map.of("items", items, "total", total, "page", safePage, "pageSize", safePageSize);
    }

    void reviewRefund(Long reviewerId, Long refundId, boolean passed, String reason, BigDecimal deductDeposit) {
        tx.executeWithoutResult(status -> {
            Map<String, Object> refund = jdbc.queryForMap(
                    "SELECT * FROM refund_request WHERE id = ? AND is_deleted = 0", refundId);
            if (((Number) refund.get("status")).intValue() != 0) {
                throw new BusinessException(30063, "退款申请已处理");
            }
            if (passed) {
                Long orderId = ((Number) refund.get("order_id")).longValue();
                Map<String, Object> order = jdbc.queryForMap(
                        "SELECT * FROM order_main WHERE id = ? AND is_deleted = 0", orderId);
                Map<String, Object> payment = queryPaymentByOrderId(orderId);
                if (payment == null || payment.get("out_trade_no") == null) {
                    throw new BusinessException(30062, "该订单无支付记录，无法退款");
                }
                String tradeOrderId = (String) payment.get("out_trade_no");
                BigDecimal amount = (BigDecimal) order.get("amount");
                var response = paymentServerClient.refundPayment(
                        new PaymentServerClient.RefundRequest(tradeOrderId, amount, reason));
                if (!response.success()) {
                    throw new BusinessException(30060, "退款请求失败: " + response.message());
                }
                completeRefund(refundId, orderId, reviewerId, response.refundId());
                if (deductDeposit != null && deductDeposit.compareTo(BigDecimal.ZERO) > 0) {
                    deductSellerDeposit(((Number) refund.get("seller_id")).longValue(),
                            orderId, deductDeposit, reason);
                }
            } else {
                jdbc.update("""
                    UPDATE refund_request
                    SET status = 2, reviewer_id = ?, review_remark = ?, review_time = CURRENT_TIMESTAMP
                    WHERE id = ?
                    """, reviewerId, reason, refundId);
            }
        });
    }

    private void completeRefund(Long refundId, Long orderId, Long reviewerId, String refundTransactionId) {
        jdbc.update("""
            UPDATE refund_request
            SET status = 1, reviewer_id = ?, review_remark = ?, review_time = CURRENT_TIMESTAMP
            WHERE id = ?
            """, reviewerId, "退款成功, 流水号: " + refundTransactionId, refundId);
        int orderUpdated = jdbc.update(
                "UPDATE order_main SET status = 80 WHERE id = ? AND status IN (20, 30, 40) AND is_deleted = 0",
                orderId);
        if (orderUpdated > 0) {
            orderAppService.insertOrderLog(orderId, null, 80, reviewerId != null ? reviewerId : 0L,
                    "退款完成, 流水号: " + refundTransactionId);
        }
    }

    private void deductSellerDeposit(Long sellerId, Long orderId, BigDecimal amount, String reason) {
        jdbc.update("UPDATE seller_account SET deposit_amount = deposit_amount - ? WHERE seller_id = ? AND deposit_amount >= ?",
                amount, sellerId, amount);
        jdbc.update("""
            INSERT INTO deposit_deduction (seller_id, order_id, amount, reason)
            VALUES (?, ?, ?, ?)
            """, sellerId, orderId, amount, reason);
    }

    private Map<String, Object> queryPaymentByOrderId(Long orderId) {
        try {
            return jdbc.queryForMap("""
                SELECT * FROM payment_record
                WHERE order_id = ? AND is_deleted = 0
                ORDER BY id DESC LIMIT 1
                """, orderId);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    private Map<String, Object> toRefundMap(Map<String, Object> row) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", row.get("id"));
        map.put("orderId", row.get("order_id"));
        map.put("userId", row.get("user_id"));
        map.put("sellerId", row.get("seller_id"));
        map.put("reason", row.get("reason"));
        map.put("evidenceUrls", fromJsonArray((String) row.get("evidence_urls")));
        map.put("amount", row.get("amount"));
        map.put("status", row.get("status"));
        map.put("reviewerId", row.get("reviewer_id"));
        map.put("reviewRemark", row.get("review_remark"));
        map.put("reviewTime", row.get("review_time"));
        map.put("deductDeposit", row.get("deduct_deposit"));
        map.put("createTime", row.get("create_time"));
        map.put("updateTime", row.get("update_time"));
        if (row.containsKey("orderNo")) {
            map.put("orderNo", row.get("orderNo"));
        }
        if (row.containsKey("orderAmount")) {
            map.put("orderAmount", row.get("orderAmount"));
        }
        if (row.containsKey("orderStatus")) {
            map.put("orderStatus", row.get("orderStatus"));
        }
        return map;
    }

    private String toJsonArray(List<String> urls) {
        if (urls == null || urls.isEmpty()) {
            return "[]";
        }
        try {
            return objectMapper.writeValueAsString(urls);
        } catch (JsonProcessingException e) {
            return "[]";
        }
    }

    private List<String> fromJsonArray(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            @SuppressWarnings("unchecked")
            List<String> list = objectMapper.readValue(json, List.class);
            return list == null ? List.of() : list;
        } catch (Exception e) {
            return List.of();
        }
    }
}

record RefundSubmitRequest(Long orderId, String reason, List<String> evidenceUrls) {
}
