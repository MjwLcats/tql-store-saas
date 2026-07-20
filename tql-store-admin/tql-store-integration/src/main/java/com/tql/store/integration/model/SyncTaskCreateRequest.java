package com.tql.store.integration.model;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;

public record SyncTaskCreateRequest(
        @NotBlank(message = "请选择数据来源") String provider,
        @NotBlank(message = "请选择数据类型") String dataType,
        @NotBlank(message = "请选择同步方式") String syncMode,
        LocalDate rangeStart,
        LocalDate rangeEnd
) {
}
