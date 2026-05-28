package com.iflytek.skillhub.bootstrap;

import com.iflytek.skillhub.config.DingTalkProperties;
import com.iflytek.skillhub.service.DingTalkOrgSyncService;
import java.util.concurrent.Executor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Triggers the first DingTalk organization full sync after the application is ready,
 * when no successful baseline sync has been recorded yet.
 */
@Component
@Order(100)
public class DingTalkOrgStartupSyncInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DingTalkOrgStartupSyncInitializer.class);

    private final DingTalkProperties dingTalkProperties;
    private final DingTalkOrgSyncService dingTalkOrgSyncService;
    private final Executor backgroundExecutor;

    public DingTalkOrgStartupSyncInitializer(
            DingTalkProperties dingTalkProperties,
            DingTalkOrgSyncService dingTalkOrgSyncService,
            @Qualifier("skillhubEventExecutor") Executor backgroundExecutor) {
        this.dingTalkProperties = dingTalkProperties;
        this.dingTalkOrgSyncService = dingTalkOrgSyncService;
        this.backgroundExecutor = backgroundExecutor;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!dingTalkProperties.getOrgSync().isStartupEnabled()) {
            log.debug("Skip DingTalk org startup sync because startup sync is disabled");
            return;
        }
        if (!dingTalkOrgSyncService.needsInitialFullSync()) {
            log.debug("Skip DingTalk org startup sync because a successful baseline already exists");
            return;
        }
        log.info("Scheduling DingTalk organization full sync on first startup");
        backgroundExecutor.execute(() -> {
            try {
                dingTalkOrgSyncService.runFullSync();
                log.info("DingTalk organization startup full sync completed");
            } catch (Exception ex) {
                log.error("DingTalk organization startup full sync failed", ex);
            }
        });
    }
}
