package com.tql.store.cost.bom.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record BomDetailView(
        Long id,
        Long storeId,
        Long dishId,
        String status,
        Integer bomVersion,
        Integer rowVersion,
        String remark,
        LocalDateTime updatedTime,
        List<Item> items
) {
    public record Item(
            Long id,
            Long materialId,
            Long unitId,
            BigDecimal quantity,
            Integer sortOrder
    ) {
    }
}
