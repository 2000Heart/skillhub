package com.iflytek.skillhub.auth.dingtalk;

import com.iflytek.skillhub.auth.oauth.OAuthClaims;
import com.iflytek.skillhub.auth.oauth.OAuthLoginFlowService;
import com.iflytek.skillhub.auth.rbac.PlatformPrincipal;
import com.iflytek.skillhub.auth.rbac.PlatformRoleDefaults;
import com.iflytek.skillhub.auth.entity.Role;
import com.iflytek.skillhub.auth.entity.UserRoleBinding;
import com.iflytek.skillhub.auth.repository.RoleRepository;
import com.iflytek.skillhub.auth.repository.UserRoleBindingRepository;
import com.iflytek.skillhub.auth.session.PlatformSessionService;
import com.iflytek.skillhub.domain.user.UserAccountRepository;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Orchestrates DingTalk login for in-app auth codes and browser OAuth callbacks.
 */
@Service
public class DingTalkLoginService {

    private final DingTalkTokenClient tokenClient;
    private final DingTalkUserResolver userResolver;
    private final DingTalkClaimsMapper claimsMapper;
    private final OAuthLoginFlowService oauthLoginFlowService;
    private final PlatformSessionService platformSessionService;
    private final UserRoleBindingRepository userRoleBindingRepository;
    private final RoleRepository roleRepository;
    private final DingTalkRuntimeConfig runtimeConfig;
    private final DingTalkPrivilegeEvaluator privilegeEvaluator;
    private final UserAccountRepository userAccountRepository;

    public DingTalkLoginService(DingTalkTokenClient tokenClient,
                                DingTalkUserResolver userResolver,
                                DingTalkClaimsMapper claimsMapper,
                                OAuthLoginFlowService oauthLoginFlowService,
                                PlatformSessionService platformSessionService,
                                UserRoleBindingRepository userRoleBindingRepository,
                                RoleRepository roleRepository,
                                DingTalkRuntimeConfig runtimeConfig,
                                DingTalkPrivilegeEvaluator privilegeEvaluator,
                                UserAccountRepository userAccountRepository) {
        this.tokenClient = tokenClient;
        this.userResolver = userResolver;
        this.claimsMapper = claimsMapper;
        this.oauthLoginFlowService = oauthLoginFlowService;
        this.platformSessionService = platformSessionService;
        this.userRoleBindingRepository = userRoleBindingRepository;
        this.roleRepository = roleRepository;
        this.runtimeConfig = runtimeConfig;
        this.privilegeEvaluator = privilegeEvaluator;
        this.userAccountRepository = userAccountRepository;
    }

    public void assertEnabled() {
        if (!runtimeConfig.enabled()) {
            throw new DingTalkAuthException("DingTalk authentication is disabled");
        }
    }

    @Transactional
    public PlatformPrincipal loginWithAuthCode(String code, String corpId, HttpServletRequest request) {
        assertEnabled();
        String resolvedCorpId = resolveCorpId(corpId);
        DingTalkUserProfile profile = userResolver.resolveFromAuthCode(
                resolvedCorpId,
                runtimeConfig.clientId(),
                runtimeConfig.clientSecret(),
                code
        );
        return establishSession(profile, resolvedCorpId, request);
    }

    @Transactional
    public PlatformPrincipal loginWithOAuthCode(String code, HttpServletRequest request) {
        assertEnabled();
        String userAccessToken = tokenClient.getUserAccessToken(
                runtimeConfig.clientId(),
                runtimeConfig.clientSecret(),
                code
        );
        DingTalkUserProfile profile = userResolver.resolveFromUserAccessToken(userAccessToken);
        return establishSession(profile, runtimeConfig.defaultCorpId(), request);
    }

    private PlatformPrincipal establishSession(DingTalkUserProfile profile,
                                               String corpId,
                                               HttpServletRequest request) {
        OAuthClaims claims = claimsMapper.toClaims(profile, corpId);
        PlatformPrincipal principal = oauthLoginFlowService.authenticate(claims);
        syncDepartment(principal.userId(), profile.department());
        ensureSuperAdmin(profile.unionId(), principal.userId());
        ensureSuperAdminForManager(profile, principal.userId());
        platformSessionService.establishSession(reloadPrincipal(principal), request);
        return reloadPrincipal(principal);
    }

    private PlatformPrincipal reloadPrincipal(PlatformPrincipal principal) {
        var roles = userRoleBindingRepository.findByUserId(principal.userId()).stream()
                .map(binding -> binding.getRole().getCode())
                .collect(java.util.stream.Collectors.toSet());
        roles = PlatformRoleDefaults.withDefaultUserRole(roles);
        String department = userAccountRepository.findById(principal.userId())
                .map(com.iflytek.skillhub.domain.user.UserAccount::getDepartment)
                .orElse(principal.department());
        return new PlatformPrincipal(
                principal.userId(),
                principal.displayName(),
                principal.email(),
                principal.avatarUrl(),
                principal.oauthProvider(),
                roles,
                department
        );
    }

    private void syncDepartment(String userId, String department) {
        userAccountRepository.findById(userId).ifPresent(user -> {
            if (!java.util.Objects.equals(user.getDepartment(), department)) {
                user.setDepartment(department);
                userAccountRepository.save(user);
            }
        });
    }

    private void ensureSuperAdminForManager(DingTalkUserProfile profile, String userId) {
        if (!runtimeConfig.autoSuperAdminForManagers()) {
            return;
        }
        if (!privilegeEvaluator.shouldPromoteToSuperAdmin(profile)) {
            return;
        }
        ensureRoleBinding(userId, "SUPER_ADMIN");
    }

    private void ensureSuperAdmin(String unionId, String userId) {
        if (!StringUtils.hasText(unionId) || runtimeConfig.superAdminUnionIds().isEmpty()) {
            return;
        }
        if (!runtimeConfig.superAdminUnionIds().contains(unionId)) {
            return;
        }
        ensureRoleBinding(userId, "SUPER_ADMIN");
    }

    private void ensureRoleBinding(String userId, String roleCode) {
        List<UserRoleBinding> existing = userRoleBindingRepository.findByUserId(userId);
        boolean alreadyBound = existing.stream()
                .anyMatch(binding -> roleCode.equals(binding.getRole().getCode()));
        if (alreadyBound) {
            return;
        }
        Role role = roleRepository.findByCode(roleCode)
                .orElseThrow(() -> new IllegalStateException(roleCode + " role is missing"));
        userRoleBindingRepository.save(new UserRoleBinding(userId, role));
    }

    private String resolveCorpId(String corpId) {
        if (StringUtils.hasText(corpId)) {
            return corpId.trim();
        }
        if (StringUtils.hasText(runtimeConfig.defaultCorpId())) {
            return runtimeConfig.defaultCorpId().trim();
        }
        throw new DingTalkAuthException("corpId is required for DingTalk in-app login");
    }
}
