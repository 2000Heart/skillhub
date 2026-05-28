package com.iflytek.skillhub.auth.dingtalk;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

/**
 * Resolves DingTalk users from in-app auth codes and browser OAuth access tokens.
 */
@Component
public class DingTalkUserResolver {

    private static final String OAPI_BASE = "https://oapi.dingtalk.com";

    private final DingTalkTokenClient tokenClient;
    private final DingTalkOrgClient orgClient;
    private final RestClient oapiClient = RestClient.builder()
            .baseUrl(OAPI_BASE)
            .build();
    private final RestClient apiClient = RestClient.builder()
            .baseUrl("https://api.dingtalk.com")
            .build();

    public DingTalkUserResolver(DingTalkTokenClient tokenClient, DingTalkOrgClient orgClient) {
        this.tokenClient = tokenClient;
        this.orgClient = orgClient;
    }

    public DingTalkUserProfile resolveFromAuthCode(String corpId,
                                                     String clientId,
                                                     String clientSecret,
                                                     String authCode) {
        String accessToken = tokenClient.getCorpAccessToken(corpId, clientId, clientSecret);
        GetUserInfoResponse response = oapiClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/topapi/v2/user/getuserinfo")
                        .queryParam("access_token", accessToken)
                        .build())
                .contentType(MediaType.APPLICATION_JSON)
                .body(new AuthCodeRequest(authCode))
                .retrieve()
                .body(GetUserInfoResponse.class);

        if (response == null || response.result() == null) {
            throw new DingTalkAuthException("DingTalk getuserinfo returned empty result");
        }
        UserInfoResult result = response.result();
        String unionId = firstNonBlank(result.unionId(), result.userid());
        if (!StringUtils.hasText(unionId)) {
            throw new DingTalkAuthException("DingTalk user is missing unionId and userid");
        }

        String avatarUrl = loadAvatar(accessToken, result.userid());
        DingTalkOrgClient.DingTalkOrgProfile orgProfile = orgClient.getOrgProfile(accessToken, result.userid());
        return new DingTalkUserProfile(
                result.userid(),
                unionId,
                firstNonBlank(result.name(), result.userid()),
                avatarUrl,
                orgProfile.departmentName(),
                orgProfile.managerOrAbove()
        );
    }

    public DingTalkUserProfile resolveFromUserAccessToken(String userAccessToken) {
        ContactUserResponse response;
        try {
            response = apiClient.get()
                    .uri("/v1.0/contact/users/me")
                    .header("x-acs-dingtalk-access-token", userAccessToken)
                    .retrieve()
                    .body(ContactUserResponse.class);
        } catch (HttpClientErrorException.Forbidden ex) {
            throw new DingTalkAuthException(
                    "DingTalk denied contact/users/me (Contact.User.Read). "
                            + "Enable the delegated personal permission in DingTalk Open Platform and re-authorize.");
        } catch (HttpClientErrorException ex) {
            throw new DingTalkAuthException("DingTalk contact/users/me failed: " + ex.getStatusCode());
        }

        if (response == null) {
            throw new DingTalkAuthException("DingTalk contact/users/me returned empty result");
        }
        String unionId = firstNonBlank(response.unionId(), response.openId(), response.userId());
        if (!StringUtils.hasText(unionId)) {
            throw new DingTalkAuthException("DingTalk OAuth user is missing unionId");
        }
        return new DingTalkUserProfile(
                response.userId(),
                unionId,
                firstNonBlank(response.nick(), response.name(), unionId),
                response.avatarUrl(),
                null,
                false
        );
    }

    private String loadAvatar(String accessToken, String userId) {
        if (!StringUtils.hasText(userId)) {
            return null;
        }
        GetUserResponse response = oapiClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/topapi/v2/user/get")
                        .queryParam("access_token", accessToken)
                        .build())
                .contentType(MediaType.APPLICATION_JSON)
                .body(new UserIdRequest(userId))
                .retrieve()
                .body(GetUserResponse.class);
        if (response == null || response.result() == null) {
            return null;
        }
        return response.result().avatar();
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value;
            }
        }
        return null;
    }

    private record AuthCodeRequest(String code) {
    }

    private record UserIdRequest(@JsonProperty("userid") String userId) {
    }

    private record GetUserInfoResponse(
            Integer errcode,
            String errmsg,
            UserInfoResult result
    ) {
    }

    private record UserInfoResult(
            String userid,
            @JsonProperty("unionid") String unionId,
            String name
    ) {
    }

    private record GetUserResponse(
            Integer errcode,
            String errmsg,
            UserDetailResult result
    ) {
    }

    private record UserDetailResult(String avatar) {
    }

    private record ContactUserResponse(
            String unionId,
            String openId,
            String userId,
            String nick,
            String name,
            String avatarUrl
    ) {
    }
}
