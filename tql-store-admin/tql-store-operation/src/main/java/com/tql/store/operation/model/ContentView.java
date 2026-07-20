package com.tql.store.operation.model;

import java.time.LocalDateTime;

public record ContentView(
        Long id,
        Long tenantId,
        Long storeId,
        String storeName,
        String title,
        String category,
        String status,
        String owner,
        LocalDateTime publishTime
) {
}
