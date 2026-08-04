package com.tql.store.operation.content.controller;

import com.tql.store.common.api.ApiResponse;
import com.tql.store.operation.content.model.CreateActivityRequest;
import com.tql.store.operation.content.model.CreatePlanRequest;
import com.tql.store.operation.content.model.ContentDeliveryView;
import com.tql.store.operation.content.model.ActivitySummaryView;
import com.tql.store.operation.content.model.PlanSummaryView;
import com.tql.store.operation.content.model.PlanPrecheckView;
import com.tql.store.operation.content.model.PlanPublishView;
import com.tql.store.operation.content.model.PlanTargetRequest;
import com.tql.store.operation.content.model.UpdateContentPlanRequest;
import com.tql.store.operation.content.model.AiStoryboardRequest;
import com.tql.store.operation.content.model.AiStoryboardScriptsView;
import com.tql.store.operation.content.model.AiTextView;
import com.tql.store.operation.content.model.AiVideoDescriptionRequest;
import com.tql.store.operation.content.service.ContentAiService;
import com.tql.store.operation.content.service.ContentPermissionService;
import com.tql.store.operation.content.service.ContentPlanService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/operation")
public class ContentPlanController {

    private final ContentPlanService contentPlanService;
    private final ContentPermissionService permissionService;
    private final ContentAiService contentAiService;

    public ContentPlanController(
            ContentPlanService contentPlanService,
            ContentPermissionService permissionService,
            ContentAiService contentAiService) {
        this.contentPlanService = contentPlanService;
        this.permissionService = permissionService;
        this.contentAiService = contentAiService;
    }

    @PostMapping("/marketing-activities")
    public ApiResponse<Long> createActivity(
            @RequestHeader("X-Tenant-Id") Long tenantId,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-Client-Type") String clientType,
            @Valid @RequestBody CreateActivityRequest request) {
        permissionService.require(
                userId, tenantId, clientType, "merchant:content:plan:create");
        return ApiResponse.success(contentPlanService.createActivity(tenantId, userId, request));
    }

    @GetMapping("/marketing-activities")
    public ApiResponse<List<ActivitySummaryView>> activities(
            @RequestHeader("X-Tenant-Id") Long tenantId,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-Client-Type") String clientType,
            @org.springframework.web.bind.annotation.RequestParam(required = false) String keyword,
            @org.springframework.web.bind.annotation.RequestParam(required = false) String status,
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "1") int page,
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "50") int pageSize) {
        permissionService.require(
                userId, tenantId, clientType, "merchant:content:view");
        return ApiResponse.success(
                contentPlanService.activities(tenantId, keyword, status, page, pageSize));
    }

    @GetMapping("/marketing-activities/{id}/content-plans")
    public ApiResponse<List<PlanSummaryView>> plans(
            @PathVariable Long id,
            @RequestHeader("X-Tenant-Id") Long tenantId,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-Client-Type") String clientType) {
        permissionService.require(
                userId, tenantId, clientType, "merchant:content:view");
        return ApiResponse.success(contentPlanService.plans(tenantId, id));
    }

    @GetMapping("/marketing-activities/{id}/employee-tasks")
    public ApiResponse<List<ContentDeliveryView>> deliveryTasks(
            @PathVariable Long id,
            @RequestHeader("X-Tenant-Id") Long tenantId,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-Client-Type") String clientType) {
        permissionService.require(
                userId, tenantId, clientType, "merchant:content:plan:delivery:view");
        return ApiResponse.success(contentPlanService.deliveryTasks(tenantId, id));
    }

    @PostMapping("/marketing-activities/{id}/terminate")
    public ApiResponse<Void> terminateActivity(
            @PathVariable Long id,
            @RequestHeader("X-Tenant-Id") Long tenantId,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-Client-Type") String clientType) {
        permissionService.require(
                userId, tenantId, clientType, "merchant:content:plan:cancel");
        contentPlanService.terminateActivity(tenantId, userId, id);
        return ApiResponse.success(null);
    }

    @PostMapping("/marketing-activities/{id}/pause")
    public ApiResponse<Void> pauseActivity(
            @PathVariable Long id,
            @RequestHeader("X-Tenant-Id") Long tenantId,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-Client-Type") String clientType) {
        permissionService.require(
                userId, tenantId, clientType, "merchant:content:plan:cancel");
        contentPlanService.pauseActivity(tenantId, userId, id);
        return ApiResponse.success(null);
    }

    @PostMapping("/marketing-activities/{id}/resume")
    public ApiResponse<Void> resumeActivity(
            @PathVariable Long id,
            @RequestHeader("X-Tenant-Id") Long tenantId,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-Client-Type") String clientType) {
        permissionService.require(
                userId, tenantId, clientType, "merchant:content:plan:cancel");
        contentPlanService.resumeActivity(tenantId, userId, id);
        return ApiResponse.success(null);
    }

    @DeleteMapping("/marketing-activities/{id}")
    public ApiResponse<Void> deleteActivity(
            @PathVariable Long id,
            @RequestHeader("X-Tenant-Id") Long tenantId,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-Client-Type") String clientType) {
        permissionService.require(
                userId, tenantId, clientType, "merchant:content:plan:cancel");
        contentPlanService.deleteActivity(tenantId, userId, id);
        return ApiResponse.success(null);
    }

    @PutMapping("/marketing-activities/{id}")
    public ApiResponse<Void> updateActivity(
            @PathVariable Long id,
            @RequestHeader("X-Tenant-Id") Long tenantId,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-Client-Type") String clientType,
            @Valid @RequestBody UpdateContentPlanRequest request) {
        permissionService.require(
                userId, tenantId, clientType, "merchant:content:plan:update");
        contentPlanService.updateActivity(tenantId, userId, id, request);
        return ApiResponse.success(null);
    }

    @PostMapping("/content-plans")
    public ApiResponse<Long> createPlan(
            @RequestHeader("X-Tenant-Id") Long tenantId,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-Client-Type") String clientType,
            @Valid @RequestBody CreatePlanRequest request) {
        permissionService.require(
                userId, tenantId, clientType, "merchant:content:plan:create");
        return ApiResponse.success(contentPlanService.createPlan(tenantId, userId, request));
    }

    @PostMapping("/content-plans/{id}/precheck")
    public ApiResponse<PlanPrecheckView> precheck(
            @PathVariable Long id,
            @RequestHeader("X-Tenant-Id") Long tenantId,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-Client-Type") String clientType,
            @Valid @RequestBody PlanTargetRequest request) {
        permissionService.require(
                userId, tenantId, clientType, "merchant:content:plan:publish");
        return ApiResponse.success(
                contentPlanService.precheck(tenantId, id, request.employeeIds()));
    }

    @PostMapping("/content-plans/{id}/publish")
    public ApiResponse<PlanPublishView> publish(
            @PathVariable Long id,
            @RequestHeader("X-Tenant-Id") Long tenantId,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-Client-Type") String clientType,
            @RequestHeader("X-Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody PlanTargetRequest request) {
        permissionService.require(
                userId, tenantId, clientType, "merchant:content:plan:publish");
        return ApiResponse.success(contentPlanService.publish(
                tenantId, userId, id, idempotencyKey, request.employeeIds()));
    }

    @PostMapping("/content-ai/video-description")
    public ApiResponse<AiTextView> generateVideoDescription(
            @RequestHeader("X-Tenant-Id") Long tenantId,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-Client-Type") String clientType,
            @Valid @RequestBody AiVideoDescriptionRequest request) {
        permissionService.require(
                userId, tenantId, clientType, "merchant:content:plan:script:generate");
        return ApiResponse.success(contentAiService.generateVideoDescription(request.prompt()));
    }

    @PostMapping("/content-ai/storyboard-scripts")
    public ApiResponse<AiStoryboardScriptsView> generateStoryboardScripts(
            @RequestHeader("X-Tenant-Id") Long tenantId,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-Client-Type") String clientType,
            @Valid @RequestBody AiStoryboardRequest request) {
        permissionService.requireAny(
                userId,
                tenantId,
                clientType,
                "merchant:content:plan:script:generate",
                "merchant:content:plan:create",
                "merchant:content:plan:update");
        return ApiResponse.success(contentAiService.generateStoryboardScripts(request.description(), request.count()));
    }

    @PostMapping("/content-ai/shooting-requirement")
    public ApiResponse<AiTextView> generateShootingRequirement(
            @RequestHeader("X-Tenant-Id") Long tenantId,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-Client-Type") String clientType,
            @Valid @RequestBody AiVideoDescriptionRequest request) {
        permissionService.requireAny(
                userId,
                tenantId,
                clientType,
                "merchant:content:plan:script:generate",
                "merchant:content:plan:create",
                "merchant:content:plan:update");
        return ApiResponse.success(contentAiService.generateShootingRequirement(request.prompt()));
    }
}
