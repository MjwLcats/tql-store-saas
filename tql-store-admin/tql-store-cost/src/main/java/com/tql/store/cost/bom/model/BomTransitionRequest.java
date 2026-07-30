package com.tql.store.cost.bom.model;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record BomTransitionRequest(
        @NotNull @PositiveOrZero Integer expectedVersion,
        String remark
) {
}
