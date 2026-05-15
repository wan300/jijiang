package com.jijiang.modules;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jijiang.common.BusinessException;
import com.jijiang.common.UserContext;
import com.jijiang.infra.InternalSignatureSupport;
import com.jijiang.infra.PaymentServerClient;
import com.jijiang.infra.PaymentServerProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaymentAppServiceTest {
    private static final String CLIENT_ID = "jijiang-app";
    private static final String SECRET = "secret";

    private JdbcTemplate jdbc;
    private ObjectMapper objectMapper;
    private PaymentAppService service;
    private StubPaymentServerClient paymentServerClient;

    @BeforeEach
    void setUp() {
        DataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH;DB_CLOSE_DELAY=-1",
                "sa",
                ""
        );
        jdbc = new JdbcTemplate(dataSource);
        objectMapper = new ObjectMapper();
        createSchema();
        PlatformTransactionManager txManager = new org.springframework.jdbc.datasource.DataSourceTransactionManager(dataSource);
        TransactionTemplate tx = new TransactionTemplate(txManager);
        PaymentServerProperties properties = new PaymentServerProperties();
        properties.setBaseUrl("https://pay.example.com");
        properties.setClientId(CLIENT_ID);
        properties.setSharedSecret(SECRET);
        paymentServerClient = new StubPaymentServerClient();
        service = new PaymentAppService(jdbc, tx, objectMapper, paymentServerClient, properties, null,
                new OrderAppService(jdbc, tx, null));
    }

    @Test
    void createPaymentRejectsNonBuyer() {
        insertOrder(1L, "JJ1", 10L, 10);

        assertThatThrownBy(() -> service.createPayment(new UserContext(11L, 1, 1L), 1L))
                .isInstanceOf(BusinessException.class)
                .hasMessage("无权支付该订单");
    }

    @Test
    void createPaymentRejectsNonWaitPayOrder() {
        insertOrder(1L, "JJ1", 10L, 20);

        assertThatThrownBy(() -> service.createPayment(new UserContext(10L, 1, 1L), 1L))
                .isInstanceOf(BusinessException.class)
                .hasMessage("订单状态不允许支付");
    }

    @Test
    void createPaymentStoresPendingMirror() {
        insertOrder(1L, "JJ1", 10L, 10);

        Map<String, Object> result = service.createPayment(new UserContext(10L, 1, 1L), 1L);

        assertThat(result).containsEntry("channel", "XUNHUPAY")
                .containsEntry("orderId", 1L)
                .containsEntry("tradeOrderId", "JJ1")
                .containsEntry("payUrl", "https://cashier.example.com/pay");
        Map<String, Object> payment = jdbc.queryForMap("SELECT * FROM payment_record WHERE out_trade_no = 'JJ1'");
        assertThat(((Number) payment.get("status")).intValue()).isEqualTo(1);
        assertThat(payment.get("pay_channel")).isEqualTo("XUNHUPAY");
    }

    @Test
    void signedCallbackTransitionsOrderAndIsIdempotent() {
        insertOrder(1L, "JJ1", 10L, 10);
        service.createPayment(new UserContext(10L, 1, 1L), 1L);
        String body = callbackBody("JJ1", "12.34");

        service.handlePaymentServerCallback(body, signedHeaders(body));
        service.handlePaymentServerCallback(body, signedHeaders(body));

        Map<String, Object> payment = jdbc.queryForMap("SELECT * FROM payment_record WHERE out_trade_no = 'JJ1'");
        assertThat(((Number) payment.get("status")).intValue()).isEqualTo(2);
        assertThat(payment.get("transaction_id")).isEqualTo("TX1");
        Map<String, Object> order = jdbc.queryForMap("SELECT * FROM order_main WHERE id = 1");
        assertThat(((Number) order.get("status")).intValue()).isEqualTo(20);
        Integer logCount = jdbc.queryForObject("SELECT COUNT(*) FROM order_log WHERE order_id = 1 AND to_status = 20", Integer.class);
        assertThat(logCount).isEqualTo(1);
    }

    @Test
    void callbackRejectsAmountMismatch() {
        insertOrder(1L, "JJ1", 10L, 10);
        service.createPayment(new UserContext(10L, 1, 1L), 1L);
        String body = callbackBody("JJ1", "12.35");

        assertThatThrownBy(() -> service.handlePaymentServerCallback(body, signedHeaders(body)))
                .isInstanceOf(BusinessException.class)
                .hasMessage("支付服务回调金额不匹配");
        Integer orderStatus = jdbc.queryForObject("SELECT status FROM order_main WHERE id = 1", Integer.class);
        assertThat(orderStatus).isEqualTo(10);
    }

    @Test
    void callbackRejectsBadSignature() {
        insertOrder(1L, "JJ1", 10L, 10);
        service.createPayment(new UserContext(10L, 1, 1L), 1L);
        String body = callbackBody("JJ1", "12.34");
        HttpHeaders headers = signedHeaders(body);
        headers.set(InternalSignatureSupport.SIGNATURE, "bad");

        assertThatThrownBy(() -> service.handlePaymentServerCallback(body, headers))
                .isInstanceOf(BusinessException.class)
                .hasMessage("内部支付签名错误");
    }

    @Test
    void callbackRejectsReplayedNonce() {
        insertOrder(1L, "JJ1", 10L, 10);
        service.createPayment(new UserContext(10L, 1, 1L), 1L);
        String body = callbackBody("JJ1", "12.34");
        HttpHeaders headers = signedHeaders(body);

        service.handlePaymentServerCallback(body, headers);

        assertThatThrownBy(() -> service.handlePaymentServerCallback(body, headers))
                .isInstanceOf(BusinessException.class)
                .hasMessage("内部支付请求重复");
    }

    private String callbackBody(String tradeOrderId, String amount) {
        try {
            return objectMapper.writeValueAsString(Map.of(
                    "orderId", 1L,
                    "orderNo", tradeOrderId,
                    "tradeOrderId", tradeOrderId,
                    "transactionId", "TX1",
                    "amount", new BigDecimal(amount),
                    "status", "SUCCESS",
                    "channel", "XUNHUPAY",
                    "payUrl", "https://cashier.example.com/pay"
            ));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private HttpHeaders signedHeaders(String body) {
        HttpHeaders headers = new HttpHeaders();
        InternalSignatureSupport.signedHeaders(CLIENT_ID, SECRET, "POST", "/internal/payment/callback", body)
                .forEach(headers::set);
        return headers;
    }

    private void createSchema() {
        jdbc.execute("""
            CREATE TABLE order_main (
              id BIGINT PRIMARY KEY,
              order_no VARCHAR(32) NOT NULL,
              buyer_id BIGINT NOT NULL,
              seller_id BIGINT NOT NULL,
              service_id BIGINT NOT NULL,
              campus_id BIGINT NOT NULL,
              amount DECIMAL(10,2) NOT NULL,
              status TINYINT NOT NULL,
              expire_time DATETIME,
              pay_time DATETIME,
              is_deleted TINYINT NOT NULL DEFAULT 0
            )
            """);
        jdbc.execute("""
            CREATE TABLE payment_record (
              id BIGINT AUTO_INCREMENT PRIMARY KEY,
              order_id BIGINT NOT NULL,
              out_trade_no VARCHAR(64) NOT NULL,
              transaction_id VARCHAR(64),
              amount DECIMAL(10,2) NOT NULL,
              status TINYINT NOT NULL DEFAULT 1,
              pay_channel VARCHAR(32) NOT NULL DEFAULT 'XUNHUPAY',
              pay_url VARCHAR(512),
              notify_body TEXT,
              pay_time DATETIME,
              create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
              update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
              is_deleted TINYINT NOT NULL DEFAULT 0,
              UNIQUE KEY uk_payment_out_trade_no (out_trade_no)
            )
            """);
        jdbc.execute("""
            CREATE TABLE order_log (
              id BIGINT AUTO_INCREMENT PRIMARY KEY,
              order_id BIGINT NOT NULL,
              from_status TINYINT,
              to_status TINYINT NOT NULL,
              operator_id BIGINT,
              remark VARCHAR(255),
              create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
              update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
              is_deleted TINYINT NOT NULL DEFAULT 0
            )
            """);
    }

    private void insertOrder(Long orderId, String orderNo, Long buyerId, int status) {
        jdbc.update("""
            INSERT INTO order_main (id, order_no, buyer_id, seller_id, service_id, campus_id, amount, status)
            VALUES (?, ?, ?, 99, 100, 1, ?, ?)
            """, orderId, orderNo, buyerId, new BigDecimal("12.34"), status);
    }

    private static class StubPaymentServerClient implements PaymentServerClient {
        @Override
        public CreatePaymentResponse createPayment(CreatePaymentRequest request) {
            return new CreatePaymentResponse("XUNHUPAY", request.orderId(), request.orderNo(), request.orderNo(),
                    "https://cashier.example.com/pay", "https://cashier.example.com/qr", 300);
        }

        @Override
        public PaymentStatusResponse queryPaymentStatus(String tradeOrderId) {
            return new PaymentStatusResponse(null, null, tradeOrderId, null, "PENDING",
                    null, null, null, null, null);
        }
    }
}
