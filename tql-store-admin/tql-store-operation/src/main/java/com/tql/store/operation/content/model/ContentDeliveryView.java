package com.tql.store.operation.content.model;

import java.time.LocalDateTime;

public record ContentDeliveryView(
        Long taskId,
        Long employeeId,
        String employeeNumber,
        String employeeName,
        String organizationName,
        String storeName,
        String stage,
        LocalDateTime createdTime,
        LocalDateTime deadline,
        LocalDateTime completionTime
) {
}
