package com.iflytek.skillhub.task;

import com.iflytek.skillhub.service.DingTalkOrgSyncService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Nightly full-sync job for DingTalk organization directory.
 */
@Component
public class DingTalkOrgFullSyncTask {

    private static final Logger logger = LoggerFactory.getLogger(DingTalkOrgFullSyncTask.class);

    private final DingTalkOrgSyncService dingTalkOrgSyncService;

    public DingTalkOrgFullSyncTask(DingTalkOrgSyncService dingTalkOrgSyncService) {
        this.dingTalkOrgSyncService = dingTalkOrgSyncService;
    }

    @Scheduled(cron = "${skillhub.dingtalk.org-sync.full-cron}")
    public void fullSync() {
        try {
            dingTalkOrgSyncService.runFullSync();
        } catch (Exception ex) {
            logger.error("DingTalk org full sync failed", ex);
        }
    }
}
