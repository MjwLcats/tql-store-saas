package com.tql.store.cost.food.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.tql.store.common.api.ApiResponse;
import com.tql.store.cost.food.model.FoodView;
import com.tql.store.cost.food.model.PullFoodRequest;
import com.tql.store.cost.food.service.FoodService;
import com.tql.store.cost.security.CostPermissionService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/cost/foods")
public class FoodController {
    private final FoodService service;
    private final CostPermissionService permissionService;

    public FoodController(FoodService service, CostPermissionService permissionService) {
        this.service = service;
        this.permissionService = permissionService;
    }

    @GetMapping("/sync-candidates")
    public ApiResponse<Map<String, Object>> syncCandidates(
            @RequestHeader("X-Tenant-Id") Long tenantId,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-Client-Type") String clientType,
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam Long shopId,
            @RequestParam(required = false) String foodCode,
            @RequestParam(required = false) String foodName,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        permissionService.require(userId, tenantId, clientType, "merchant:cost:master:manage");
        return ApiResponse.success(service.syncCandidates(shopId, foodCode, foodName, pageNum, pageSize, authorization));
    }

    @GetMapping("/source-shops")
    public ApiResponse<JsonNode> sourceShops(
            @RequestHeader("X-Tenant-Id") Long tenantId,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-Client-Type") String clientType,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        permissionService.require(userId, tenantId, clientType, "merchant:cost:master:manage");
        return ApiResponse.success(service.sourceShops(authorization));
    }

    @PostMapping("/sync-selected")
    public ApiResponse<Integer> syncSelected(
            @RequestHeader("X-Tenant-Id") Long tenantId,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-Client-Type") String clientType,
            @RequestBody List<JsonNode> rows) {
        permissionService.require(userId, tenantId, clientType, "merchant:cost:master:manage");
        return ApiResponse.success(service.saveSelected(tenantId, rows));
    }

    @PostMapping("/sync-all")
    public ApiResponse<Integer> syncAll(
            @RequestHeader("X-Tenant-Id") Long tenantId,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-Client-Type") String clientType,
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody Map<String, Object> request) {
        permissionService.require(userId, tenantId, clientType, "merchant:cost:master:manage");
        Long shopId = Long.valueOf(String.valueOf(request.get("shopId")));
        String foodCode = request.get("foodCode") == null ? null : String.valueOf(request.get("foodCode"));
        String foodName = request.get("foodName") == null ? null : String.valueOf(request.get("foodName"));
        return ApiResponse.success(service.saveAll(tenantId, shopId, foodCode, foodName, authorization));
    }

    @GetMapping
    public ApiResponse<List<FoodView>> list(
            @RequestHeader("X-Tenant-Id") Long tenantId,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-Client-Type") String clientType,
            @RequestParam Long shopId,
            @RequestParam(required = false) String keyword) {
        permissionService.require(userId, tenantId, clientType, "merchant:cost:bom:query");
        return ApiResponse.success(service.list(tenantId, shopId, keyword));
    }

    @PostMapping("/pull")
    public ApiResponse<Integer> pull(
            @RequestHeader("X-Tenant-Id") Long tenantId,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-Client-Type") String clientType,
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @Valid @RequestBody PullFoodRequest request) {
        permissionService.require(userId, tenantId, clientType, "merchant:cost:master:manage");
        return ApiResponse.success(service.pull(tenantId, request, authorization));
    }
}
