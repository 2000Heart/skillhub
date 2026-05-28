package com.iflytek.skillhub.controller;

import com.iflytek.skillhub.auth.dingtalk.DingTalkLoginService;
import com.iflytek.skillhub.auth.dingtalk.DingTalkOAuthService;
import com.iflytek.skillhub.auth.rbac.PlatformPrincipal;
import com.iflytek.skillhub.dto.ApiResponse;
import com.iflytek.skillhub.dto.ApiResponseFactory;
import com.iflytek.skillhub.dto.AuthMeResponse;
import com.iflytek.skillhub.dto.DingTalkLoginRequest;
import com.iflytek.skillhub.ratelimit.RateLimit;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.io.IOException;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * DingTalk enterprise web-app authentication endpoints.
 */
@RestController
@RequestMapping("/api/v1/auth/dingtalk")
public class DingTalkAuthController extends BaseApiController {

    private final DingTalkLoginService dingTalkLoginService;
    private final DingTalkOAuthService dingTalkOAuthService;

    public DingTalkAuthController(ApiResponseFactory responseFactory,
                                  DingTalkLoginService dingTalkLoginService,
                                  DingTalkOAuthService dingTalkOAuthService) {
        super(responseFactory);
        this.dingTalkLoginService = dingTalkLoginService;
        this.dingTalkOAuthService = dingTalkOAuthService;
    }

    @PostMapping("/login")
    @RateLimit(category = "auth-dingtalk-login", authenticated = 30, anonymous = 15, windowSeconds = 60)
    public ApiResponse<AuthMeResponse> login(@Valid @RequestBody DingTalkLoginRequest request,
                                             HttpServletRequest httpRequest) {
        PlatformPrincipal principal = dingTalkLoginService.loginWithAuthCode(
                request.code(),
                request.corpId(),
                httpRequest
        );
        return ok("response.success.read", AuthMeResponse.from(principal));
    }

    @GetMapping("/oauth/authorize")
    public void authorize(@RequestParam(name = "returnTo", required = false) String returnTo,
                          HttpServletRequest request,
                          HttpServletResponse response) throws IOException {
        response.sendRedirect(dingTalkOAuthService.buildAuthorizeUrl(request, returnTo));
    }

    @GetMapping("/oauth/callback")
    public void callback(@RequestParam(name = "code", required = false) String code,
                         @RequestParam(name = "state", required = false) String state,
                         HttpServletRequest request,
                         HttpServletResponse response) throws IOException {
        if (!StringUtils.hasText(code) || !StringUtils.hasText(state)) {
            response.sendRedirect("/login?reason=dingtalkOAuthFailed");
            return;
        }
        dingTalkOAuthService.validateState(request, state);
        dingTalkLoginService.loginWithOAuthCode(code, request);
        String returnTo = dingTalkOAuthService.consumeReturnTo(request);
        response.sendRedirect(dingTalkOAuthService.buildFrontendRedirect(returnTo));
    }
}
