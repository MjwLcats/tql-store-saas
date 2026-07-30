package com.tql.store.cost.food.model;

import jakarta.validation.constraints.NotNull;

public record PullFoodRequest(
        @NotNull Long shopId,
        String deptCode,
        String foodCode,
        String foodName
) {
}
