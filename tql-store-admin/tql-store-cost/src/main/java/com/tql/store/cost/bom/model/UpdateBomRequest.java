package com.tql.store.cost.bom.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.util.List;

public record UpdateBomRequest(
        @NotNull @PositiveOrZero Integer expectedVersion,
        String remark,
        @NotEmpty List<@Valid Item> items
) {
    public record Item(
            @NotNull Long materialId,
            @NotNull Long unitId,
            @NotNull @Positive BigDecimal quantity,
            Integer sortOrder
    ) {
    }
}
