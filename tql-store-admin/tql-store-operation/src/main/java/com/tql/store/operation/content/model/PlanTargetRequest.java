package com.tql.store.operation.content.model;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record PlanTargetRequest(
        @NotEmpty(message = "至少选择一名员工")
        List<Long> employeeIds
) {
}
