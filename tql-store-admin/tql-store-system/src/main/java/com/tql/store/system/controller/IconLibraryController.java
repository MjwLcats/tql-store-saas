package com.tql.store.system.controller;

import com.tql.store.common.api.ApiResponse;
import com.tql.store.system.model.IconUpdateRequest;
import com.tql.store.system.model.IconView;
import com.tql.store.system.service.IconLibraryService;
import com.tql.store.system.service.RbacService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/system/icons")
public class IconLibraryController {
    private final IconLibraryService service; private final RbacService rbac;
    public IconLibraryController(IconLibraryService service,RbacService rbac){this.service=service;this.rbac=rbac;}
    @GetMapping public ApiResponse<List<IconView>> list(@RequestParam(required=false) String keyword,
      @RequestParam(required=false) String category,@RequestParam(required=false) Integer status,
      @RequestHeader("X-User-Id") Long uid,@RequestHeader("X-Tenant-Id") Long tid,
      @RequestHeader("X-Client-Type") String ct){require(uid,tid,ct,"view");return ApiResponse.success(service.list(keyword,category,status));}
    @PostMapping("/upload") public ApiResponse<Long> upload(@RequestParam String name,@RequestParam String code,
      @RequestParam String category,@RequestParam(required=false) Integer order,@RequestPart("file") MultipartFile file,
      @RequestHeader("X-User-Id") Long uid,@RequestHeader("X-Tenant-Id") Long tid,@RequestHeader("X-Client-Type") String ct){
      require(uid,tid,ct,"create");return ApiResponse.success(service.upload(name,code,category,order,file,uid));}
    @PutMapping("/{id}") public ApiResponse<Void> update(@PathVariable Long id,@RequestBody IconUpdateRequest body,
      @RequestHeader("X-User-Id") Long uid,@RequestHeader("X-Tenant-Id") Long tid,@RequestHeader("X-Client-Type") String ct){
      require(uid,tid,ct,"update");service.update(id,body);return ApiResponse.success(null);}
    @PutMapping("/{id}/status") public ApiResponse<Void> status(@PathVariable Long id,@RequestBody Map<String,Integer> body,
      @RequestHeader("X-User-Id") Long uid,@RequestHeader("X-Tenant-Id") Long tid,@RequestHeader("X-Client-Type") String ct){
      require(uid,tid,ct,"update");service.status(id,body.get("status"));return ApiResponse.success(null);}
    @DeleteMapping("/{id}") public ApiResponse<Void> delete(@PathVariable Long id,@RequestHeader("X-User-Id") Long uid,
      @RequestHeader("X-Tenant-Id") Long tid,@RequestHeader("X-Client-Type") String ct){
      require(uid,tid,ct,"delete");service.delete(id);return ApiResponse.success(null);}
    private void require(Long uid,Long tid,String ct,String action){
      if(!"PLATFORM".equalsIgnoreCase(ct)||tid!=0)throw new SecurityException("仅平台端可以管理图标");
      rbac.requirePermissionCode(uid,tid,ct,"platform:system:icon:"+action);}
}
