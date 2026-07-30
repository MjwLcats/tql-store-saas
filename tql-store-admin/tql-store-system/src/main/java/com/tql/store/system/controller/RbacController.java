package com.tql.store.system.controller;

import com.tql.store.common.api.ApiResponse;
import com.tql.store.common.api.PageResult;
import com.tql.store.system.model.MenuView;
import com.tql.store.system.model.OrganizationOption;
import com.tql.store.system.model.RoleSaveRequest;
import com.tql.store.system.model.RoleView;
import com.tql.store.system.model.StoreOption;
import com.tql.store.system.model.UserDetail;
import com.tql.store.system.model.UserSaveRequest;
import com.tql.store.system.model.UserView;
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

@RestController
@RequestMapping("/api/system")
public class RbacController {

    private final RbacService rbacService;

    public RbacController(RbacService rbacService) {
        this.rbacService = rbacService;
    }

    @GetMapping("/users")
    public ApiResponse<PageResult<UserView>> users(
            @RequestHeader("X-User-Id") Long operatorId,
            @RequestHeader("X-Tenant-Id") Long tenantId,
            @RequestHeader("X-Client-Type") String clientType,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Boolean loginEnabled,
            @RequestParam(required = false) Long storeId,
            @RequestParam(required = false) Long organizationId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        rbacService.requirePermission(operatorId, tenantId, clientType, "user");
        return ApiResponse.success(rbacService.listUsers(
                tenantId, clientType, keyword, status, loginEnabled, storeId, organizationId, page, pageSize));
    }

    @GetMapping("/users/{id}")
    public ApiResponse<UserDetail> user(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long operatorId,
            @RequestHeader("X-Tenant-Id") Long tenantId,
            @RequestHeader("X-Client-Type") String clientType) {
        rbacService.requirePermission(operatorId, tenantId, clientType, "user");
        return ApiResponse.success(rbacService.getUser(id, tenantId, clientType));
    }

    @GetMapping("/users/content-task-options")
    public ApiResponse<PageResult<UserView>> contentTaskUsers(
            @RequestHeader("X-User-Id") Long operatorId,
            @RequestHeader("X-Tenant-Id") Long tenantId,
            @RequestHeader("X-Client-Type") String clientType,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "100") int pageSize) {
        rbacService.requirePermission(
                operatorId, tenantId, clientType, "merchant:content:plan:employee:select");
        return ApiResponse.success(rbacService.listUsers(
                tenantId, clientType, null, 1, true, null, null, page, pageSize));
    }

    @GetMapping("/users/content-account-options")
    public ApiResponse<PageResult<UserView>> contentAccountUsers(
            @RequestHeader("X-User-Id") Long operatorId,
            @RequestHeader("X-Tenant-Id") Long tenantId,
            @RequestHeader("X-Client-Type") String clientType,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "100") int pageSize) {
        rbacService.requirePermission(
                operatorId, tenantId, clientType, "merchant:content:account:view");
        return ApiResponse.success(rbacService.listUsers(
                tenantId, clientType, null, 1, true, null, null, page, pageSize));
    }

    @PostMapping("/users")
    public ApiResponse<Long> createUser(
            @RequestBody UserSaveRequest request,
            @RequestHeader("X-User-Id") Long operatorId,
            @RequestHeader("X-Tenant-Id") Long tenantId,
            @RequestHeader("X-Client-Type") String clientType) {
        rbacService.requirePermission(operatorId, tenantId, clientType, "user");
        return ApiResponse.success(rbacService.createUser(request, tenantId, clientType));
    }

    @PutMapping("/users/{id}")
    public ApiResponse<Void> updateUser(
            @PathVariable Long id,
            @RequestBody UserSaveRequest request,
            @RequestHeader("X-User-Id") Long operatorId,
            @RequestHeader("X-Tenant-Id") Long tenantId,
            @RequestHeader("X-Client-Type") String clientType) {
        rbacService.requirePermission(operatorId, tenantId, clientType, "user");
        rbacService.updateUser(id, request, tenantId, clientType);
        return ApiResponse.success(null);
    }

    @DeleteMapping("/users/{id}")
    public ApiResponse<Void> deleteUser(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long operatorId,
            @RequestHeader("X-Tenant-Id") Long tenantId,
            @RequestHeader("X-Client-Type") String clientType) {
        rbacService.requirePermission(operatorId, tenantId, clientType, "user");
        rbacService.deleteUser(id, operatorId, tenantId, clientType);
        return ApiResponse.success(null);
    }

    @GetMapping("/roles")
    public ApiResponse<List<RoleView>> roles(
            @RequestHeader("X-User-Id") Long operatorId,
            @RequestHeader("X-Tenant-Id") Long tenantId,
            @RequestHeader("X-Client-Type") String clientType,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status) {
        rbacService.requirePermission(operatorId, tenantId, clientType, "role");
        return ApiResponse.success(rbacService.listRoles(tenantId, clientType, keyword, status));
    }

    @PostMapping("/roles")
    public ApiResponse<Long> createRole(
            @RequestBody RoleSaveRequest request,
            @RequestHeader("X-User-Id") Long operatorId,
            @RequestHeader("X-Tenant-Id") Long tenantId,
            @RequestHeader("X-Client-Type") String clientType) {
        rbacService.requirePermission(operatorId, tenantId, clientType, "role");
        return ApiResponse.success(rbacService.createRole(request, tenantId, clientType));
    }

    @PutMapping("/roles/{id}")
    public ApiResponse<Void> updateRole(
            @PathVariable Long id,
            @RequestBody RoleSaveRequest request,
            @RequestHeader("X-User-Id") Long operatorId,
            @RequestHeader("X-Tenant-Id") Long tenantId,
            @RequestHeader("X-Client-Type") String clientType) {
        rbacService.requirePermission(operatorId, tenantId, clientType, "role");
        rbacService.updateRole(id, request, tenantId, clientType);
        return ApiResponse.success(null);
    }

    @DeleteMapping("/roles/{id}")
    public ApiResponse<Void> deleteRole(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long operatorId,
            @RequestHeader("X-Tenant-Id") Long tenantId,
            @RequestHeader("X-Client-Type") String clientType) {
        rbacService.requirePermission(operatorId, tenantId, clientType, "role");
        rbacService.deleteRole(id, tenantId, clientType);
        return ApiResponse.success(null);
    }

    @GetMapping("/stores")
    public ApiResponse<List<StoreOption>> stores(
            @RequestHeader("X-User-Id") Long operatorId,
            @RequestHeader("X-Tenant-Id") Long tenantId,
            @RequestHeader("X-Client-Type") String clientType) {
        rbacService.requirePermission(operatorId, tenantId, clientType, "user");
        return ApiResponse.success(rbacService.listStores(tenantId));
    }

    @GetMapping("/organizations")
    public ApiResponse<List<OrganizationOption>> organizations(
            @RequestHeader("X-User-Id") Long operatorId,
            @RequestHeader("X-Tenant-Id") Long tenantId,
            @RequestHeader("X-Client-Type") String clientType) {
        rbacService.requirePermission(operatorId, tenantId, clientType, "user");
        return ApiResponse.success(rbacService.listOrganizations(tenantId, clientType));
    }

    @GetMapping("/organizations/content-task-options")
    public ApiResponse<List<OrganizationOption>> contentTaskOrganizations(
            @RequestHeader("X-User-Id") Long operatorId,
            @RequestHeader("X-Tenant-Id") Long tenantId,
            @RequestHeader("X-Client-Type") String clientType) {
        rbacService.requirePermission(
                operatorId, tenantId, clientType, "merchant:content:plan:employee:select");
        return ApiResponse.success(rbacService.listOrganizations(tenantId, clientType));
    }

    @GetMapping("/organizations/content-account-options")
    public ApiResponse<List<OrganizationOption>> contentAccountOrganizations(
            @RequestHeader("X-User-Id") Long operatorId,
            @RequestHeader("X-Tenant-Id") Long tenantId,
            @RequestHeader("X-Client-Type") String clientType) {
        rbacService.requirePermission(
                operatorId, tenantId, clientType, "merchant:content:account:view");
        return ApiResponse.success(rbacService.listOrganizations(tenantId, clientType));
    }

    @GetMapping("/roles/menus")
    public ApiResponse<List<MenuView>> assignableMenus(
            @RequestHeader("X-User-Id") Long operatorId,
            @RequestHeader("X-Tenant-Id") Long tenantId,
            @RequestHeader("X-Client-Type") String clientType) {
        rbacService.requirePermission(operatorId, tenantId, clientType, "role");
        return ApiResponse.success(rbacService.listAssignableMenus(tenantId, clientType));
    }
}
