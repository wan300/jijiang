package com.jijiang.payment.infra;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class XunhuPaySignerTest {
    @Test
    void canonicalStringSortsAndSkipsBlankAndHash() {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("b", "2");
        params.put("hash", "ignored");
        params.put("empty", "");
        params.put("a", "1");
        params.put("nullValue", null);

        assertThat(XunhuPaySigner.canonicalString(params)).isEqualTo("a=1&b=2");
    }

    @Test
    void signUsesCanonicalStringPlusSecret() {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("b", "2");
        params.put("a", "1");
        params.put("hash", "ignored");

        assertThat(XunhuPaySigner.sign(params, "secret")).isEqualTo("8d9f51949e440aa629fd1a035708473a");
    }

    @Test
    void verifyRejectsMismatchedHash() {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("a", "1");
        params.put("hash", "bad");

        assertThat(XunhuPaySigner.verify(params, "secret")).isFalse();
    }
}
