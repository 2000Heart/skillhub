package com.iflytek.skillhub.auth.dingtalk;

import static org.assertj.core.api.Assertions.assertThat;

import com.iflytek.skillhub.auth.oauth.OAuthClaims;
import org.junit.jupiter.api.Test;

class DingTalkClaimsMapperTest {

    private final DingTalkClaimsMapper mapper = new DingTalkClaimsMapper();

    @Test
    void toClaimsShouldPreferDisplayNameOverUserIdForProviderLogin() {
        OAuthClaims claims = mapper.toClaims(
                new DingTalkUserProfile(null, "union-1", "张三", "https://avatar.example/a.png", "研发部", true),
                "corp-1"
        );

        assertThat(claims.provider()).isEqualTo("dingtalk");
        assertThat(claims.subject()).isEqualTo("union-1");
        assertThat(claims.providerLogin()).isEqualTo("张三");
    }

    @Test
    void toClaimsShouldFallBackToUserIdWhenNameMissing() {
        OAuthClaims claims = mapper.toClaims(
                new DingTalkUserProfile("userid-1", "union-2", null, null, null, false),
                null
        );

        assertThat(claims.providerLogin()).isEqualTo("userid-1");
    }

    @Test
    void toClaimsShouldFallBackToUnionIdWhenNameAndUserIdMissing() {
        OAuthClaims claims = mapper.toClaims(
                new DingTalkUserProfile(null, "union-3", "", null, null, false),
                null
        );

        assertThat(claims.providerLogin()).isEqualTo("union-3");
    }
}
