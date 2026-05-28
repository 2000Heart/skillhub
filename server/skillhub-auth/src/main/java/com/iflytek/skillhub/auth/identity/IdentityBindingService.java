package com.iflytek.skillhub.auth.identity;

import com.iflytek.skillhub.auth.dingtalk.DingTalkClaimsMapper;
import com.iflytek.skillhub.auth.entity.IdentityBinding;
import com.iflytek.skillhub.auth.oauth.OAuthClaims;
import com.iflytek.skillhub.auth.rbac.PlatformPrincipal;
import com.iflytek.skillhub.auth.rbac.PlatformRoleDefaults;
import com.iflytek.skillhub.auth.repository.IdentityBindingRepository;
import com.iflytek.skillhub.auth.repository.UserRoleBindingRepository;
import com.iflytek.skillhub.domain.namespace.GlobalNamespaceMembershipService;
import com.iflytek.skillhub.domain.orgsync.OrgUserProfile;
import com.iflytek.skillhub.domain.orgsync.OrgUserProfileRepository;
import com.iflytek.skillhub.domain.user.UserAccount;
import com.iflytek.skillhub.domain.user.UserAccountRepository;
import com.iflytek.skillhub.domain.user.UserStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import java.util.UUID;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Resolves external OAuth identities to platform users, creating or updating
 * bindings and user records as needed.
 */
@Service
public class IdentityBindingService {

    private final IdentityBindingRepository bindingRepo;
    private final UserAccountRepository userRepo;
    private final UserRoleBindingRepository roleBindingRepo;
    private final GlobalNamespaceMembershipService globalNamespaceMembershipService;
    private final OrgUserProfileRepository orgUserProfileRepository;

    public IdentityBindingService(IdentityBindingRepository bindingRepo,
                                  UserAccountRepository userRepo,
                                  UserRoleBindingRepository roleBindingRepo,
                                  GlobalNamespaceMembershipService globalNamespaceMembershipService,
                                  OrgUserProfileRepository orgUserProfileRepository) {
        this.bindingRepo = bindingRepo;
        this.userRepo = userRepo;
        this.roleBindingRepo = roleBindingRepo;
        this.globalNamespaceMembershipService = globalNamespaceMembershipService;
        this.orgUserProfileRepository = orgUserProfileRepository;
    }

    @Transactional
    public PlatformPrincipal bindOrCreate(OAuthClaims claims, UserStatus initialStatus) {
        IdentityBinding binding = bindingRepo
            .findByProviderCodeAndSubject(claims.provider(), claims.subject())
            .orElse(null);

        UserAccount user;
        if (binding != null) {
            user = userRepo.findById(binding.getUserId())
                .orElseThrow(() -> new IllegalStateException("User not found for binding"));
            user.setDisplayName(resolveDisplayName(claims));
            if (claims.email() != null) user.setEmail(claims.email());
            if (claims.extra().get("avatar_url") != null) {
                user.setAvatarUrl((String) claims.extra().get("avatar_url"));
            }
            if (claims.extra().get("department") != null) {
                user.setDepartment((String) claims.extra().get("department"));
            }
            user = userRepo.save(user);
        } else {
            user = resolveOrCreateUser(claims, initialStatus);
            if (initialStatus == UserStatus.ACTIVE) {
                globalNamespaceMembershipService.ensureMember(user.getId());
            }

            binding = new IdentityBinding(user.getId(), claims.provider(), claims.subject(), claims.providerLogin());
            bindingRepo.save(binding);
        }

        if (user.getStatus() == UserStatus.PENDING) {
            throw new com.iflytek.skillhub.auth.oauth.AccountPendingException();
        }
        if (user.getStatus() == UserStatus.DISABLED) {
            throw new com.iflytek.skillhub.auth.oauth.AccountDisabledException();
        }

        Set<String> roles = roleBindingRepo.findByUserId(user.getId()).stream()
            .map(rb -> rb.getRole().getCode())
            .collect(Collectors.toSet());
        roles = PlatformRoleDefaults.withDefaultUserRole(roles);

        return new PlatformPrincipal(
            user.getId(), user.getDisplayName(), user.getEmail(),
            user.getAvatarUrl(), claims.provider(), roles, user.getDepartment()
        );
    }

    @Transactional
    public void createPendingUserIfAbsent(OAuthClaims claims) {
        IdentityBinding existingBinding = bindingRepo
            .findByProviderCodeAndSubject(claims.provider(), claims.subject())
            .orElse(null);
        if (existingBinding != null) {
            UserAccount existingUser = userRepo.findById(existingBinding.getUserId())
                .orElseThrow(() -> new IllegalStateException("User not found for binding"));
            if (existingUser.getStatus() == UserStatus.DISABLED) {
                throw new com.iflytek.skillhub.auth.oauth.AccountDisabledException();
            }
            throw new com.iflytek.skillhub.auth.oauth.AccountPendingException();
        }

        UserAccount user = resolveOrCreateUser(claims, UserStatus.PENDING);

        IdentityBinding binding = new IdentityBinding(
                user.getId(),
                claims.provider(),
                claims.subject(),
                resolveDisplayName(claims)
        );
        bindingRepo.save(binding);
    }

    private UserAccount resolveOrCreateUser(OAuthClaims claims, UserStatus initialStatus) {
        String userId = resolvePlatformUserId(claims);
        UserAccount user = userRepo.findById(userId).orElseGet(() -> new UserAccount(
                userId,
                resolveDisplayName(claims),
                claims.email(),
                (String) claims.extra().get("avatar_url"),
                (String) claims.extra().get("department")
        ));
        user.setDisplayName(resolveDisplayName(claims));
        if (claims.email() != null) {
            user.setEmail(claims.email());
        }
        if (claims.extra().get("avatar_url") != null) {
            user.setAvatarUrl((String) claims.extra().get("avatar_url"));
        }
        if (claims.extra().get("department") != null) {
            user.setDepartment((String) claims.extra().get("department"));
        }
        user.setStatus(initialStatus);
        return userRepo.save(user);
    }

    private String resolvePlatformUserId(OAuthClaims claims) {
        if (DingTalkClaimsMapper.PROVIDER_CODE.equals(claims.provider())) {
            return orgUserProfileRepository.findByUnionId(claims.subject())
                    .map(OrgUserProfile::getUserId)
                    .orElseGet(() -> "usr_" + UUID.randomUUID());
        }
        return "usr_" + UUID.randomUUID();
    }

    private static String resolveDisplayName(OAuthClaims claims) {
        if (StringUtils.hasText(claims.providerLogin())) {
            return claims.providerLogin().trim();
        }
        return claims.subject();
    }
}
