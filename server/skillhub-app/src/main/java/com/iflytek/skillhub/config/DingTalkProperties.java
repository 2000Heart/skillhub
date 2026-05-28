package com.iflytek.skillhub.config;

import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * DingTalk enterprise web-app authentication settings.
 */
@Component
@ConfigurationProperties(prefix = "skillhub.dingtalk")
public class DingTalkProperties {

    private boolean enabled = false;
    private String clientId = "";
    private String clientSecret = "";
    private String defaultCorpId = "";
    private List<String> superAdminUnionIds = new ArrayList<>();
    private boolean autoSuperAdminForManagers = false;
    private OrgSync orgSync = new OrgSync();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public String getDefaultCorpId() {
        return defaultCorpId;
    }

    public void setDefaultCorpId(String defaultCorpId) {
        this.defaultCorpId = defaultCorpId;
    }

    public List<String> getSuperAdminUnionIds() {
        return superAdminUnionIds;
    }

    public void setSuperAdminUnionIds(List<String> superAdminUnionIds) {
        this.superAdminUnionIds = superAdminUnionIds != null ? superAdminUnionIds : new ArrayList<>();
    }

    public boolean isAutoSuperAdminForManagers() {
        return autoSuperAdminForManagers;
    }

    public void setAutoSuperAdminForManagers(boolean autoSuperAdminForManagers) {
        this.autoSuperAdminForManagers = autoSuperAdminForManagers;
    }

    public OrgSync getOrgSync() {
        return orgSync;
    }

    public void setOrgSync(OrgSync orgSync) {
        this.orgSync = orgSync != null ? orgSync : new OrgSync();
    }

    /**
     * DingTalk organization directory synchronization settings.
     */
    public static class OrgSync {
        /** Run a full org sync on startup when no successful baseline exists yet. */
        private boolean startupEnabled = true;
        private String fullCron = "0 30 2 * * ?";

        public boolean isStartupEnabled() {
            return startupEnabled;
        }

        public void setStartupEnabled(boolean startupEnabled) {
            this.startupEnabled = startupEnabled;
        }

        public String getFullCron() {
            return fullCron;
        }

        public void setFullCron(String fullCron) {
            this.fullCron = fullCron;
        }
    }
}
