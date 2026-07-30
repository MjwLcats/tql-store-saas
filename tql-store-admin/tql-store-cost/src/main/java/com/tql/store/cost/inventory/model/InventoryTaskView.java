package com.tql.store.cost.inventory.model;

import java.time.LocalDateTime;

public record InventoryTaskView(
        Long id,
        Long storeId,
        String taskCode,
        String taskName,
        String status,
        LocalDateTime plannedStartTime,
        LocalDateTime plannedEndTime,
        Integer version
) {
}
