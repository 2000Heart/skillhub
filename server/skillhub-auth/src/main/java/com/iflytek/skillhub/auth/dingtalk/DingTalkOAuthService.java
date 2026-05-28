package com.iflytek.skillhub.auth.dingtalk;

import com.iflytek.skillhub.auth.oauth.OAuthLoginRedirectSupport;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Builds DingTalk browser OAuth URLs and validates callback state.
 */
@Service
public class DingTalkOAuthService {

    private static final String STATE_SESSION_KEY = "dingtalk.oauth.state";
    private static final String RETURN_TO_SESSION_KEY = OAuthLoginRedirectSupport.SESSION_RETURN_TO_ATTRIBUTE;
    private static final String AUTHORIZE_URL = "https://login.dingtalk.com/oauth2/auth";
    /**
     * Browser OAuth scopes. {@code Contact.User.Read} is required for {@code GET /v1.0/contact/users/me}
     * and must also be enabled under the app's delegated personal permissions in DingTalk Open Platform.
     */
    private static final String OAUTH_SCOPE = "openid corpid Contact.User.Read";

    private final DingTalkRuntimeConfig runtimeConfig;
    private final SecureRandom secureRandom = new SecureRandom();

    public DingTalkOAuthService(DingTalkRuntimeConfig runtimeConfig) {
        this.runtimeConfig = runtimeConfig;
    }

    public String buildAuthorizeUrl(HttpServletRequest request, String returnTo) {
        if (!runtimeConfig.enabled()) {
            throw new DingTalkAuthException("DingTalk authentication is disabled");
        }
        String state = generateState();
        HttpSession session = request.getSession(true);
        session.setAttribute(STATE_SESSION_KEY, state);
        String sanitizedReturnTo = OAuthLoginRedirectSupport.sanitizeReturnTo(returnTo);
        if (sanitizedReturnTo != null) {
            session.setAttribute(RETURN_TO_SESSION_KEY, sanitizedReturnTo);
        } else {
            session.removeAttribute(RETURN_TO_SESSION_KEY);
        }

        String redirectUri = buildRedirectUri(request);
        return UriComponentsBuilder.fromUriString(AUTHORIZE_URL)
                .queryParam("redirect_uri", redirectUri)
                .queryParam("response_type", "code")
                .queryParam("client_id", runtimeConfig.clientId())
                .queryParam("scope", OAUTH_SCOPE)
                .queryParam("state", state)
                .queryParam("prompt", "consent")
                .build()
                .encode()
                .toUriString();
    }

    public void validateState(HttpServletRequest request, String state) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            throw new DingTalkAuthException("Missing OAuth session");
        }
        Object expected = session.getAttribute(STATE_SESSION_KEY);
        session.removeAttribute(STATE_SESSION_KEY);
        if (!(expected instanceof String expectedState) || !expectedState.equals(state)) {
            throw new DingTalkAuthException("Invalid DingTalk OAuth state");
        }
    }

    public String consumeReturnTo(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return null;
        }
        Object value = session.getAttribute(RETURN_TO_SESSION_KEY);
        session.removeAttribute(RETURN_TO_SESSION_KEY);
        return value instanceof String str ? OAuthLoginRedirectSupport.sanitizeReturnTo(str) : null;
    }

    public String buildFrontendRedirect(String returnTo) {
        if (StringUtils.hasText(returnTo)) {
            return returnTo.startsWith("/") ? returnTo : "/" + returnTo;
        }
        return "/dashboard";
    }

    private String buildRedirectUri(HttpServletRequest request) {
        String configuredBase = runtimeConfig.publicBaseUrl();
        if (StringUtils.hasText(configuredBase)) {
            return configuredBase.replaceAll("/+$", "") + "/api/v1/auth/dingtalk/oauth/callback";
        }
        return request.getScheme() + "://" + request.getServerName()
                + (request.getServerPort() == 80 || request.getServerPort() == 443
                ? "" : ":" + request.getServerPort())
                + "/api/v1/auth/dingtalk/oauth/callback";
    }

    private String generateState() {
        byte[] bytes = new byte[24];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
