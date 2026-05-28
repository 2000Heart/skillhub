package com.iflytek.skillhub.auth.dingtalk;

/**
 * Normalized DingTalk user profile used for SkillHub identity binding.
 */
public record DingTalkUserProfile(
    String userId,
    String unionId,
    String name,
    String avatarUrl,
    String department,
    boolean managerOrAbove
) {
}
