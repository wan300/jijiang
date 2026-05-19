package com.jijiang.modules;

import com.jijiang.common.BusinessException;
import com.jijiang.infra.ExternalClients;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

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
