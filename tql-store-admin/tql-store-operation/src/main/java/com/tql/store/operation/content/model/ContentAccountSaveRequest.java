package com.tql.store.operation.content.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ContentAccountSaveRequest(
        @NotBlank String platform,
        @NotBlank String accountName,
        @NotBlank String platformAccountId,
        @NotBlank String accountType,
        Long organizationId,
        @NotNull Long employeeId
) {
}
