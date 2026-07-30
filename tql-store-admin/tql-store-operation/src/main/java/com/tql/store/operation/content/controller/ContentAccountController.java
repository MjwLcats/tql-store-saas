package com.tql.store.operation.content.controller;

import com.tql.store.common.api.ApiResponse;
import com.tql.store.operation.content.model.ContentAccountBatchRequest;
import com.tql.store.operation.content.model.ContentAccountDeleteRequest;
import com.tql.store.operation.content.model.ContentAccountSaveRequest;
import com.tql.store.operation.content.model.ContentAccountView;
import com.tql.store.operation.content.service.ContentAccountService;
import com.tql.store.operation.content.service.ContentPermissionService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/operation/content-accounts")
public class ContentAccountController {
    private final ContentAccountService service;
    private final ContentPermissionService permissionService;

    public ContentAccountController(ContentAccountService service, ContentPermissionService permissionService) {
        this.service = service;
        this.permissionService = permissionService;
    }

    @GetMapping
    public ApiResponse<List<ContentAccountView>> list(
            @RequestHeader("X-Tenant-Id") Long tenantId,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-Client-Type") String clientType) {
        permissionService.require(userId, tenantId, clientType, "merchant:content:account:query");
        return ApiResponse.success(service.list(tenantId));
    }

    @PostMapping
    public ApiResponse<Long> create(
            @RequestHeader("X-Tenant-Id") Long tenantId,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-Client-Type") String clientType,
            @Valid @RequestBody ContentAccountSaveRequest request) {
        permissionService.require(userId, tenantId, clientType, "merchant:content:account:create");
        return ApiResponse.success(service.create(tenantId, userId, request));
    }

    @PutMapping("/{id}")
    public ApiResponse<Void> update(
            @PathVariable Long id,
            @RequestHeader("X-Tenant-Id") Long tenantId,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-Client-Type") String clientType,
            @Valid @RequestBody ContentAccountSaveRequest request) {
        permissionService.require(userId, tenantId, clientType, "merchant:content:account:update");
        service.update(tenantId, userId, id, request);
        return ApiResponse.success(null);
    }

    @PostMapping("/import")
    public ApiResponse<Integer> importRecords(
            @RequestHeader("X-Tenant-Id") Long tenantId,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-Client-Type") String clientType,
            @Valid @RequestBody ContentAccountBatchRequest request) {
        permissionService.require(userId, tenantId, clientType, "merchant:content:account:import");
        return ApiResponse.success(service.importRecords(tenantId, userId, request.records()));
    }

    @PostMapping("/delete")
    public ApiResponse<Void> delete(
            @RequestHeader("X-Tenant-Id") Long tenantId,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-Client-Type") String clientType,
            @Valid @RequestBody ContentAccountDeleteRequest request) {
        permissionService.require(userId, tenantId, clientType, "merchant:content:account:delete");
        service.delete(tenantId, userId, request.ids());
        return ApiResponse.success(null);
    }
}
