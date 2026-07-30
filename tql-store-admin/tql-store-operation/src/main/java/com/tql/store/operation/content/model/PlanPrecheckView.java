package com.tql.store.operation.content.model;

import java.util.List;

public record PlanPrecheckView(
        int requestedCount,
        int eligibleCount,
        int duplicateCount,
        int unavailableCount,
        List<TargetFailure> failures
) {
    public record TargetFailure(Long employeeId, String code, String message) {
    }
}
