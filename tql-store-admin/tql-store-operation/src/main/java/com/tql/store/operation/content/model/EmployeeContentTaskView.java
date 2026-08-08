package com.tql.store.operation.content.model;

import java.time.LocalDateTime;

public record EmployeeContentTaskView(
        Long id,
        String activityName,
        String planName,
        String taskInstruction,
        String creationMode,
        int storyboardCount,
        String planStatus,
        String planStatusLabel,
        String stage,
        String stageLabel,
        String actionHint,
        String category,
        LocalDateTime deadline,
        LocalDateTime completionTime,
        LocalDateTime createdTime
) {
}
