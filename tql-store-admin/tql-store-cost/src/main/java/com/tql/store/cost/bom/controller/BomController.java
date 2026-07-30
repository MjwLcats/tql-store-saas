package com.tql.store.cost.bom.controller;

import com.tql.store.common.api.ApiResponse;
import com.tql.store.cost.bom.model.*;
import com.tql.store.cost.bom.service.BomService;
import com.tql.store.cost.security.CostPermissionService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cost/boms")
public class BomController {
    private final BomService service;
    private final CostPermissionService permissionService;

    public BomController(BomService service, CostPermissionService permissionService) {
        this.service = service;
        this.permissionService = permissionService;
    }

    @GetMapping
    public ApiResponse<List<BomSummaryView>> list(
            @RequestHeader("X-Tenant-Id") Long tenantId,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-Client-Type") String clientType,
            @RequestParam Long storeId) {
        permissionService.require(userId, tenantId, clientType, "merchant:cost:bom:query");
        permissionService.requireStoreAccess(userId, tenantId, storeId);
        return ApiResponse.success(service.list(tenantId, storeId));
    }

    @PostMapping
    public ApiResponse<Long> create(
            @RequestHeader("X-Tenant-Id") Long tenantId,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-Client-Type") String clientType,
            @Valid @RequestBody CreateBomRequest request) {
        permissionService.require(userId, tenantId, clientType, "merchant:cost:bom:create");
        permissionService.requireStoreAccess(userId, tenantId, request.storeId());
        return ApiResponse.success(service.createDraft(tenantId, userId, request));
    }

    @GetMapping("/{id}")
    public ApiResponse<BomDetailView> detail(
            @PathVariable Long id,
            @RequestHeader("X-Tenant-Id") Long tenantId,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-Client-Type") String clientType) {
        permissionService.require(userId, tenantId, clientType, "merchant:cost:bom:query");
        BomDetailView detail = service.detail(tenantId, id);
        permissionService.requireStoreAccess(userId, tenantId, detail.storeId());
        return ApiResponse.success(detail);
    }

    @PutMapping("/{id}")
    public ApiResponse<Void> update(
            @PathVariable Long id,
            @RequestHeader("X-Tenant-Id") Long tenantId,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-Client-Type") String clientType,
            @Valid @RequestBody UpdateBomRequest request) {
        permissionService.require(userId, tenantId, clientType, "merchant:cost:bom:update");
        requireBomStoreAccess(tenantId, userId, id);
        service.updateDraft(tenantId, userId, id, request);
        return ApiResponse.success(null);
    }

    @PostMapping("/{id}/submit")
    public ApiResponse<Void> submit(
            @PathVariable Long id,
            @RequestHeader("X-Tenant-Id") Long tenantId,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-Client-Type") String clientType,
            @Valid @RequestBody BomTransitionRequest request) {
        permissionService.require(userId, tenantId, clientType, "merchant:cost:bom:submit");
        requireBomStoreAccess(tenantId, userId, id);
        service.submit(tenantId, userId, id, request);
        return ApiResponse.success(null);
    }

    @PostMapping("/{id}/reject")
    public ApiResponse<Void> reject(
            @PathVariable Long id,
            @RequestHeader("X-Tenant-Id") Long tenantId,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-Client-Type") String clientType,
            @Valid @RequestBody BomTransitionRequest request) {
        permissionService.require(userId, tenantId, clientType, "merchant:cost:bom:review");
        requireBomStoreAccess(tenantId, userId, id);
        service.reject(tenantId, userId, id, request);
        return ApiResponse.success(null);
    }

    @PostMapping("/{id}/publish")
    public ApiResponse<Void> publish(
            @PathVariable Long id,
            @RequestHeader("X-Tenant-Id") Long tenantId,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-Client-Type") String clientType,
            @Valid @RequestBody BomTransitionRequest request) {
        permissionService.require(userId, tenantId, clientType, "merchant:cost:bom:review");
        requireBomStoreAccess(tenantId, userId, id);
        service.publish(tenantId, userId, id, request);
        return ApiResponse.success(null);
    }

    private void requireBomStoreAccess(Long tenantId, Long userId, Long bomId) {
        permissionService.requireStoreAccess(userId, tenantId, service.detail(tenantId, bomId).storeId());
    }
}
