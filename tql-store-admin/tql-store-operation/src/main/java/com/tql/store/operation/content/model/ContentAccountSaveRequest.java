package com.tql.store.operation.content.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ContentAccountSaveRequest(
        @NotBlank String platform,
        @NotBlank String accountName,
        @NotBlank String platformAccountId,
        @Size(max = 500) @Pattern(regexp = "^$|https?://\\S+$", message = "平台主页地址必须以http://或https://开头") String platformHomepageUrl,
        @NotBlank String accountType,
        Long organizationId,
        @NotNull Long employeeId
) {
}
