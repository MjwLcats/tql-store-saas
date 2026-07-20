package com.tql.store.integration.provider;

import java.time.LocalDate;

public record SyncExecutionContext(
        Long taskId,
        Long tenantId,
        String provider,
        String dataType,
        String syncMode,
        LocalDate rangeStart,
        LocalDate rangeEnd
) {
}
