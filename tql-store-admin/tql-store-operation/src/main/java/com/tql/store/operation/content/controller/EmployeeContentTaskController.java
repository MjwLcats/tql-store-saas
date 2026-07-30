package com.tql.store.operation.content.controller;

import com.tql.store.common.api.ApiResponse;
import com.tql.store.operation.content.model.EmployeeContentTaskView;
import com.tql.store.operation.content.service.ContentPlanService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/operation/employee/content-tasks")
public class EmployeeContentTaskController {

    private final ContentPlanService contentPlanService;

    public EmployeeContentTaskController(ContentPlanService contentPlanService) {
        this.contentPlanService = contentPlanService;
    }

    @GetMapping
    public ApiResponse<List<EmployeeContentTaskView>> list(
            @RequestHeader("X-Tenant-Id") Long tenantId,
            @RequestHeader("X-User-Id") Long employeeId,
            @RequestHeader("X-Client-Type") String clientType,
            @RequestParam(defaultValue = "ALL") String category,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        requireMerchant(clientType);
        return ApiResponse.success(
                contentPlanService.employeeTasks(
                        tenantId, employeeId, category, page, pageSize));
    }

    @GetMapping("/{id}")
    public ApiResponse<EmployeeContentTaskView> detail(
            @PathVariable Long id,
            @RequestHeader("X-Tenant-Id") Long tenantId,
            @RequestHeader("X-User-Id") Long employeeId,
            @RequestHeader("X-Client-Type") String clientType) {
        requireMerchant(clientType);
        return ApiResponse.success(
                contentPlanService.employeeTask(tenantId, employeeId, id));
    }

    private void requireMerchant(String clientType) {
        if (!"MERCHANT".equalsIgnoreCase(clientType)) {
            throw new SecurityException("当前端不允许访问员工任务");
        }
    }
}
