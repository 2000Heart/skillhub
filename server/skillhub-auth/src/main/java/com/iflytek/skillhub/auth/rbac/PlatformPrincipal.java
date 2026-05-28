package com.iflytek.skillhub.auth.rbac;

import java.io.Serializable;
import java.util.Set;

/**
 * Serializable authenticated principal shared across session, OAuth, and API-token flows.
 */
public record PlatformPrincipal(
    String userId,
    String displayName,
    String email,
    String avatarUrl,
    String oauthProvider,
    Set<String> platformRoles,
    String department
) implements Serializable {

    public PlatformPrincipal(
            String userId,
            String displayName,
            String email,
            String avatarUrl,
            String oauthProvider,
            Set<String> platformRoles) {
        this(userId, displayName, email, avatarUrl, oauthProvider, platformRoles, null);
    }
}
