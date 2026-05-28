package com.iflytek.skillhub.auth.dingtalk;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Exchanges DingTalk application credentials for corp-scoped access tokens.
 */
@Component
public class DingTalkTokenClient {

    private final RestClient restClient = RestClient.builder()
            .baseUrl("https://api.dingtalk.com")
            .build();

    public String getCorpAccessToken(String corpId, String clientId, String clientSecret) {
        TokenResponse response = restClient.post()
                .uri("/v1.0/oauth2/{corpId}/token", corpId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new TokenRequest(clientId, clientSecret, "client_credentials"))
                .retrieve()
                .body(TokenResponse.class);

        if (response == null || response.accessToken() == null || response.accessToken().isBlank()) {
            throw new DingTalkAuthException("Failed to obtain DingTalk corp access token");
        }
        return response.accessToken();
    }

    public String getUserAccessToken(String clientId, String clientSecret, String code) {
        UserTokenResponse response = restClient.post()
                .uri("/v1.0/oauth2/userAccessToken")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new UserTokenRequest(clientId, clientSecret, code, "authorization_code"))
                .retrieve()
                .body(UserTokenResponse.class);

        if (response == null || response.accessToken() == null || response.accessToken().isBlank()) {
            throw new DingTalkAuthException("Failed to exchange DingTalk authorization code");
        }
        return response.accessToken();
    }

    private record TokenRequest(
            @JsonProperty("client_id") String clientId,
            @JsonProperty("client_secret") String clientSecret,
            @JsonProperty("grant_type") String grantType
    ) {
    }

    private record TokenResponse(
            @JsonProperty("access_token") String accessToken,
            @JsonProperty("expires_in") Long expiresIn
    ) {
    }

    private record UserTokenRequest(
            String clientId,
            String clientSecret,
            String code,
            String grantType
    ) {
    }

    private record UserTokenResponse(
            String accessToken,
            String refreshToken,
            Long expireIn
    ) {
    }
}
