package com.tql.store.cost.masterdata.controller;

import com.tql.store.common.api.ApiResponse;
import com.tql.store.cost.masterdata.model.MasterDataModels.*;
import com.tql.store.cost.masterdata.service.MasterDataService;
import com.tql.store.cost.security.CostPermissionService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cost/master-data")
public class MasterDataController {
    private final MasterDataService service;
    private final CostPermissionService permissionService;

    public MasterDataController(MasterDataService service, CostPermissionService permissionService) {
        this.service = service;
        this.permissionService = permissionService;
    }

    @GetMapping("/units")
    public ApiResponse<List<UnitView>> units(
            @RequestHeader("X-Tenant-Id") Long tenantId,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-Client-Type") String clientType) {
        require(userId, tenantId, clientType, "merchant:cost:master:query");
        return ApiResponse.success(service.units(tenantId));
    }

    @PostMapping("/units")
    public ApiResponse<Long> createUnit(
            @RequestHeader("X-Tenant-Id") Long tenantId,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-Client-Type") String clientType,
            @Valid @RequestBody CreateUnitRequest request) {
        require(userId, tenantId, clientType, "merchant:cost:master:manage");
        return ApiResponse.success(service.createUnit(tenantId, userId, request));
    }

    @GetMapping("/materials")
    public ApiResponse<List<MaterialView>> materials(
            @RequestHeader("X-Tenant-Id") Long tenantId,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-Client-Type") String clientType) {
        require(userId, tenantId, clientType, "merchant:cost:master:query");
        return ApiResponse.success(service.materials(tenantId));
    }

    @PostMapping("/materials")
    public ApiResponse<Long> createMaterial(
            @RequestHeader("X-Tenant-Id") Long tenantId,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-Client-Type") String clientType,
            @Valid @RequestBody CreateMaterialRequest request) {
        require(userId, tenantId, clientType, "merchant:cost:master:manage");
        return ApiResponse.success(service.createMaterial(tenantId, userId, request));
    }

    @GetMapping("/dishes")
    public ApiResponse<List<DishView>> dishes(
            @RequestHeader("X-Tenant-Id") Long tenantId,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-Client-Type") String clientType) {
        require(userId, tenantId, clientType, "merchant:cost:master:query");
        return ApiResponse.success(service.dishes(tenantId));
    }

    @PostMapping("/dishes")
    public ApiResponse<Long> createDish(
            @RequestHeader("X-Tenant-Id") Long tenantId,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-Client-Type") String clientType,
            @Valid @RequestBody CreateDishRequest request) {
        require(userId, tenantId, clientType, "merchant:cost:master:manage");
        return ApiResponse.success(service.createDish(tenantId, userId, request));
    }

    @GetMapping("/conversions")
    public ApiResponse<List<ConversionView>> conversions(
            @RequestHeader("X-Tenant-Id") Long tenantId,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-Client-Type") String clientType,
            @RequestParam Long materialId) {
        require(userId, tenantId, clientType, "merchant:cost:master:query");
        return ApiResponse.success(service.conversions(tenantId, materialId));
    }

    @PostMapping("/conversions")
    public ApiResponse<Long> createConversion(
            @RequestHeader("X-Tenant-Id") Long tenantId,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-Client-Type") String clientType,
            @Valid @RequestBody CreateConversionRequest request) {
        require(userId, tenantId, clientType, "merchant:cost:master:manage");
        return ApiResponse.success(service.createConversion(tenantId, userId, request));
    }

    @GetMapping("/prices")
    public ApiResponse<List<PriceView>> prices(
            @RequestHeader("X-Tenant-Id") Long tenantId,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-Client-Type") String clientType,
            @RequestParam Long storeId,
            @RequestParam Long materialId) {
        require(userId, tenantId, clientType, "merchant:cost:master:query");
        permissionService.requireStoreAccess(userId, tenantId, storeId);
        return ApiResponse.success(service.prices(tenantId, storeId, materialId));
    }

    @PostMapping("/prices")
    public ApiResponse<Long> createPrice(
            @RequestHeader("X-Tenant-Id") Long tenantId,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-Client-Type") String clientType,
            @Valid @RequestBody CreatePriceRequest request) {
        require(userId, tenantId, clientType, "merchant:cost:price:manage");
        permissionService.requireStoreAccess(userId, tenantId, request.storeId());
        return ApiResponse.success(service.createPrice(tenantId, userId, request));
    }

    private void require(Long userId, Long tenantId, String clientType, String permission) {
        permissionService.require(userId, tenantId, clientType, permission);
    }
}
