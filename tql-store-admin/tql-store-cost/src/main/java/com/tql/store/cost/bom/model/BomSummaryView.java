package com.tql.store.cost.bom.model;

import java.time.LocalDateTime;

public record BomSummaryView(
        Long id,
        Long storeId,
        Long dishId,
        String status,
        Integer currentVersion,
        Integer rowVersion,
        LocalDateTime updatedTime
) {
}
