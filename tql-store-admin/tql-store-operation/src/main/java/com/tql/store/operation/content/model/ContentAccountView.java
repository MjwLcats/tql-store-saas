package com.tql.store.operation.content.model;

import java.time.LocalDateTime;

public record ContentAccountView(
        Long id,
        String platform,
        String accountName,
        String platformAccountId,
        String platformHomepageUrl,
        String accountType,
        Long organizationId,
        String organizationName,
        Long employeeId,
        String employeeName,
        String employeeNumber,
        String status,
        LocalDateTime updateTime
) {
}
