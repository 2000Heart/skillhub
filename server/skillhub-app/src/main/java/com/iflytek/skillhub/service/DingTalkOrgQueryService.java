package com.iflytek.skillhub.service;

import com.iflytek.skillhub.domain.orgsync.OrgDepartmentRepository;
import com.iflytek.skillhub.domain.orgsync.OrgRoleRepository;
import com.iflytek.skillhub.domain.orgsync.OrgSyncStateRepository;
import com.iflytek.skillhub.domain.orgsync.OrgUserDepartmentRepository;
import com.iflytek.skillhub.domain.orgsync.OrgUserProfileRepository;
import com.iflytek.skillhub.domain.orgsync.OrgUserRoleRepository;
import com.iflytek.skillhub.domain.user.UserAccountRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class DingTalkOrgQueryService {

    private final OrgDepartmentRepository orgDepartmentRepository;
    private final OrgUserProfileRepository orgUserProfileRepository;
    private final OrgUserDepartmentRepository orgUserDepartmentRepository;
    private final OrgRoleRepository orgRoleRepository;
    private final OrgUserRoleRepository orgUserRoleRepository;
    private final OrgSyncStateRepository orgSyncStateRepository;
    private final UserAccountRepository userAccountRepository;

    public DingTalkOrgQueryService(OrgDepartmentRepository orgDepartmentRepository,
                                   OrgUserProfileRepository orgUserProfileRepository,
                                   OrgUserDepartmentRepository orgUserDepartmentRepository,
                                   OrgRoleRepository orgRoleRepository,
                                   OrgUserRoleRepository orgUserRoleRepository,
                                   OrgSyncStateRepository orgSyncStateRepository,
                                   UserAccountRepository userAccountRepository) {
        this.orgDepartmentRepository = orgDepartmentRepository;
        this.orgUserProfileRepository = orgUserProfileRepository;
        this.orgUserDepartmentRepository = orgUserDepartmentRepository;
        this.orgRoleRepository = orgRoleRepository;
        this.orgUserRoleRepository = orgUserRoleRepository;
        this.orgSyncStateRepository = orgSyncStateRepository;
        this.userAccountRepository = userAccountRepository;
    }

    public List<OrgDepartmentSummary> listDepartments() {
        return orgDepartmentRepository.findAll().stream()
                .map(it -> new OrgDepartmentSummary(
                        it.getDeptId(),
                        it.getParentDeptId(),
                        it.getName(),
                        it.getDeptOrder(),
                        it.getLastSyncAt(),
                        it.getDeletedAt() != null
                ))
                .toList();
    }

    public List<OrgUserSummary> listUsers() {
        return orgUserProfileRepository.findAll().stream().map(user -> {
            var account = userAccountRepository.findById(user.getUserId()).orElse(null);
            return new OrgUserSummary(
                    user.getUserId(),
                    user.getUnionId(),
                    user.getName(),
                    user.getTitle(),
                    user.getMobile(),
                    user.getEmail(),
                    user.isActive(),
                    user.getPrimaryDeptId(),
                    user.getLastSyncAt(),
                    account != null ? account.getPermissionSource() : null,
                    account != null ? account.getPermissionInitializedAt() : null,
                    account != null ? account.getStatus() : null,
                    orgUserRoleRepository.findByUserId(user.getUserId()).stream()
                            .map(role -> role.getRoleName())
                            .distinct()
                            .sorted()
                            .toList()
            );
        }).toList();
    }

    public OrgSyncStatus getSyncStatus() {
        var state = orgSyncStateRepository.findById("FULL_SYNC").orElse(null);
        return new OrgSyncStatus(
                state != null ? state.getPhase() : "NEVER_RUN",
                state != null && state.isSuccess(),
                state != null ? state.getLastRunAt() : null,
                state != null ? state.getLastError() : null
        );
    }

    public record OrgDepartmentSummary(
            Long deptId,
            Long parentDeptId,
            String name,
            Long order,
            java.time.Instant lastSyncAt,
            boolean deleted
    ) {}

    public record OrgUserSummary(
            String userId,
            String unionId,
            String name,
            String title,
            String mobile,
            String email,
            boolean active,
            Long primaryDeptId,
            java.time.Instant lastSyncAt,
            com.iflytek.skillhub.domain.user.PermissionSource permissionSource,
            java.time.Instant permissionInitializedAt,
            com.iflytek.skillhub.domain.user.UserStatus accountStatus,
            List<String> dingtalkRoles
    ) {}

    public record OrgSyncStatus(String phase, boolean success, java.time.Instant lastRunAt, String lastError) {}
}
