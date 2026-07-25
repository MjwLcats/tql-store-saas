package com.tql.store.system.controller;

import com.tql.store.common.api.ApiResponse;
import com.tql.store.system.model.MerchantMenuSaveRequest;
import com.tql.store.system.model.MerchantMenuView;
import com.tql.store.system.model.MerchantOption;
import com.tql.store.system.service.MenuManagementService;
import com.tql.store.system.service.RbacService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/system/merchant-menus")
public class MenuManagementController {

    private final MenuManagementService menuService;
    private final RbacService rbacService;

    public MenuManagementController(MenuManagementService menuService, RbacService rbacService) {
        this.menuService = menuService;
        this.rbacService = rbacService;
    }

    @GetMapping("/merchants")
    public ApiResponse<List<MerchantOption>> merchants(
            @RequestHeader("X-User-Id") Long operatorId,
            @RequestHeader("X-Tenant-Id") Long operatorTenantId,
            @RequestHeader("X-Client-Type") String clientType) {
        require(operatorId, operatorTenantId, clientType, "view");
        return ApiResponse.success(menuService.listMerchants());
    }

    @GetMapping
    public ApiResponse<List<MerchantMenuView>> menus(
            @RequestParam Long tenantId,
            @RequestHeader("X-User-Id") Long operatorId,
            @RequestHeader("X-Tenant-Id") Long operatorTenantId,
            @RequestHeader("X-Client-Type") String clientType) {
        require(operatorId, operatorTenantId, clientType, "view");
        return ApiResponse.success(menuService.list(tenantId));
    }

    @GetMapping("/{id}")
    public ApiResponse<MerchantMenuView> menu(
            @PathVariable Long id,
            @RequestParam Long tenantId,
            @RequestHeader("X-User-Id") Long operatorId,
            @RequestHeader("X-Tenant-Id") Long operatorTenantId,
            @RequestHeader("X-Client-Type") String clientType) {
        require(operatorId, operatorTenantId, clientType, "view");
        return ApiResponse.success(menuService.get(id, tenantId));
    }

    @PostMapping
    public ApiResponse<Long> create(
            @RequestBody MerchantMenuSaveRequest request,
            @RequestHeader("X-User-Id") Long operatorId,
            @RequestHeader("X-Tenant-Id") Long operatorTenantId,
            @RequestHeader("X-Client-Type") String clientType) {
        require(operatorId, operatorTenantId, clientType, "create");
        return ApiResponse.success(menuService.create(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<Void> update(
            @PathVariable Long id,
            @RequestBody MerchantMenuSaveRequest request,
            @RequestHeader("X-User-Id") Long operatorId,
            @RequestHeader("X-Tenant-Id") Long operatorTenantId,
            @RequestHeader("X-Client-Type") String clientType) {
        require(operatorId, operatorTenantId, clientType, "update");
        menuService.update(id, request);
        return ApiResponse.success(null);
    }

    @PutMapping("/{id}/status")
    public ApiResponse<Void> status(
            @PathVariable Long id,
            @RequestBody Map<String, Integer> body,
            @RequestParam Long tenantId,
            @RequestHeader("X-User-Id") Long operatorId,
            @RequestHeader("X-Tenant-Id") Long operatorTenantId,
            @RequestHeader("X-Client-Type") String clientType) {
        require(operatorId, operatorTenantId, clientType, "update");
        menuService.updateStatus(id, tenantId, body.get("status"));
        return ApiResponse.success(null);
    }

    @PutMapping("/{id}/visibility")
    public ApiResponse<Void> visibility(
            @PathVariable Long id,
            @RequestBody Map<String, Integer> body,
            @RequestParam Long tenantId,
            @RequestHeader("X-User-Id") Long operatorId,
            @RequestHeader("X-Tenant-Id") Long operatorTenantId,
            @RequestHeader("X-Client-Type") String clientType) {
        require(operatorId, operatorTenantId, clientType, "update");
        menuService.updateVisibility(id, tenantId, body.get("visible"));
        return ApiResponse.success(null);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(
            @PathVariable Long id,
            @RequestParam Long tenantId,
            @RequestHeader("X-User-Id") Long operatorId,
            @RequestHeader("X-Tenant-Id") Long operatorTenantId,
            @RequestHeader("X-Client-Type") String clientType) {
        require(operatorId, operatorTenantId, clientType, "delete");
        menuService.delete(id, tenantId);
        return ApiResponse.success(null);
    }

    private void require(Long operatorId, Long tenantId, String clientType, String action) {
        if (!"PLATFORM".equalsIgnoreCase(clientType) || tenantId != 0) {
            throw new SecurityException("仅平台端可以管理商家菜单");
        }
        rbacService.requirePermissionCode(
                operatorId, tenantId, clientType, "platform:system:menu:" + action);
    }
}
