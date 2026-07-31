package com.tql.store.integration.controller;

import com.tql.store.common.api.ApiResponse;
import com.tql.store.common.api.PageResult;
import com.tql.store.integration.model.SyncLogView;
import com.tql.store.integration.model.SyncTaskCreateRequest;
import com.tql.store.integration.model.SyncTaskView;
import com.tql.store.integration.service.IntegrationPermissionService;
import com.tql.store.integration.service.SyncTaskService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/integration/sync-tasks")
public class SyncTaskController {

    private final IntegrationPermissionService permissionService;
    private final SyncTaskService syncTaskService;

    public SyncTaskController(
            IntegrationPermissionService permissionService, SyncTaskService syncTaskService) {
        this.permissionService = permissionService;
        this.syncTaskService = syncTaskService;
    }

    @GetMapping
    public ApiResponse<PageResult<SyncTaskView>> list(
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-Tenant-Id") Long tenantId,
            @RequestHeader("X-Client-Type") String clientType,
            @RequestParam(required = false) String provider,
            @RequestParam(required = false) String dataType,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) java.time.LocalDate createdStart,
            @RequestParam(required = false) java.time.LocalDate createdEnd,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        permissionService.requireSyncPermission(userId, tenantId, clientType);
        return ApiResponse.success(syncTaskService.list(
                tenantId, provider, dataType, status, createdStart, createdEnd, page, pageSize));
    }

    @PostMapping
    public ApiResponse<Long> create(
            @Valid @RequestBody SyncTaskCreateRequest request,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-Tenant-Id") Long tenantId,
            @RequestHeader("X-Client-Type") String clientType) {
        permissionService.requireSyncPermission(userId, tenantId, clientType);
        return ApiResponse.success(syncTaskService.create(request, tenantId, userId));
    }

    @PostMapping("/{id}/retry")
    public ApiResponse<Long> retry(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-Tenant-Id") Long tenantId,
            @RequestHeader("X-Client-Type") String clientType) {
        permissionService.requireSyncPermission(userId, tenantId, clientType);
        return ApiResponse.success(syncTaskService.retry(id, tenantId, userId));
    }

    @GetMapping("/{id}/logs")
    public ApiResponse<List<SyncLogView>> logs(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-Tenant-Id") Long tenantId,
            @RequestHeader("X-Client-Type") String clientType) {
        permissionService.requireSyncPermission(userId, tenantId, clientType);
        return ApiResponse.success(syncTaskService.logs(id, tenantId));
    }
}
