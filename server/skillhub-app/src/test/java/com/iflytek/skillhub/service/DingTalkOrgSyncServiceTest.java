package com.iflytek.skillhub.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.iflytek.skillhub.auth.dingtalk.DingTalkDirectoryClient;
import com.iflytek.skillhub.auth.dingtalk.DingTalkRuntimeConfig;
import com.iflytek.skillhub.auth.dingtalk.DingTalkTokenClient;
import com.iflytek.skillhub.auth.repository.RoleRepository;
import com.iflytek.skillhub.auth.repository.UserRoleBindingRepository;
import com.iflytek.skillhub.domain.orgsync.OrgDepartmentRepository;
import com.iflytek.skillhub.domain.orgsync.OrgEventCheckpointRepository;
import com.iflytek.skillhub.domain.orgsync.OrgRoleRepository;
import com.iflytek.skillhub.domain.orgsync.OrgSyncState;
import com.iflytek.skillhub.domain.orgsync.OrgSyncStateRepository;
import com.iflytek.skillhub.domain.orgsync.OrgUserDepartmentRepository;
import com.iflytek.skillhub.domain.orgsync.OrgUserProfileRepository;
import com.iflytek.skillhub.domain.orgsync.OrgUserRoleRepository;
import com.iflytek.skillhub.domain.user.UserAccount;
import com.iflytek.skillhub.domain.user.UserAccountRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DingTalkOrgSyncServiceTest {

    @Mock private DingTalkTokenClient tokenClient;
    @Mock private DingTalkDirectoryClient directoryClient;
    @Mock private OrgDepartmentRepository orgDepartmentRepository;
    @Mock private OrgUserProfileRepository orgUserProfileRepository;
    @Mock private OrgUserDepartmentRepository orgUserDepartmentRepository;
    @Mock private OrgRoleRepository orgRoleRepository;
    @Mock private OrgUserRoleRepository orgUserRoleRepository;
    @Mock private OrgSyncStateRepository orgSyncStateRepository;
    @Mock private OrgEventCheckpointRepository orgEventCheckpointRepository;
    @Mock private UserAccountRepository userAccountRepository;
    @Mock private UserRoleBindingRepository userRoleBindingRepository;
    @Mock private RoleRepository roleRepository;

    private DingTalkOrgSyncService service;

    @BeforeEach
    void setUp() {
        service = new DingTalkOrgSyncService(
                new DingTalkRuntimeConfig(false, "", "", "", "", List.of(), false),
                tokenClient,
                directoryClient,
                orgDepartmentRepository,
                orgUserProfileRepository,
                orgUserDepartmentRepository,
                orgRoleRepository,
                orgUserRoleRepository,
                orgSyncStateRepository,
                orgEventCheckpointRepository,
                userAccountRepository,
                userRoleBindingRepository,
                roleRepository,
                new SimpleMeterRegistry(),
                Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC)
        );
    }

    @Test
    void initializePermissionsDryRunReturnsCountWithoutMutation() {
        UserAccount uninitialized = new UserAccount("u-1", "Test", null, null);
        when(userAccountRepository.findAll()).thenReturn(List.of(uninitialized));

        int count = service.initializeUninitializedUserPermissions(true);

        assertThat(count).isEqualTo(1);
        verify(userRoleBindingRepository, never()).deleteByUserId(any());
    }

    @Test
    void needsInitialFullSync_whenDisabled_returnsFalse() {
        DingTalkOrgSyncService disabledService = new DingTalkOrgSyncService(
                new DingTalkRuntimeConfig(false, "", "", "", "", List.of(), false),
                tokenClient,
                directoryClient,
                orgDepartmentRepository,
                orgUserProfileRepository,
                orgUserDepartmentRepository,
                orgRoleRepository,
                orgUserRoleRepository,
                orgSyncStateRepository,
                orgEventCheckpointRepository,
                userAccountRepository,
                userRoleBindingRepository,
                roleRepository,
                new SimpleMeterRegistry(),
                Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC)
        );

        assertThat(disabledService.needsInitialFullSync()).isFalse();
    }

    @Test
    void needsInitialFullSync_whenNeverSynced_returnsTrue() {
        DingTalkOrgSyncService enabledService = new DingTalkOrgSyncService(
                new DingTalkRuntimeConfig(true, "id", "secret", "corp", "", List.of(), false),
                tokenClient,
                directoryClient,
                orgDepartmentRepository,
                orgUserProfileRepository,
                orgUserDepartmentRepository,
                orgRoleRepository,
                orgUserRoleRepository,
                orgSyncStateRepository,
                orgEventCheckpointRepository,
                userAccountRepository,
                userRoleBindingRepository,
                roleRepository,
                new SimpleMeterRegistry(),
                Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC)
        );
        when(orgSyncStateRepository.findById(DingTalkOrgSyncService.STATE_KEY_FULL_SYNC)).thenReturn(Optional.empty());

        assertThat(enabledService.needsInitialFullSync()).isTrue();
    }

    @Test
    void needsInitialFullSync_whenLastRunSucceeded_returnsFalse() {
        DingTalkOrgSyncService enabledService = new DingTalkOrgSyncService(
                new DingTalkRuntimeConfig(true, "id", "secret", "corp", "", List.of(), false),
                tokenClient,
                directoryClient,
                orgDepartmentRepository,
                orgUserProfileRepository,
                orgUserDepartmentRepository,
                orgRoleRepository,
                orgUserRoleRepository,
                orgSyncStateRepository,
                orgEventCheckpointRepository,
                userAccountRepository,
                userRoleBindingRepository,
                roleRepository,
                new SimpleMeterRegistry(),
                Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC)
        );
        OrgSyncState state = new OrgSyncState(DingTalkOrgSyncService.STATE_KEY_FULL_SYNC, "DONE");
        state.setSuccess(true);
        state.setLastRunAt(Instant.parse("2026-01-01T00:00:00Z"));
        when(orgSyncStateRepository.findById(DingTalkOrgSyncService.STATE_KEY_FULL_SYNC)).thenReturn(Optional.of(state));

        assertThat(enabledService.needsInitialFullSync()).isFalse();
    }

    @Test
    void markEventProcessingIsIdempotent() {
        when(orgEventCheckpointRepository.findById("evt-1"))
                .thenReturn(java.util.Optional.empty())
                .thenReturn(java.util.Optional.of(new com.iflytek.skillhub.domain.orgsync.OrgEventCheckpoint("evt-1", "user_add_org", "SUCCESS")));

        boolean first = service.markEventProcessing("evt-1", "user_add_org");
        boolean second = service.markEventProcessing("evt-1", "user_add_org");

        assertThat(first).isTrue();
        assertThat(second).isFalse();
    }
}
