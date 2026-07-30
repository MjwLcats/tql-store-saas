package com.tql.store.cost.inventory.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

public final class InventoryCountModels {
    private InventoryCountModels() {
    }

    public record CountItemView(
            Long snapshotId,
            String materialCode,
            String materialName,
            String specification,
            String locationName,
            String unitName,
            BigDecimal bookQuantity,
            BigDecimal countedQuantity
    ) {
    }

    public record SubmitCountItem(
            @NotNull Long snapshotId,
            @NotNull @DecimalMin("0") BigDecimal countedQuantity
    ) {
    }

    public record SubmitCountsRequest(
            @NotEmpty List<@Valid SubmitCountItem> items,
            @NotNull String idempotencyKey
    ) {
    }

    public record ReviewRequest(@NotNull Integer expectedVersion, String remark) {
    }
}
