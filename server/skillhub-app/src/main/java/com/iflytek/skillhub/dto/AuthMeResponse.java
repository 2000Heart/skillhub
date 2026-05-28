package com.iflytek.skillhub.dto;

import com.iflytek.skillhub.auth.rbac.PlatformPrincipal;

import java.util.Set;

public record AuthMeResponse(
        String userId,
        String displayName,
        String email,
        String avatarUrl,
        String oauthProvider,
        Set<String> platformRoles,
        String department
) {
    public static AuthMeResponse from(PlatformPrincipal principal) {
        return new AuthMeResponse(
                principal.userId(),
                principal.displayName(),
                principal.email() != null ? principal.email() : "",
                principal.avatarUrl() != null ? principal.avatarUrl() : "",
                principal.oauthProvider(),
                principal.platformRoles(),
                principal.department()
        );
    }
}
