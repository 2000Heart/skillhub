package com.iflytek.skillhub.auth.dingtalk;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

@Component
public class DingTalkOrgClient {

    private final RestClient oapiClient = RestClient.builder()
            .baseUrl("https://oapi.dingtalk.com")
            .build();

    public DingTalkOrgProfile getOrgProfile(String accessToken, String userId) {
        if (!StringUtils.hasText(userId)) {
            return new DingTalkOrgProfile(null, false);
        }
        UserDetailResponse response = oapiClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/topapi/v2/user/get")
                        .queryParam("access_token", accessToken)
                        .build())
                .contentType(MediaType.APPLICATION_JSON)
                .body(new UserIdRequest(userId))
                .retrieve()
                .body(UserDetailResponse.class);
        if (response == null || response.result() == null) {
            return new DingTalkOrgProfile(null, false);
        }
        UserDetailResult result = response.result();
        String department = resolveDepartmentName(accessToken, result.deptIdList());
        boolean managerOrAbove = Boolean.TRUE.equals(result.admin()) || Boolean.TRUE.equals(result.leader());
        return new DingTalkOrgProfile(department, managerOrAbove);
    }

    private String resolveDepartmentName(String accessToken, Long[] deptIdList) {
        if (deptIdList == null || deptIdList.length == 0 || deptIdList[0] == null) {
            return null;
        }
        DepartmentDetailResponse response = oapiClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/topapi/v2/department/get")
                        .queryParam("access_token", accessToken)
                        .build())
                .contentType(MediaType.APPLICATION_JSON)
                .body(new DeptIdRequest(deptIdList[0]))
                .retrieve()
                .body(DepartmentDetailResponse.class);
        if (response == null || response.result() == null || !StringUtils.hasText(response.result().name())) {
            return null;
        }
        return response.result().name().trim();
    }

    public record DingTalkOrgProfile(String departmentName, boolean managerOrAbove) {
    }

    private record UserIdRequest(@JsonProperty("userid") String userId) {
    }

    private record DeptIdRequest(@JsonProperty("dept_id") Long deptId) {
    }

    private record UserDetailResponse(
            Integer errcode,
            String errmsg,
            UserDetailResult result
    ) {
    }

    private record UserDetailResult(
            Boolean admin,
            Boolean leader,
            @JsonProperty("dept_id_list") Long[] deptIdList
    ) {
    }

    private record DepartmentDetailResponse(
            Integer errcode,
            String errmsg,
            DepartmentDetailResult result
    ) {
    }

    private record DepartmentDetailResult(
            String name
    ) {
    }
}
