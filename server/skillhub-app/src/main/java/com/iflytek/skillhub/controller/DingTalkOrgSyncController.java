package com.iflytek.skillhub.controller;

import com.iflytek.skillhub.controller.BaseApiController;
import com.iflytek.skillhub.dto.ApiResponse;
import com.iflytek.skillhub.dto.ApiResponseFactory;
import com.iflytek.skillhub.service.DingTalkOrgSyncService;
import java.util.Map;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Receives DingTalk callback events for organization changes.
 */
@RestController
@RequestMapping("/api/v1/dingtalk/events")
public class DingTalkOrgSyncController extends BaseApiController {

    private final DingTalkOrgSyncService dingTalkOrgSyncService;

    public DingTalkOrgSyncController(ApiResponseFactory responseFactory,
                                     DingTalkOrgSyncService dingTalkOrgSyncService) {
        super(responseFactory);
        this.dingTalkOrgSyncService = dingTalkOrgSyncService;
    }

    @PostMapping("/callback")
    public Map<String, String> callback(@RequestBody Map<String, Object> payload) {
        String eventType = String.valueOf(payload.getOrDefault("EventType", payload.getOrDefault("eventType", "unknown")));
        String eventId = String.valueOf(payload.getOrDefault("EventId", payload.getOrDefault("eventId", payload.hashCode())));
        if (!dingTalkOrgSyncService.markEventProcessing(eventId, eventType)) {
            return Map.of("success", "true");
        }
        try {
            String userId = String.valueOf(payload.getOrDefault("UserId", payload.getOrDefault("userId", "")));
            if (eventType.toLowerCase().contains("leave")) {
                dingTalkOrgSyncService.handleUserLeave(userId);
            } else if (!userId.isBlank()) {
                dingTalkOrgSyncService.handleUserEvent(userId);
            }
            dingTalkOrgSyncService.markEventProcessed(eventId);
            return Map.of("success", "true");
        } catch (Exception ex) {
            dingTalkOrgSyncService.markEventFailed(eventId, ex.getMessage());
            return Map.of("success", "false");
        }
    }

    @PostMapping("/challenge")
    public Map<String, String> challenge(@RequestBody Map<String, Object> payload) {
        String challenge = String.valueOf(payload.getOrDefault("challenge", ""));
        return Map.of("challenge", challenge);
    }
}
