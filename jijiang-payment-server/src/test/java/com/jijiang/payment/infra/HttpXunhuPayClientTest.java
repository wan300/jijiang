package com.jijiang.payment.infra;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HttpXunhuPayClientTest {
    @Test
    void normalizesRelativeResponseUrlAgainstGateway() {
        String result = HttpXunhuPayClient.normalizeResponseUrl(
                "qrcode_v3?id=1",
                "https://api.xunhupay.com/payment/do.html"
        );

        assertThat(result).isEqualTo("https://api.xunhupay.com/payment/qrcode_v3?id=1");
    }

    @Test
    void normalizesProtocolRelativeResponseUrl() {
        String result = HttpXunhuPayClient.normalizeResponseUrl("//api.xunhupay.com/payment/qrcode_v3?id=1", null);

        assertThat(result).isEqualTo("https://api.xunhupay.com/payment/qrcode_v3?id=1");
    }

    @Test
    void keepsAbsoluteResponseUrl() {
        String result = HttpXunhuPayClient.normalizeResponseUrl(
                "https://cashier.example.com/pay",
                "https://api.xunhupay.com/payment/do.html"
        );

        assertThat(result).isEqualTo("https://cashier.example.com/pay");
    }
}
