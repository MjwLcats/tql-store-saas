package com.tql.store.cost.inventory.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record CreateInventoryTaskRequest(
        @NotNull Long storeId,
        @NotBlank String taskName,
        @NotNull LocalDateTime plannedStartTime,
        @NotNull LocalDateTime plannedEndTime,
        String remark
) {
}
