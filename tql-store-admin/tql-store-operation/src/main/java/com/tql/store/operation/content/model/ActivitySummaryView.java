package com.tql.store.operation.content.model;

import java.time.LocalDateTime;

public record ActivitySummaryView(
        Long id,
        String name,
        String objective,
        LocalDateTime startTime,
        LocalDateTime releaseStartTime,
        LocalDateTime endTime,
        String status,
        Long ownerId,
        String ownerName,
        int planCount,
        int employeeCount,
        int completedCount,
        int completedVideoCount,
        int totalVideoCount,
        String creationMode,
        LocalDateTime createdTime
) {
}
