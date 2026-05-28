package com.iflytek.skillhub.config;

import com.iflytek.skillhub.auth.dingtalk.DingTalkRuntimeConfig;
import java.util.Arrays;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

/**
 * Bridges application DingTalk properties into the auth module runtime contract.
 */
@Configuration
public class DingTalkConfiguration {

    @Bean
    DingTalkRuntimeConfig dingTalkRuntimeConfig(DingTalkProperties properties,
                                                SkillHubPublicProperties publicProperties) {
        return new DingTalkRuntimeConfig(
                properties.isEnabled(),
                properties.getClientId(),
                properties.getClientSecret(),
                properties.getDefaultCorpId(),
                publicProperties.getBaseUrl(),
                parseUnionIds(properties.getSuperAdminUnionIds()),
                properties.isAutoSuperAdminForManagers()
        );
    }

    private static List<String> parseUnionIds(List<String> configured) {
        if (configured == null || configured.isEmpty()) {
            return List.of();
        }
        return configured.stream()
                .flatMap(value -> Arrays.stream(value.split(",")))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .toList();
    }
}
