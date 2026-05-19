package com.jijiang.modules;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
class DeletionScheduler {
    private static final Logger log = LoggerFactory.getLogger(DeletionScheduler.class);

    private final AccountDeletionService accountDeletionService;

    DeletionScheduler(AccountDeletionService accountDeletionService) {
        this.accountDeletionService = accountDeletionService;
    }

    @Scheduled(cron = "${jijiang.deletion.cron:0 0 2 * * *}")
    public void processDeletions() {
        log.info("开始处理过期注销申请...");
        accountDeletionService.processExpiredDeletions();
        log.info("注销处理完成");
    }
}
