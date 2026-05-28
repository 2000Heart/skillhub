package com.iflytek.skillhub.controller.admin;

import com.iflytek.skillhub.controller.BaseApiController;
import com.iflytek.skillhub.dto.ApiResponse;
import com.iflytek.skillhub.dto.ApiResponseFactory;
import com.iflytek.skillhub.service.DingTalkOrgQueryService;
import com.iflytek.skillhub.service.DingTalkOrgSyncService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin operations for DingTalk organization synchronization.
 */
@RestController
@RequestMapping("/api/v1/admin/dingtalk")
public class DingTalkOrgAdminController extends BaseApiController {

    private final DingTalkOrgSyncService dingTalkOrgSyncService;
    private final DingTalkOrgQueryService dingTalkOrgQueryService;

    public DingTalkOrgAdminController(ApiResponseFactory responseFactory,
                                      DingTalkOrgSyncService dingTalkOrgSyncService,
                                      DingTalkOrgQueryService dingTalkOrgQueryService) {
        super(responseFactory);
        this.dingTalkOrgSyncService = dingTalkOrgSyncService;
        this.dingTalkOrgQueryService = dingTalkOrgQueryService;
    }

    @GetMapping("/departments")
    @PreAuthorize("hasAnyRole('USER_ADMIN', 'SUPER_ADMIN')")
    public ApiResponse<List<DingTalkOrgQueryService.OrgDepartmentSummary>> listDepartments() {
        return ok("response.success.read", dingTalkOrgQueryService.listDepartments());
    }

    @GetMapping("/users")
    @PreAuthorize("hasAnyRole('USER_ADMIN', 'SUPER_ADMIN')")
    public ApiResponse<List<DingTalkOrgQueryService.OrgUserSummary>> listOrgUsers() {
        return ok("response.success.read", dingTalkOrgQueryService.listUsers());
    }

    @GetMapping("/sync/status")
    @PreAuthorize("hasAnyRole('USER_ADMIN', 'SUPER_ADMIN')")
    public ApiResponse<DingTalkOrgQueryService.OrgSyncStatus> syncStatus() {
        return ok("response.success.read", dingTalkOrgQueryService.getSyncStatus());
    }

    @PostMapping("/sync/full")
    @PreAuthorize("hasAnyRole('USER_ADMIN', 'SUPER_ADMIN')")
    public ApiResponse<Void> runFullSync() {
        dingTalkOrgSyncService.runFullSync();
        return ok("response.success.updated", null);
    }

    @PostMapping("/permissions/bootstrap")
    @PreAuthorize("hasAnyRole('USER_ADMIN', 'SUPER_ADMIN')")
    public ApiResponse<Integer> bootstrapPermissions(@RequestParam(defaultValue = "false") boolean dryRun) {
        return ok("response.success.updated", dingTalkOrgSyncService.initializeUninitializedUserPermissions(dryRun));
    }
}
