package com.tql.store.operation.content.model;

import java.time.LocalDateTime;

public record PlanSummaryView(
        Long id,
        Long activityId,
        String name,
        String taskInstruction,
        String creationMode,
        int storyboardCount,
        String trainingPolicy,
        LocalDateTime deadline,
        String status,
        int currentVersionNo,
        int employeeCount
) {
}
