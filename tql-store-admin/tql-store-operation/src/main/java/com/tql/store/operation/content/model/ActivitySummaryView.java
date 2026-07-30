package com.tql.store.operation.content.model;

import java.time.LocalDateTime;

public record ActivitySummaryView(
        Long id,
        String name,
        String objective,
        LocalDateTime startTime,
        LocalDateTime endTime,
        String status,
        Long ownerId,
        String ownerName,
        int planCount,
        int employeeCount,
        int completedCount,
        LocalDateTime createdTime
) {
}
