package com.iflytek.skillhub.dto;

import jakarta.validation.constraints.NotBlank;

public record DingTalkLoginRequest(
    @NotBlank String code,
    String corpId
) {
}
