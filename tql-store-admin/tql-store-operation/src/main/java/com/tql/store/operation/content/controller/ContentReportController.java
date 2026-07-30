package com.tql.store.operation.content.controller;

import com.tql.store.common.api.ApiResponse;
import com.tql.store.operation.content.model.ContentVideoPerformanceView;
import com.tql.store.operation.content.service.ContentPermissionService;
import com.tql.store.operation.content.service.ContentReportService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/operation/content-reports")
public class ContentReportController {
    private final ContentReportService service;
    private final ContentPermissionService permissionService;

    public ContentReportController(ContentReportService service, ContentPermissionService permissionService) {
        this.service = service;
        this.permissionService = permissionService;
    }

    @GetMapping("/video-performance")
    public ApiResponse<List<ContentVideoPerformanceView>> videoPerformance(
            @RequestHeader("X-Tenant-Id") Long tenantId,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-Client-Type") String clientType) {
        permissionService.require(userId, tenantId, clientType, "merchant:content:analytics:query");
        return ApiResponse.success(service.videoPerformance(tenantId));
    }
}
