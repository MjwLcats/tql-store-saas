package com.tql.store.cost.bom.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.List;

public record CreateBomRequest(
        @NotNull Long storeId,
        @NotNull Long dishId,
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
