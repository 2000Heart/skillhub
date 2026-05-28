package com.iflytek.skillhub.service;

import com.iflytek.skillhub.auth.dingtalk.DingTalkDirectoryClient;
import com.iflytek.skillhub.auth.dingtalk.DingTalkRuntimeConfig;
import com.iflytek.skillhub.auth.dingtalk.DingTalkTokenClient;
import com.iflytek.skillhub.auth.entity.Role;
import com.iflytek.skillhub.auth.entity.UserRoleBinding;
import com.iflytek.skillhub.auth.repository.RoleRepository;
import com.iflytek.skillhub.auth.repository.UserRoleBindingRepository;
import com.iflytek.skillhub.domain.orgsync.OrgDepartment;
import com.iflytek.skillhub.domain.orgsync.OrgDepartmentRepository;
import com.iflytek.skillhub.domain.orgsync.OrgEventCheckpoint;
import com.iflytek.skillhub.domain.orgsync.OrgEventCheckpointRepository;
import com.iflytek.skillhub.domain.orgsync.OrgRole;
import com.iflytek.skillhub.domain.orgsync.OrgRoleRepository;
import com.iflytek.skillhub.domain.orgsync.OrgSyncState;
import com.iflytek.skillhub.domain.orgsync.OrgSyncStateRepository;
import com.iflytek.skillhub.domain.orgsync.OrgUserDepartment;
import com.iflytek.skillhub.domain.orgsync.OrgUserDepartmentRepository;
import com.iflytek.skillhub.domain.orgsync.OrgUserProfile;
import com.iflytek.skillhub.domain.orgsync.OrgUserProfileRepository;
import com.iflytek.skillhub.domain.orgsync.OrgUserRole;
import com.iflytek.skillhub.domain.orgsync.OrgUserRoleRepository;
import com.iflytek.skillhub.domain.user.PermissionSource;
import com.iflytek.skillhub.domain.user.UserAccount;
import com.iflytek.skillhub.domain.user.UserAccountRepository;
import com.iflytek.skillhub.domain.user.UserStatus;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Synchronizes DingTalk organization data into local projection tables.
 */
@Service
public class DingTalkOrgSyncService {

    private static final Logger logger = LoggerFactory.getLogger(DingTalkOrgSyncService.class);
    public static final String STATE_KEY_FULL_SYNC = "FULL_SYNC";

    private final DingTalkRuntimeConfig runtimeConfig;
    private final DingTalkTokenClient tokenClient;
    private final DingTalkDirectoryClient directoryClient;
    private final OrgDepartmentRepository orgDepartmentRepository;
    private final OrgUserProfileRepository orgUserProfileRepository;
    private final OrgUserDepartmentRepository orgUserDepartmentRepository;
    private final OrgRoleRepository orgRoleRepository;
    private final OrgUserRoleRepository orgUserRoleRepository;
    private final OrgSyncStateRepository orgSyncStateRepository;
    private final OrgEventCheckpointRepository orgEventCheckpointRepository;
    private final UserAccountRepository userAccountRepository;
    private final UserRoleBindingRepository userRoleBindingRepository;
    private final RoleRepository roleRepository;
    private final MeterRegistry meterRegistry;
    private final Clock clock;

    public DingTalkOrgSyncService(
            DingTalkRuntimeConfig runtimeConfig,
            DingTalkTokenClient tokenClient,
            DingTalkDirectoryClient directoryClient,
            OrgDepartmentRepository orgDepartmentRepository,
            OrgUserProfileRepository orgUserProfileRepository,
            OrgUserDepartmentRepository orgUserDepartmentRepository,
            OrgRoleRepository orgRoleRepository,
            OrgUserRoleRepository orgUserRoleRepository,
            OrgSyncStateRepository orgSyncStateRepository,
            OrgEventCheckpointRepository orgEventCheckpointRepository,
            UserAccountRepository userAccountRepository,
            UserRoleBindingRepository userRoleBindingRepository,
            RoleRepository roleRepository,
            MeterRegistry meterRegistry,
            Clock clock) {
        this.runtimeConfig = runtimeConfig;
        this.tokenClient = tokenClient;
        this.directoryClient = directoryClient;
        this.orgDepartmentRepository = orgDepartmentRepository;
        this.orgUserProfileRepository = orgUserProfileRepository;
        this.orgUserDepartmentRepository = orgUserDepartmentRepository;
        this.orgRoleRepository = orgRoleRepository;
        this.orgUserRoleRepository = orgUserRoleRepository;
        this.orgSyncStateRepository = orgSyncStateRepository;
        this.orgEventCheckpointRepository = orgEventCheckpointRepository;
        this.userAccountRepository = userAccountRepository;
        this.userRoleBindingRepository = userRoleBindingRepository;
        this.roleRepository = roleRepository;
        this.meterRegistry = meterRegistry;
        this.clock = clock;
    }

    /**
     * Whether a startup full sync should run (DingTalk enabled and no successful baseline yet).
     */
    public boolean needsInitialFullSync() {
        if (!isOrgSyncConfigured()) {
            return false;
        }
        return orgSyncStateRepository.findById(STATE_KEY_FULL_SYNC)
                .map(state -> !state.isSuccess() || state.getLastRunAt() == null)
                .orElse(true);
    }

    public boolean isOrgSyncConfigured() {
        return runtimeConfig.enabled() && StringUtils.hasText(runtimeConfig.defaultCorpId());
    }

    @Transactional
    public void runFullSync() {
        if (!isOrgSyncConfigured()) {
            logger.info("Skip DingTalk org sync because DingTalk auth is disabled or corpId is missing");
            return;
        }
        Instant now = Instant.now(clock);
        OrgSyncState state = orgSyncStateRepository.findById(STATE_KEY_FULL_SYNC)
                .orElse(new OrgSyncState(STATE_KEY_FULL_SYNC, "STARTED"));
        try {
            String accessToken = tokenClient.getCorpAccessToken(
                    runtimeConfig.defaultCorpId(),
                    runtimeConfig.clientId(),
                    runtimeConfig.clientSecret()
            );
            syncDepartments(accessToken, now);
            syncRoles(accessToken);
            syncUsers(accessToken, now);
            state.setPhase("DONE");
            state.setSuccess(true);
            state.setLastError(null);
            state.setLastRunAt(now);
            orgSyncStateRepository.save(state);
            meterRegistry.counter("skillhub.dingtalk.sync.full.success").increment();
            logger.info("DingTalk full sync finished successfully");
        } catch (Exception ex) {
            state.setPhase("FAILED");
            state.setSuccess(false);
            state.setLastError(ex.getMessage());
            state.setLastRunAt(now);
            orgSyncStateRepository.save(state);
            meterRegistry.counter("skillhub.dingtalk.sync.full.failed").increment();
            throw ex;
        }
    }

    @Transactional
    public boolean markEventProcessing(String eventKey, String eventType) {
        if (orgEventCheckpointRepository.findById(eventKey).isPresent()) {
            meterRegistry.counter("skillhub.dingtalk.event.duplicate").increment();
            return false;
        }
        orgEventCheckpointRepository.save(new OrgEventCheckpoint(eventKey, eventType, "PROCESSING"));
        meterRegistry.counter("skillhub.dingtalk.event.received").increment();
        return true;
    }

    @Transactional
    public void markEventProcessed(String eventKey) {
        orgEventCheckpointRepository.findById(eventKey).ifPresent(checkpoint -> {
            checkpoint.setStatus("SUCCESS");
            checkpoint.setProcessedAt(Instant.now(clock));
            checkpoint.setErrorMessage(null);
            orgEventCheckpointRepository.save(checkpoint);
        });
    }

    @Transactional
    public void markEventFailed(String eventKey, String message) {
        orgEventCheckpointRepository.findById(eventKey).ifPresent(checkpoint -> {
            checkpoint.setStatus("FAILED");
            checkpoint.setErrorMessage(message);
            orgEventCheckpointRepository.save(checkpoint);
            meterRegistry.counter("skillhub.dingtalk.event.failed").increment();
        });
    }

    @Transactional
    public void handleUserEvent(String userId) {
        if (!runtimeConfig.enabled() || !StringUtils.hasText(runtimeConfig.defaultCorpId())) {
            return;
        }
        String accessToken = tokenClient.getCorpAccessToken(
                runtimeConfig.defaultCorpId(),
                runtimeConfig.clientId(),
                runtimeConfig.clientSecret()
        );
        var detail = directoryClient.getUser(accessToken, userId);
        upsertUserProjection(detail.userid(), detail.unionid(), detail.name(), detail.title(), detail.mobile(),
                detail.email(), Boolean.TRUE.equals(detail.active()), detail.managerUserId(), detail.deptIdList(), Instant.now(clock));
    }

    @Transactional
    public void handleUserLeave(String userId) {
        userAccountRepository.findById(userId).ifPresent(user -> {
            user.setStatus(UserStatus.DISABLED);
            userAccountRepository.save(user);
        });
        orgUserProfileRepository.findById(userId).ifPresent(profile -> {
            profile.setActive(false);
            profile.setLastSyncAt(Instant.now(clock));
            orgUserProfileRepository.save(profile);
        });
    }

    @Transactional
    public int initializeUninitializedUserPermissions(boolean dryRun) {
        List<UserAccount> users = userAccountRepository.findAll().stream()
                .filter(user -> user.getPermissionInitializedAt() == null)
                .toList();
        if (dryRun) {
            return users.size();
        }
        for (UserAccount user : users) {
            Set<String> mappedRoles = mapRolesFromOrgSnapshot(user.getId());
            applyInitialRolesIfNeeded(user, mappedRoles);
        }
        return users.size();
    }

    private void syncDepartments(String accessToken, Instant now) {
        ArrayDeque<Long> queue = new ArrayDeque<>();
        Set<Long> visited = new HashSet<>();
        queue.add(1L);
        while (!queue.isEmpty()) {
            Long deptId = queue.poll();
            if (!visited.add(deptId)) {
                continue;
            }
            var detail = directoryClient.getDepartment(accessToken, deptId);
            OrgDepartment department = orgDepartmentRepository.findById(deptId)
                    .orElse(new OrgDepartment(detail.deptId(), detail.parentId(), detail.name(), detail.order()));
            department.setName(detail.name());
            department.setParentDeptId(detail.parentId());
            department.setDeptOrder(detail.order());
            department.setDeletedAt(null);
            department.setLastSyncAt(now);
            orgDepartmentRepository.save(department);
            queue.addAll(directoryClient.listSubDepartmentIds(accessToken, deptId));
        }
    }

    private void syncUsers(String accessToken, Instant now) {
        Set<String> processedUsers = new HashSet<>();
        List<OrgDepartment> departments = orgDepartmentRepository.findAll();
        for (OrgDepartment department : departments) {
            long cursor = 0L;
            boolean hasMore = true;
            while (hasMore) {
                var result = directoryClient.listUsersByDepartment(accessToken, department.getDeptId(), cursor, 100);
                for (var user : result.users()) {
                    if (!processedUsers.add(user.userid())) {
                        continue;
                    }
                    var detail = directoryClient.getUser(accessToken, user.userid());
                    upsertUserProjection(detail.userid(), detail.unionid(), detail.name(), detail.title(), detail.mobile(),
                            detail.email(), Boolean.TRUE.equals(detail.active()), detail.managerUserId(), detail.deptIdList(), now);
                }
                hasMore = result.hasMore();
                cursor = result.nextCursor();
            }
        }
    }

    private void syncRoles(String accessToken) {
        orgUserRoleRepository.deleteAll();
        Set<Long> syncedRoleIds = new HashSet<>();
        int offset = 0;
        int size = 200;
        while (true) {
            List<DingTalkDirectoryClient.RoleGroupDto> groups = directoryClient.listRoles(accessToken, offset, size);
            if (groups.isEmpty()) {
                return;
            }
            for (var group : groups) {
                if (group.roles() == null) {
                    continue;
                }
                for (var roleDto : group.roles()) {
                    if (!syncedRoleIds.add(roleDto.id())) {
                        continue;
                    }
                    OrgRole role = orgRoleRepository.findById(roleDto.id())
                            .orElse(new OrgRole(roleDto.id(), group.groupId(), group.name(), roleDto.name()));
                    orgRoleRepository.save(role);
                    syncRoleMembers(accessToken, roleDto.id(), roleDto.name());
                }
            }
            if (groups.size() < size) {
                return;
            }
            offset += size;
        }
    }

    private void syncRoleMembers(String accessToken, Long roleId, String roleName) {
        int cursor = 0;
        boolean hasMore = true;
        Set<String> seenUsers = new HashSet<>();
        while (hasMore) {
            var users = directoryClient.listRoleUsers(accessToken, roleId, cursor, 100);
            for (var user : users.users()) {
                if (!seenUsers.add(user.userid())) {
                    continue;
                }
                orgUserRoleRepository.save(new OrgUserRole(user.userid(), roleId, roleName, "ROLE_SYNC"));
            }
            hasMore = users.hasMore();
            cursor = users.nextCursor();
        }
    }

    private void upsertUserProjection(String userId,
                                      String unionId,
                                      String name,
                                      String title,
                                      String mobile,
                                      String email,
                                      boolean active,
                                      String managerUserId,
                                      List<Long> deptIds,
                                      Instant now) {
        OrgUserProfile profile = orgUserProfileRepository.findById(userId)
                .orElse(new OrgUserProfile(userId, unionId, name));
        profile.setUnionId(unionId);
        profile.setName(name);
        profile.setTitle(title);
        profile.setMobile(mobile);
        profile.setEmail(email);
        profile.setActive(active);
        profile.setManagerUserId(managerUserId);
        if (deptIds != null && !deptIds.isEmpty()) {
            profile.setPrimaryDeptId(deptIds.get(0));
        }
        profile.setLastSyncAt(now);
        orgUserProfileRepository.save(profile);

        orgUserDepartmentRepository.deleteByUserId(userId);
        if (deptIds != null) {
            Set<Long> addedDeptIds = new HashSet<>();
            boolean primaryAssigned = false;
            for (Long deptId : deptIds) {
                if (deptId == null || !addedDeptIds.add(deptId)) {
                    continue;
                }
                boolean primary = !primaryAssigned;
                primaryAssigned = primaryAssigned || primary;
                orgUserDepartmentRepository.save(new OrgUserDepartment(userId, deptId, primary));
            }
        }

        upsertUserAccount(profile);
    }

    private void upsertUserAccount(OrgUserProfile profile) {
        UserAccount user = userAccountRepository.findById(profile.getUserId())
                .orElse(new UserAccount(
                        profile.getUserId(),
                        profile.getName(),
                        profile.getEmail(),
                        null,
                        resolveDepartmentName(profile.getPrimaryDeptId())
                ));
        user.setDisplayName(profile.getName());
        user.setEmail(profile.getEmail());
        user.setDepartment(resolveDepartmentName(profile.getPrimaryDeptId()));
        user.setStatus(profile.isActive() ? UserStatus.ACTIVE : UserStatus.DISABLED);
        userAccountRepository.save(user);

        if (user.getPermissionInitializedAt() == null) {
            applyInitialRolesIfNeeded(user, mapRolesFromOrgSnapshot(user.getId()));
        }
    }

    private void applyInitialRolesIfNeeded(UserAccount user, Set<String> mappedRoles) {
        if (user.getPermissionInitializedAt() != null) {
            return;
        }
        userRoleBindingRepository.deleteByUserId(user.getId());
        for (String roleCode : mappedRoles) {
            if ("USER".equals(roleCode)) {
                continue;
            }
            roleRepository.findByCode(roleCode).ifPresent(role -> userRoleBindingRepository.save(new UserRoleBinding(user.getId(), role)));
        }
        user.setPermissionSource(PermissionSource.INITIAL_FROM_DINGTALK);
        user.setPermissionInitializedAt(Instant.now(clock));
        userAccountRepository.save(user);
    }

    private Set<String> mapRolesFromOrgSnapshot(String userId) {
        Set<String> result = new HashSet<>();
        List<OrgUserRole> orgRoles = orgUserRoleRepository.findByUserId(userId);
        for (OrgUserRole orgRole : orgRoles) {
            String roleName = orgRole.getRoleName() == null ? "" : orgRole.getRoleName().toLowerCase(Locale.ROOT);
            if (roleName.contains("主管理员") || roleName.contains("超级管理员") || roleName.contains("super")) {
                result.add("SUPER_ADMIN");
            } else if (roleName.contains("用户管理员") || roleName.contains("user admin")) {
                result.add("USER_ADMIN");
            } else if (roleName.contains("技能管理员") || roleName.contains("审核") || roleName.contains("skill admin")) {
                result.add("SKILL_ADMIN");
            } else if (roleName.contains("审计") || roleName.contains("auditor")) {
                result.add("AUDITOR");
            }
        }
        if (result.isEmpty()) {
            result.add("USER");
        }
        return result;
    }

    private String resolveDepartmentName(Long primaryDeptId) {
        if (primaryDeptId == null) {
            return null;
        }
        return orgDepartmentRepository.findById(primaryDeptId)
                .map(OrgDepartment::getName)
                .orElse(null);
    }
}
