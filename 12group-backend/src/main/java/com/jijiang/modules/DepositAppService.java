package com.jijiang.modules;

import com.jijiang.common.BusinessException;
import com.jijiang.common.UserContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Service
class DepositAppService {
    private final JdbcTemplate jdbc;
    private final TransactionTemplate tx;

    DepositAppService(JdbcTemplate jdbc, TransactionTemplate tx) {
        this.jdbc = jdbc;
        this.tx = tx;
    }

    static final BigDecimal DEFAULT_DEPOSIT_AMOUNT = new BigDecimal("500.00");

    Map<String, Object> createDeposit(UserContext ctx) {
        checkUserVerified(ctx.userId());

        BigDecimal amount = getDepositAmount();
        List<Map<String, Object>> existing = jdbc.queryForList(
                "SELECT id, status FROM deposit_record WHERE user_id = ? AND status = 0 AND is_deleted = 0",
                ctx.userId());
        if (!existing.isEmpty()) {
            return Map.of("recordId", existing.get(0).get("id"), "amount", amount, "status", 0,
                    "message", "已有待支付保证金记录");
        }

        String outTradeNo = "DEP" + DateTimeFormatter.ofPattern("yyyyMMddHHmmss").format(LocalDateTime.now()) + ctx.userId();
        Long recordId = JdbcHelper.insertAndReturnId(jdbc, """
            INSERT INTO deposit_record (user_id, amount, status, deposit_type, out_trade_no)
            VALUES (?, ?, 0, 'INITIAL', ?)
            """, ctx.userId(), amount, outTradeNo);
        return Map.of("recordId", recordId, "amount", amount, "status", 0, "outTradeNo", outTradeNo,
                "message", "保证金支付单已创建，请完成支付");
    }

    void markDepositPaid(String outTradeNo, String transactionId) {
        tx.executeWithoutResult(status -> {
            int updated = jdbc.update("""
                UPDATE deposit_record
                SET status = 1, transaction_id = ?, pay_time = CURRENT_TIMESTAMP, update_time = CURRENT_TIMESTAMP
                WHERE out_trade_no = ? AND status = 0 AND is_deleted = 0
                """, transactionId, outTradeNo);
            if (updated > 0) {
                Long userId = jdbc.queryForObject(
                        "SELECT user_id FROM deposit_record WHERE out_trade_no = ?", Long.class, outTradeNo);
                jdbc.update("UPDATE `user` SET deposit_paid = 1, update_time = CURRENT_TIMESTAMP WHERE id = ?", userId);
                jdbc.update("""
                    UPDATE seller_account
                    SET deposit_amount = COALESCE(deposit_amount, 0) + (SELECT amount FROM deposit_record WHERE out_trade_no = ?),
                        update_time = CURRENT_TIMESTAMP
                    WHERE seller_id = ?
                    """, outTradeNo, userId);
            }
        });
    }

    Map<String, Object> getDepositStatus(UserContext ctx) {
        Map<String, Object> user = jdbc.queryForMap(
                "SELECT deposit_paid AS depositPaid FROM `user` WHERE id = ?", ctx.userId());
        List<Map<String, Object>> records = jdbc.queryForList("""
            SELECT id, amount, status, deposit_type AS depositType, out_trade_no AS outTradeNo,
                   transaction_id AS transactionId, pay_time AS payTime, refund_time AS refundTime,
                   remark, create_time AS createTime
            FROM deposit_record WHERE user_id = ? AND is_deleted = 0 ORDER BY id DESC
            """, ctx.userId());
        return Map.of("depositPaid", user.get("depositPaid"), "records", records);
    }

    // --- Admin operations ---

    Map<String, Object> listAllDeposits(String keyword, Integer page, Integer pageSize) {
        int safePage = page == null || page < 1 ? 1 : page;
        int safePageSize = pageSize == null || pageSize < 1 ? 20 : Math.min(pageSize, 100);
        int offset = (safePage - 1) * safePageSize;
        String like = keyword == null || keyword.isBlank() ? null : "%" + keyword.trim() + "%";

        List<Object> args = new java.util.ArrayList<>();
        StringBuilder where = new StringBuilder("""
            FROM deposit_record d LEFT JOIN `user` u ON u.id = d.user_id
            WHERE d.is_deleted = 0
            """);
        if (like != null) {
            where.append(" AND (u.nickname LIKE ? OR d.out_trade_no LIKE ?)");
            args.add(like);
            args.add(like);
        }
        Number count = jdbc.queryForObject("SELECT COUNT(*) " + where, Number.class, args.toArray());
        long total = count == null ? 0L : count.longValue();
        args.add(safePageSize);
        args.add(offset);
        List<Map<String, Object>> items = jdbc.queryForList("""
            SELECT d.id, d.user_id AS userId, u.nickname, d.amount, d.status,
                   d.deposit_type AS depositType, d.out_trade_no AS outTradeNo,
                   d.transaction_id AS transactionId, d.pay_time AS payTime, d.create_time AS createTime
            """ + where + " ORDER BY d.id DESC LIMIT ? OFFSET ?", args.toArray());
        return Map.of("items", items, "total", total, "page", safePage, "pageSize", safePageSize);
    }

    void deductDeposit(Long userId, BigDecimal amount, String reason, Long operatorId) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(10020, "扣除金额必须大于0");
        }
        tx.executeWithoutResult(status -> {
            Map<String, Object> account = jdbc.queryForMap(
                    "SELECT deposit_amount FROM seller_account WHERE seller_id = ?", userId);
            BigDecimal current = (BigDecimal) account.get("deposit_amount");
            if (current == null || current.compareTo(amount) < 0) {
                throw new BusinessException(10021, "保证金余额不足");
            }
            jdbc.update("UPDATE seller_account SET deposit_amount = deposit_amount - ?, update_time = CURRENT_TIMESTAMP WHERE seller_id = ?",
                    amount, userId);
            jdbc.update("""
                INSERT INTO deposit_deduction (user_id, amount, reason, operator_id)
                VALUES (?, ?, ?, ?)
                """, userId, amount, reason, operatorId);
            jdbc.update("""
                INSERT INTO deposit_record (user_id, amount, status, deposit_type, remark)
                VALUES (?, ?, 3, 'DEDUCTION', ?)
                """, userId, amount, reason);
        });
    }

    void refundDeposit(Long userId, String reason, Long operatorId) {
        tx.executeWithoutResult(status -> {
            Map<String, Object> account = jdbc.queryForMap(
                    "SELECT deposit_amount FROM seller_account WHERE seller_id = ?", userId);
            BigDecimal current = (BigDecimal) account.get("deposit_amount");
            if (current == null || current.compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessException(10021, "保证金余额为0，无需退还");
            }
            jdbc.update("UPDATE seller_account SET deposit_amount = 0, update_time = CURRENT_TIMESTAMP WHERE seller_id = ?", userId);
            jdbc.update("""
                INSERT INTO deposit_record (user_id, amount, status, deposit_type, refund_time, remark)
                VALUES (?, ?, 4, 'REFUND', CURRENT_TIMESTAMP, ?)
                """, userId, current, reason);
            jdbc.update("UPDATE `user` SET deposit_paid = 0, update_time = CURRENT_TIMESTAMP WHERE id = ?", userId);
        });
    }

    private void checkUserVerified(Long userId) {
        Map<String, Object> user = jdbc.queryForMap(
                "SELECT verify_status, is_seller_verified FROM `user` WHERE id = ?", userId);
        if (((Number) user.get("verify_status")).intValue() != 2
                || ((Number) user.get("is_seller_verified")).intValue() != 1) {
            throw new BusinessException(10010, "请先完成实名认证和讲师认证");
        }
    }

    private BigDecimal getDepositAmount() {
        return DEFAULT_DEPOSIT_AMOUNT;
    }
}
