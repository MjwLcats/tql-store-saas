package com.tql.store.cost.inventory.controller;

import com.tql.store.common.api.ApiResponse;
import com.tql.store.cost.inventory.model.CreateInventoryTaskRequest;
import com.tql.store.cost.inventory.model.InventoryTaskView;
import com.tql.store.cost.inventory.model.InventoryCountModels.CountItemView;
import com.tql.store.cost.inventory.model.InventoryCountModels.SubmitCountsRequest;
import com.tql.store.cost.inventory.model.InventoryCountModels.ReviewRequest;
import com.tql.store.cost.inventory.service.InventoryTaskService;
import com.tql.store.cost.security.CostPermissionService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cost/inventory-tasks")
public class InventoryTaskController {
    private final InventoryTaskService service;
    private final CostPermissionService permissionService;

    public InventoryTaskController(InventoryTaskService service, CostPermissionService permissionService) {
        this.service = service;
        this.permissionService = permissionService;
    }

    @GetMapping
    public ApiResponse<List<InventoryTaskView>> list(
            @RequestHeader("X-Tenant-Id") Long tenantId,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-Client-Type") String clientType,
            @RequestParam Long storeId) {
        permissionService.require(userId, tenantId, clientType, "merchant:cost:inventory:query");
        permissionService.requireStoreAccess(userId, tenantId, storeId);
        return ApiResponse.success(service.list(tenantId, storeId));
    }

    @PostMapping
    public ApiResponse<Long> create(
            @RequestHeader("X-Tenant-Id") Long tenantId,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-Client-Type") String clientType,
            @Valid @RequestBody CreateInventoryTaskRequest request) {
        permissionService.require(userId, tenantId, clientType, "merchant:cost:inventory:create");
        permissionService.requireStoreAccess(userId, tenantId, request.storeId());
        return ApiResponse.success(service.create(tenantId, userId, request));
    }

    @PostMapping("/{taskId}/start")
    public ApiResponse<List<CountItemView>> start(
            @RequestHeader("X-Tenant-Id") Long tenantId,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-Client-Type") String clientType,
            @PathVariable Long taskId) {
        permissionService.require(userId, tenantId, clientType, "merchant:cost:inventory:count");
        Long storeId = service.storeId(tenantId, taskId);
        permissionService.requireStoreAccess(userId, tenantId, storeId);
        return ApiResponse.success(service.start(tenantId, userId, taskId));
    }

    @GetMapping("/{taskId}/items")
    public ApiResponse<List<CountItemView>> items(
            @RequestHeader("X-Tenant-Id") Long tenantId,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-Client-Type") String clientType,
            @PathVariable Long taskId) {
        permissionService.require(userId, tenantId, clientType, "merchant:cost:inventory:query");
        Long storeId = service.storeId(tenantId, taskId);
        permissionService.requireStoreAccess(userId, tenantId, storeId);
        return ApiResponse.success(service.items(tenantId, userId, taskId));
    }

    @PostMapping("/{taskId}/submit")
    public ApiResponse<Void> submit(
            @RequestHeader("X-Tenant-Id") Long tenantId,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-Client-Type") String clientType,
            @PathVariable Long taskId,
            @Valid @RequestBody SubmitCountsRequest request) {
        permissionService.require(userId, tenantId, clientType, "merchant:cost:inventory:count");
        Long storeId = service.storeId(tenantId, taskId);
        permissionService.requireStoreAccess(userId, tenantId, storeId);
        service.submit(tenantId, storeId, userId, taskId, request);
        return ApiResponse.success(null);
    }

    @PostMapping("/{taskId}/approve")
    public ApiResponse<Void> approve(
            @RequestHeader("X-Tenant-Id") Long tenantId,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-Client-Type") String clientType,
            @PathVariable Long taskId,
            @Valid @RequestBody ReviewRequest request) {
        permissionService.require(userId, tenantId, clientType, "merchant:cost:inventory:review");
        permissionService.requireStoreAccess(userId, tenantId, service.storeId(tenantId, taskId));
        service.review(tenantId, userId, taskId, request.expectedVersion(), true, request.remark());
        return ApiResponse.success(null);
    }

    @PostMapping("/{taskId}/reject")
    public ApiResponse<Void> reject(
            @RequestHeader("X-Tenant-Id") Long tenantId,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-Client-Type") String clientType,
            @PathVariable Long taskId,
            @Valid @RequestBody ReviewRequest request) {
        permissionService.require(userId, tenantId, clientType, "merchant:cost:inventory:review");
        permissionService.requireStoreAccess(userId, tenantId, service.storeId(tenantId, taskId));
        service.review(tenantId, userId, taskId, request.expectedVersion(), false, request.remark());
        return ApiResponse.success(null);
    }

    @PostMapping("/{taskId}/close")
    public ApiResponse<Void> close(
            @RequestHeader("X-Tenant-Id") Long tenantId,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-Client-Type") String clientType,
            @PathVariable Long taskId,
            @Valid @RequestBody ReviewRequest request) {
        permissionService.require(userId, tenantId, clientType, "merchant:cost:inventory:close");
        permissionService.requireStoreAccess(userId, tenantId, service.storeId(tenantId, taskId));
        service.close(tenantId, userId, taskId, request.expectedVersion());
        return ApiResponse.success(null);
    }
}
