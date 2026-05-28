package com.iflytek.skillhub.auth.dingtalk;

import com.iflytek.skillhub.auth.exception.AuthFlowException;
import org.springframework.http.HttpStatus;

/**
 * Raised when DingTalk identity resolution fails.
 */
public class DingTalkAuthException extends AuthFlowException {

    public DingTalkAuthException(String detail) {
        super(HttpStatus.UNAUTHORIZED, "error.auth.dingtalk.failed", detail);
    }
}
