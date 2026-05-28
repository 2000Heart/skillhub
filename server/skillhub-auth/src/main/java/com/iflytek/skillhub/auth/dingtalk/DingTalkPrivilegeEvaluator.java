package com.iflytek.skillhub.auth.dingtalk;

import org.springframework.stereotype.Component;

@Component
public class DingTalkPrivilegeEvaluator {

    public boolean shouldPromoteToSuperAdmin(DingTalkUserProfile profile) {
        return profile.managerOrAbove();
    }
}
