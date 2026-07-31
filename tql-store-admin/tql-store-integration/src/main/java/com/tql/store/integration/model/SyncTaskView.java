package com.tql.store.integration.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record SyncTaskView(
        Long id,
        String provider,
        String dataType,
        String syncMode,
        String triggerType,
        Long retryOf,
        LocalDate rangeStart,
        LocalDate rangeEnd,
        String status,
        int totalCount,
        int successCount,
        int failedCount,
        String errorMessage,
        Long createdBy,
        String creatorName,
        LocalDateTime startedAt,
        LocalDateTime finishedAt,
        Long durationMs,
        LocalDateTime createTime
) {
}
