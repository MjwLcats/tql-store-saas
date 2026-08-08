package com.tql.store.operation.content.controller;

import com.tql.store.common.api.ApiResponse;
import com.tql.store.operation.content.model.ContentAssetView;
import com.tql.store.operation.content.service.ContentAssetService;
import com.tql.store.operation.content.service.ContentPermissionService;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/operation/content-assets")
public class ContentAssetController {

    private final ContentAssetService contentAssetService;
    private final ContentPermissionService permissionService;

    public ContentAssetController(
            ContentAssetService contentAssetService,
            ContentPermissionService permissionService) {
        this.contentAssetService = contentAssetService;
        this.permissionService = permissionService;
    }

    @PostMapping("/sample-videos")
    public ApiResponse<ContentAssetView> uploadSampleVideo(
            @RequestHeader("X-Tenant-Id") Long tenantId,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-Client-Type") String clientType,
            @RequestParam("file") MultipartFile file) {
        permissionService.require(userId, tenantId, clientType, "merchant:content:view");
        return ApiResponse.success(contentAssetService.uploadSampleVideo(tenantId, file));
    }

    @GetMapping("/{tenantId}/{fileName}")
    public ResponseEntity<Resource> sampleVideo(
            @PathVariable Long tenantId,
            @PathVariable String fileName) {
        Resource resource = contentAssetService.loadSampleVideo(tenantId, fileName);
        return ResponseEntity.ok()
                .contentType(MediaType.valueOf("video/mp4"))
                .cacheControl(CacheControl.maxAge(7, TimeUnit.DAYS).cachePublic())
                .body(resource);
    }

    @PostMapping("/sample-covers")
    public ApiResponse<ContentAssetView> uploadSampleCover(
            @RequestHeader("X-Tenant-Id") Long tenantId,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-Client-Type") String clientType,
            @RequestParam("file") MultipartFile file) {
        permissionService.require(userId, tenantId, clientType, "merchant:content:view");
        return ApiResponse.success(contentAssetService.uploadSampleCover(tenantId, file));
    }

    @GetMapping("/covers/{tenantId}/{fileName}")
    public ResponseEntity<Resource> sampleCover(@PathVariable Long tenantId, @PathVariable String fileName) {
        String type = fileName.endsWith(".png") ? "image/png"
                : fileName.endsWith(".webp") ? "image/webp" : "image/jpeg";
        return ResponseEntity.ok().contentType(MediaType.valueOf(type))
                .cacheControl(CacheControl.maxAge(7, TimeUnit.DAYS).cachePublic())
                .body(contentAssetService.loadSampleCover(tenantId, fileName));
    }

    @PostMapping("/bgm")
    public ApiResponse<ContentAssetView> uploadBgm(
            @RequestHeader("X-Tenant-Id") Long tenantId,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-Client-Type") String clientType,
            @RequestParam("file") MultipartFile file) {
        permissionService.requireAny(userId, tenantId, clientType,
                "merchant:content:bgm:create", "merchant:content:bgm:update");
        return ApiResponse.success(contentAssetService.uploadBgm(tenantId, file));
    }

    @GetMapping("/bgm/{tenantId}/{fileName}")
    public ResponseEntity<Resource> bgm(@PathVariable Long tenantId, @PathVariable String fileName) {
        String type = fileName.endsWith(".wav") ? "audio/wav" : fileName.endsWith(".m4a") ? "audio/mp4" : "audio/mpeg";
        return ResponseEntity.ok().contentType(MediaType.valueOf(type))
                .cacheControl(CacheControl.maxAge(7, TimeUnit.DAYS).cachePublic())
                .body(contentAssetService.loadBgm(tenantId, fileName));
    }
    @PostMapping("/materials/{materialType}")
    public ApiResponse<ContentAssetView> uploadMaterial(
            @RequestHeader("X-Tenant-Id") Long tenantId,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-Client-Type") String clientType,
            @PathVariable String materialType,
            @RequestParam("file") MultipartFile file) {
        permissionService.requireAny(userId, tenantId, clientType,
                "merchant:content:bgm:create", "merchant:content:bgm:update");
        return ApiResponse.success(contentAssetService.uploadMaterial(tenantId, materialType, file));
    }

    @GetMapping("/materials/{tenantId}/{materialType}/{fileName}")
    public ResponseEntity<Resource> material(
            @PathVariable Long tenantId,
            @PathVariable String materialType,
            @PathVariable String fileName) {
        return ResponseEntity.ok()
                .contentType(MediaType.valueOf(contentAssetService.materialMediaType(materialType, fileName)))
                .cacheControl(CacheControl.maxAge(7, TimeUnit.DAYS).cachePublic())
                .body(contentAssetService.loadMaterial(tenantId, materialType, fileName));
    }
}
