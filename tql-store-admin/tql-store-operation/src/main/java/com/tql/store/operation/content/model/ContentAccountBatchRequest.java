package com.tql.store.operation.content.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record ContentAccountBatchRequest(
        @NotEmpty List<@Valid ContentAccountSaveRequest> records
) {
}
