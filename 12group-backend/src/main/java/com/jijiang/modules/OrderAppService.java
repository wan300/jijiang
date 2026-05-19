package com.jijiang.modules;

import com.jijiang.common.BusinessException;
import com.jijiang.common.UserContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

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
            Long orderId = JdbcHelper.insertAndReturnId(jdbc, """
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
}
