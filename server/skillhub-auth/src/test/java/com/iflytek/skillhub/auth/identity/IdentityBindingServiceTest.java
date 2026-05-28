package com.iflytek.skillhub.auth.identity;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.iflytek.skillhub.auth.entity.IdentityBinding;
import com.iflytek.skillhub.auth.entity.Role;
import com.iflytek.skillhub.auth.entity.UserRoleBinding;
import com.iflytek.skillhub.auth.oauth.AccountDisabledException;
import com.iflytek.skillhub.auth.oauth.OAuthClaims;
import com.iflytek.skillhub.auth.oauth.AccountPendingException;
import com.iflytek.skillhub.auth.rbac.PlatformPrincipal;
import com.iflytek.skillhub.auth.repository.IdentityBindingRepository;
import com.iflytek.skillhub.auth.repository.UserRoleBindingRepository;
import com.iflytek.skillhub.auth.dingtalk.DingTalkClaimsMapper;
import com.iflytek.skillhub.domain.namespace.GlobalNamespaceMembershipService;
import com.iflytek.skillhub.domain.orgsync.OrgUserProfile;
import com.iflytek.skillhub.domain.orgsync.OrgUserProfileRepository;
import com.iflytek.skillhub.domain.user.UserAccount;
import com.iflytek.skillhub.domain.user.UserAccountRepository;
import com.iflytek.skillhub.domain.user.UserStatus;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class IdentityBindingServiceTest {

    @Mock
    private IdentityBindingRepository bindingRepo;

    @Mock
    private UserAccountRepository userRepo;

    @Mock
    private UserRoleBindingRepository roleBindingRepo;

    @Mock
    private GlobalNamespaceMembershipService globalNamespaceMembershipService;

    @Mock
    private OrgUserProfileRepository orgUserProfileRepository;

    private IdentityBindingService service;

    @BeforeEach
    void setUp() {
        service = new IdentityBindingService(
                bindingRepo,
                userRepo,
                roleBindingRepo,
                globalNamespaceMembershipService,
                orgUserProfileRepository
        );
    }

    @Test
    void bindOrCreate_assignsGlobalMembershipForActiveNewUsers() {
        OAuthClaims claims = new OAuthClaims(
                "github",
                "gh_1",
                "alice@example.com",
                true,
                "alice",
                Map.of("avatar_url", "https://example.test/a.png")
        );
        when(bindingRepo.findByProviderCodeAndSubject("github", "gh_1")).thenReturn(Optional.empty());
        when(userRepo.save(any(UserAccount.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(roleBindingRepo.findByUserId(any())).thenReturn(List.of());

        PlatformPrincipal principal = service.bindOrCreate(claims, UserStatus.ACTIVE);

        ArgumentCaptor<UserAccount> userCaptor = ArgumentCaptor.forClass(UserAccount.class);
        verify(userRepo).save(userCaptor.capture());
        verify(globalNamespaceMembershipService).ensureMember(userCaptor.getValue().getId());
        verify(bindingRepo).save(any(IdentityBinding.class));
        assertThat(principal.displayName()).isEqualTo("alice");
        assertThat(principal.oauthProvider()).isEqualTo("github");
    }

    @Test
    void bindOrCreate_doesNotAssignGlobalMembershipForPendingUsers() {
        OAuthClaims claims = new OAuthClaims(
                "github",
                "gh_1",
                "alice@example.com",
                true,
                "alice",
                Map.of()
        );
        when(bindingRepo.findByProviderCodeAndSubject("github", "gh_1")).thenReturn(Optional.empty());
        when(userRepo.save(any(UserAccount.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertThatThrownBy(() -> service.bindOrCreate(claims, UserStatus.PENDING))
                .isInstanceOf(AccountPendingException.class);

        verify(globalNamespaceMembershipService, never()).ensureMember(any());
    }

    @Test
    void bindOrCreate_defaultsToUserRoleWhenNoBindingsExist() {
        OAuthClaims claims = new OAuthClaims(
                "github",
                "gh_1",
                "alice@example.com",
                true,
                "alice",
                Map.of()
        );
        when(bindingRepo.findByProviderCodeAndSubject("github", "gh_1")).thenReturn(Optional.empty());
        when(userRepo.save(any(UserAccount.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(roleBindingRepo.findByUserId(any())).thenReturn(List.of());

        PlatformPrincipal principal = service.bindOrCreate(claims, UserStatus.ACTIVE);

        assertThat(principal.platformRoles()).containsExactly("USER");
    }

    @Test
    void bindOrCreate_existingDisabledUser_throwsAccountDisabled() {
        OAuthClaims claims = new OAuthClaims(
                "github",
                "gh_1",
                "alice@example.com",
                true,
                "alice",
                Map.of()
        );
        IdentityBinding binding = new IdentityBinding("usr_1", "github", "gh_1", "alice");
        UserAccount user = new UserAccount("usr_1", "alice", "alice@example.com", null);
        user.setStatus(UserStatus.DISABLED);

        when(bindingRepo.findByProviderCodeAndSubject("github", "gh_1")).thenReturn(Optional.of(binding));
        when(userRepo.findById("usr_1")).thenReturn(Optional.of(user));
        when(userRepo.save(any(UserAccount.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertThatThrownBy(() -> service.bindOrCreate(claims, UserStatus.ACTIVE))
                .isInstanceOf(AccountDisabledException.class);
    }

    @Test
    void bindOrCreate_returnsExplicitPlatformRolesWhenBindingsExist() {
        OAuthClaims claims = new OAuthClaims(
                "github",
                "gh_1",
                "alice@example.com",
                true,
                "alice",
                Map.of()
        );
        when(bindingRepo.findByProviderCodeAndSubject("github", "gh_1")).thenReturn(Optional.empty());
        when(userRepo.save(any(UserAccount.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Role role = new Role();
        ReflectionTestUtils.setField(role, "code", "AUDITOR");
        when(roleBindingRepo.findByUserId(any())).thenReturn(List.of(new UserRoleBinding("usr_1", role)));

        PlatformPrincipal principal = service.bindOrCreate(claims, UserStatus.ACTIVE);

        assertThat(principal.platformRoles()).containsExactly("AUDITOR");
    }

    @Test
    void bindOrCreate_dingtalkLinksExistingOrgUserByUnionId() {
        OAuthClaims claims = new OAuthClaims(
                DingTalkClaimsMapper.PROVIDER_CODE,
                "union-1",
                null,
                false,
                "张鸿",
                Map.of("department", "研发")
        );
        OrgUserProfile orgProfile = new OrgUserProfile("4350194725795423", "union-1", "张鸿");
        UserAccount syncedUser = new UserAccount("4350194725795423", "张鸿", null, null, "研发");

        when(bindingRepo.findByProviderCodeAndSubject(DingTalkClaimsMapper.PROVIDER_CODE, "union-1"))
                .thenReturn(Optional.empty());
        when(orgUserProfileRepository.findByUnionId("union-1")).thenReturn(Optional.of(orgProfile));
        when(userRepo.findById("4350194725795423")).thenReturn(Optional.of(syncedUser));
        when(userRepo.save(any(UserAccount.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Role role = new Role();
        ReflectionTestUtils.setField(role, "code", "SUPER_ADMIN");
        when(roleBindingRepo.findByUserId("4350194725795423"))
                .thenReturn(List.of(new UserRoleBinding("4350194725795423", role)));

        PlatformPrincipal principal = service.bindOrCreate(claims, UserStatus.ACTIVE);

        ArgumentCaptor<UserAccount> userCaptor = ArgumentCaptor.forClass(UserAccount.class);
        verify(userRepo).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getId()).isEqualTo("4350194725795423");
        assertThat(principal.userId()).isEqualTo("4350194725795423");
        assertThat(principal.platformRoles()).containsExactly("SUPER_ADMIN");
    }

    @Test
    void createPendingUserIfAbsent_existingDisabledBinding_throwsAccountDisabled() {
        OAuthClaims claims = new OAuthClaims(
                "github",
                "gh_1",
                "alice@example.com",
                true,
                "alice",
                Map.of()
        );
        IdentityBinding binding = new IdentityBinding("usr_1", "github", "gh_1", "alice");
        UserAccount user = new UserAccount("usr_1", "alice", "alice@example.com", null);
        user.setStatus(UserStatus.DISABLED);

        when(bindingRepo.findByProviderCodeAndSubject("github", "gh_1")).thenReturn(Optional.of(binding));
        when(userRepo.findById("usr_1")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> service.createPendingUserIfAbsent(claims))
                .isInstanceOf(AccountDisabledException.class);
    }
}
