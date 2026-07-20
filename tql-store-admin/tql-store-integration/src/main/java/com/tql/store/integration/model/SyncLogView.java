package com.tql.store.integration.model;

import java.time.LocalDateTime;

public record SyncLogView(
        Long id,
        String logLevel,
        String stage,
        String message,
        String detail,
        LocalDateTime createTime
) {
}
