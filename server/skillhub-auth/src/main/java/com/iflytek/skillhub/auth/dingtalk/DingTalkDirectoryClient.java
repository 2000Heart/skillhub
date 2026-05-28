package com.iflytek.skillhub.auth.dingtalk;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Collections;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

/**
 * Reads DingTalk organization directory data from OAPI endpoints.
 */
@Component
public class DingTalkDirectoryClient {

    private final RestClient oapiClient = RestClient.builder()
            .baseUrl("https://oapi.dingtalk.com")
            .build();

    public List<Long> listSubDepartmentIds(String accessToken, Long deptId) {
        ListsubidResponse response = oapiClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/topapi/v2/department/listsubid")
                        .queryParam("access_token", accessToken)
                        .build())
                .contentType(MediaType.APPLICATION_JSON)
                .body(new DeptIdRequest(deptId))
                .retrieve()
                .body(ListsubidResponse.class);
        if (response == null || response.result() == null || response.result().deptIdList() == null) {
            return List.of();
        }
        return response.result().deptIdList();
    }

    public DepartmentDto getDepartment(String accessToken, Long deptId) {
        DepartmentGetResponse response = oapiClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/topapi/v2/department/get")
                        .queryParam("access_token", accessToken)
                        .build())
                .contentType(MediaType.APPLICATION_JSON)
                .body(new DepartmentGetRequest(deptId, "zh_CN"))
                .retrieve()
                .body(DepartmentGetResponse.class);
        if (response == null || response.result() == null) {
            throw new DingTalkAuthException("Failed to load DingTalk department " + deptId);
        }
        return response.result();
    }

    public UserListResult listUsersByDepartment(String accessToken, Long deptId, long cursor, int size) {
        UserListResponse response = oapiClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/topapi/v2/user/list")
                        .queryParam("access_token", accessToken)
                        .build())
                .contentType(MediaType.APPLICATION_JSON)
                .body(new UserListRequest(cursor, false, size, "custom", "zh_CN", deptId))
                .retrieve()
                .body(UserListResponse.class);
        if (response == null || response.result() == null) {
            return new UserListResult(false, 0L, List.of());
        }
        List<UserDto> users = response.result().list() == null ? List.of() : response.result().list();
        long nextCursor = response.result().nextCursor() == null ? 0L : response.result().nextCursor();
        return new UserListResult(Boolean.TRUE.equals(response.result().hasMore()), nextCursor, users);
    }

    public UserDetailDto getUser(String accessToken, String userId) {
        UserGetResponse response = oapiClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/topapi/v2/user/get")
                        .queryParam("access_token", accessToken)
                        .build())
                .contentType(MediaType.APPLICATION_JSON)
                .body(new UserGetRequest("zh_CN", userId))
                .retrieve()
                .body(UserGetResponse.class);
        if (response == null || response.result() == null) {
            throw new DingTalkAuthException("Failed to load DingTalk user " + userId);
        }
        return response.result();
    }

    public List<RoleGroupDto> listRoles(String accessToken, int offset, int size) {
        RoleListResponse response = oapiClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/topapi/role/list")
                        .queryParam("access_token", accessToken)
                        .build())
                .contentType(MediaType.APPLICATION_JSON)
                .body(new RoleListRequest(size, offset))
                .retrieve()
                .body(RoleListResponse.class);
        if (response == null || response.result() == null || response.result().list() == null) {
            return List.of();
        }
        return response.result().list();
    }

    public RoleUserListResult listRoleUsers(String accessToken, Long roleId, int offset, int size) {
        RoleSimpleListResponse response = oapiClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/topapi/role/simplelist")
                        .queryParam("access_token", accessToken)
                        .build())
                .contentType(MediaType.APPLICATION_JSON)
                .body(new RoleSimpleListRequest(roleId, size, offset))
                .retrieve()
                .body(RoleSimpleListResponse.class);
        if (response == null || response.result() == null) {
            return new RoleUserListResult(false, 0, List.of());
        }
        return new RoleUserListResult(
                Boolean.TRUE.equals(response.result().hasMore()),
                response.result().nextCursor() == null ? 0 : response.result().nextCursor(),
                response.result().list() == null ? List.of() : response.result().list()
        );
    }

    private record DeptIdRequest(@JsonProperty("dept_id") Long deptId) {}

    private record DepartmentGetRequest(@JsonProperty("dept_id") Long deptId, String language) {}

    private record ListsubidResponse(Integer errcode, String errmsg, ListsubidResult result) {}

    private record ListsubidResult(@JsonProperty("dept_id_list") List<Long> deptIdList) {}

    private record DepartmentGetResponse(Integer errcode, String errmsg, DepartmentDto result) {}

    public record DepartmentDto(
            @JsonProperty("dept_id") Long deptId,
            @JsonProperty("parent_id") Long parentId,
            String name,
            Long order
    ) {}

    private record UserListRequest(
            long cursor,
            @JsonProperty("contain_access_limit") boolean containAccessLimit,
            int size,
            @JsonProperty("order_field") String orderField,
            String language,
            @JsonProperty("dept_id") Long deptId
    ) {}

    private record UserListResponse(Integer errcode, String errmsg, UserListBody result) {}

    private record UserListBody(
            @JsonProperty("next_cursor") Long nextCursor,
            @JsonProperty("has_more") Boolean hasMore,
            List<UserDto> list
    ) {}

    public record UserListResult(boolean hasMore, long nextCursor, List<UserDto> users) {}

    public record UserDto(
            String userid,
            String unionid,
            String name,
            String title,
            String mobile,
            String email,
            Boolean active,
            @JsonProperty("dept_id_list") List<Long> deptIdList
    ) {}

    private record UserGetRequest(String language, String userid) {}

    private record UserGetResponse(Integer errcode, String errmsg, UserDetailDto result) {}

    public record UserDetailDto(
            String userid,
            String unionid,
            String name,
            String title,
            String mobile,
            String email,
            Boolean active,
            @JsonProperty("manager_userid") String managerUserId,
            @JsonProperty("dept_id_list") List<Long> deptIdList,
            @JsonProperty("role_list") List<UserRoleInfo> roleList
    ) {}

    public record UserRoleInfo(Long id, String name, @JsonProperty("group_name") String groupName) {}

    private record RoleListRequest(int size, int offset) {}

    private record RoleListResponse(Integer errcode, String errmsg, RoleListResult result) {}

    private record RoleListResult(Boolean hasMore, List<RoleGroupDto> list) {}

    public record RoleGroupDto(Long groupId, String name, List<RoleDto> roles) {}

    public record RoleDto(Long id, String name) {}

    private record RoleSimpleListRequest(@JsonProperty("role_id") Long roleId, int size, int offset) {}

    private record RoleSimpleListResponse(Integer errcode, String errmsg, RoleSimpleListResult result) {}

    private record RoleSimpleListResult(
            Boolean hasMore,
            Integer nextCursor,
            List<RoleUserDto> list
    ) {}

    public record RoleUserDto(String userid, String name, List<RoleScopeDto> manageScopes) {}

    public record RoleScopeDto(@JsonProperty("dept_id") Long deptId, String name) {}

    public record RoleUserListResult(boolean hasMore, int nextCursor, List<RoleUserDto> users) {}
}
