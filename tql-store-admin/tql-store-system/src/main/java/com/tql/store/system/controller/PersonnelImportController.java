package com.tql.store.system.controller;

import com.tql.store.common.api.ApiResponse;
import com.tql.store.system.model.PersonnelImportResult;
import com.tql.store.system.service.PersonnelImportService;
import com.tql.store.system.service.RbacService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/system/content-personnel-import")
public class PersonnelImportController {
    private final PersonnelImportService service;
    private final RbacService rbacService;

    public PersonnelImportController(PersonnelImportService service, RbacService rbacService) {
        this.service = service;
        this.rbacService = rbacService;
    }

    @GetMapping("/template")
    public ResponseEntity<byte[]> template(
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-Tenant-Id") Long tenantId,
            @RequestHeader("X-Client-Type") String clientType) {
        rbacService.requirePermissionCode(userId, tenantId, clientType, "merchant:content:plan:employee:import");
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename("人员导入模板.xlsx", StandardCharsets.UTF_8).build().toString())
                .body(service.template());
    }

    @PostMapping(value = "/validate", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<List<PersonnelImportResult>> validate(
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-Tenant-Id") Long tenantId,
            @RequestHeader("X-Client-Type") String clientType,
            @RequestPart("file") MultipartFile file) {
        rbacService.requirePermissionCode(userId, tenantId, clientType, "merchant:content:plan:employee:validate");
        return ApiResponse.success(service.validate(tenantId, file));
    }
}
