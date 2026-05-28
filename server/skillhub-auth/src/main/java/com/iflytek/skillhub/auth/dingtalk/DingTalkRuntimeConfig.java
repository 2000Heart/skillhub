package com.iflytek.skillhub.auth.dingtalk;

import java.util.List;

/**
 * Runtime DingTalk auth configuration exposed to the auth module.
 */
public record DingTalkRuntimeConfig(
    boolean enabled,
    String clientId,
    String clientSecret,
    String defaultCorpId,
    String publicBaseUrl,
    List<String> superAdminUnionIds,
    boolean autoSuperAdminForManagers
) {
}
