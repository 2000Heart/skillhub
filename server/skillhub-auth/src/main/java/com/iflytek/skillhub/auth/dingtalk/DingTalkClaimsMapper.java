package com.iflytek.skillhub.auth.dingtalk;

import com.iflytek.skillhub.auth.oauth.OAuthClaims;
import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Maps DingTalk profiles into the shared OAuth claims contract.
 */
@Component
public class DingTalkClaimsMapper {

    public static final String PROVIDER_CODE = "dingtalk";

    public OAuthClaims toClaims(DingTalkUserProfile profile, String corpId) {
        Map<String, Object> extra = new HashMap<>();
        extra.put("avatar_url", profile.avatarUrl());
        extra.put("dingtalk_userid", profile.userId());
        if (profile.department() != null) {
            extra.put("department", profile.department());
        }
        if (corpId != null) {
            extra.put("corp_id", corpId);
        }

        return new OAuthClaims(
                PROVIDER_CODE,
                profile.unionId(),
                null,
                false,
                resolveProviderLogin(profile),
                extra
        );
    }

    private static String resolveProviderLogin(DingTalkUserProfile profile) {
        if (StringUtils.hasText(profile.name())) {
            return profile.name().trim();
        }
        if (StringUtils.hasText(profile.userId())) {
            return profile.userId().trim();
        }
        return profile.unionId();
    }
}
