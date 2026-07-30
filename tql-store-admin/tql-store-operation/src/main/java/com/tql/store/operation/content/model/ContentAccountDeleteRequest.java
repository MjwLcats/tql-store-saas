package com.tql.store.operation.content.model;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record ContentAccountDeleteRequest(@NotEmpty List<Long> ids) {
}
