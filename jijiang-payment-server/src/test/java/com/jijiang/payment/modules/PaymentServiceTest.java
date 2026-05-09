package com.jijiang.payment.modules;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jijiang.payment.common.BusinessException;
import com.jijiang.payment.infra.AppServerClient;
import com.jijiang.payment.infra.AppServerProperties;
import com.jijiang.payment.infra.InternalSignatureSupport;
import com.jijiang.payment.infra.XunhuPayClient;
import com.jijiang.payment.infra.XunhuPayProperties;
import com.jijiang.payment.infra.XunhuPaySigner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PaymentServiceTest {
    private static final String CLIENT_ID = "jijiang-app";
    private static final String SECRET = "secret";

    private JdbcTemplate jdbc;
    private ObjectMapper objectMapper;
    private PaymentService service;
    private StubXunhuPayClient xunhuPayClient;
    private AppServerClient appServerClient;

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
        XunhuPayProperties xunhuProperties = new XunhuPayProperties();
        xunhuProperties.setAppId("appid");
        xunhuProperties.setAppSecret("xunhu-secret");
        xunhuProperties.setNotifyUrl("https://pay.example.com/api/payment/xunhu-notify");
        AppServerProperties appServerProperties = new AppServerProperties();
        appServerProperties.setCallbackUrl("https://api.example.com/internal/payment/callback");
        appServerProperties.setClientId(CLIENT_ID);
        appServerProperties.setSharedSecret(SECRET);
        xunhuPayClient = new StubXunhuPayClient();
        appServerClient = mock(AppServerClient.class);
        service = new PaymentService(jdbc, tx, objectMapper, xunhuPayClient, xunhuProperties,
                appServerClient, appServerProperties, null);
    }

    @Test
    void createStoresPendingPaymentAndIsIdempotent() {
        String body = createBody("JJ1", "12.34");

        CreatePaymentResponse first = service.create(body, signedHeaders("/internal/payment/orders", body));
        CreatePaymentResponse second = service.create(body, signedHeaders("/internal/payment/orders", body));

        assertThat(first.payUrl()).isEqualTo("https://cashier.example.com/pay");
        assertThat(second.tradeOrderId()).isEqualTo("JJ1");
        assertThat(xunhuPayClient.calls).isEqualTo(1);
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM payment_order WHERE order_no = 'JJ1'", Integer.class);
        assertThat(count).isEqualTo(1);
    }

    @Test
    void createRejectsBadSignature() {
        String body = createBody("JJ1", "12.34");
        HttpHeaders headers = signedHeaders("/internal/payment/orders", body);
        headers.set(InternalSignatureSupport.SIGNATURE, "bad");

        assertThatThrownBy(() -> service.create(body, headers))
                .isInstanceOf(BusinessException.class)
                .hasMessage("内部支付签名错误");
    }

    @Test
    void successfulNotifyMarksPaidAndCallsAppServer() {
        String body = createBody("JJ1", "12.34");
        service.create(body, signedHeaders("/internal/payment/orders", body));

        service.handleXunhuNotify(notifyParams("JJ1", "12.34"));

        Map<String, Object> payment = jdbc.queryForMap("SELECT * FROM payment_order WHERE order_no = 'JJ1'");
        assertThat(((Number) payment.get("status")).intValue()).isEqualTo(2);
        assertThat(((Number) payment.get("app_callback_status")).intValue()).isEqualTo(1);
        verify(appServerClient).notifyPaid(any(PaymentCallbackRequest.class));
    }

    @Test
    void notifyRejectsAmountMismatch() {
        String body = createBody("JJ1", "12.34");
        service.create(body, signedHeaders("/internal/payment/orders", body));

        assertThatThrownBy(() -> service.handleXunhuNotify(notifyParams("JJ1", "12.35")))
                .isInstanceOf(BusinessException.class)
                .hasMessage("虎皮椒回调金额不匹配");
    }

    @Test
    void notifyRecordsFailedAppCallbackForRetry() {
        String body = createBody("JJ1", "12.34");
        service.create(body, signedHeaders("/internal/payment/orders", body));
        doThrow(new BusinessException(30041, "应用服务器回调失败")).when(appServerClient).notifyPaid(any());

        service.handleXunhuNotify(notifyParams("JJ1", "12.34"));

        Map<String, Object> payment = jdbc.queryForMap("SELECT * FROM payment_order WHERE order_no = 'JJ1'");
        assertThat(((Number) payment.get("status")).intValue()).isEqualTo(2);
        assertThat(((Number) payment.get("app_callback_status")).intValue()).isEqualTo(2);
        assertThat(((Number) payment.get("app_callback_attempts")).intValue()).isEqualTo(1);
    }

    private String createBody(String orderNo, String amount) {
        try {
            return objectMapper.writeValueAsString(Map.of(
                    "orderId", 1L,
                    "orderNo", orderNo,
                    "amount", new BigDecimal(amount),
                    "title", "技匠订单-" + orderNo
            ));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private HttpHeaders signedHeaders(String path, String body) {
        HttpHeaders headers = new HttpHeaders();
        InternalSignatureSupport.signedHeaders(CLIENT_ID, SECRET, "POST", path, body).forEach(headers::set);
        return headers;
    }

    private Map<String, String> notifyParams(String tradeOrderId, String totalFee) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("trade_order_id", tradeOrderId);
        params.put("total_fee", totalFee);
        params.put("transaction_id", "TX1");
        params.put("open_order_id", "OPEN1");
        params.put("status", "OD");
        params.put("appid", "appid");
        params.put("time", "1710000000");
        params.put("nonce_str", "abc123");
        params.put("hash", XunhuPaySigner.sign(params, "xunhu-secret"));
        return params;
    }

    private void createSchema() {
        jdbc.execute("""
            CREATE TABLE payment_order (
              id BIGINT AUTO_INCREMENT PRIMARY KEY,
              order_id BIGINT NOT NULL,
              order_no VARCHAR(32) NOT NULL,
              trade_order_id VARCHAR(64) NOT NULL,
              amount DECIMAL(10,2) NOT NULL,
              title VARCHAR(128) NOT NULL,
              status TINYINT NOT NULL DEFAULT 1,
              channel VARCHAR(32) NOT NULL DEFAULT 'XUNHUPAY',
              pay_url VARCHAR(512),
              qr_code_url VARCHAR(512),
              transaction_id VARCHAR(64),
              notify_body TEXT,
              app_callback_body TEXT,
              app_callback_status TINYINT NOT NULL DEFAULT 0,
              app_callback_attempts INT NOT NULL DEFAULT 0,
              last_callback_error VARCHAR(512),
              paid_time DATETIME,
              callback_next_time DATETIME,
              create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
              update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
              is_deleted TINYINT NOT NULL DEFAULT 0,
              UNIQUE KEY uk_payment_order_no (order_no),
              UNIQUE KEY uk_payment_trade_order_id (trade_order_id)
            )
            """);
    }

    private static class StubXunhuPayClient implements XunhuPayClient {
        int calls;

        @Override
        public CreateOrderResponse createOrder(CreateOrderRequest request) {
            calls++;
            return new CreateOrderResponse(request.tradeOrderId(), "https://cashier.example.com/pay",
                    "https://cashier.example.com/qr", "{}");
        }
    }
}
