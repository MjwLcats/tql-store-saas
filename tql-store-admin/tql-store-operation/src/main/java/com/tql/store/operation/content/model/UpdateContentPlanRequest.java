package com.tql.store.operation.content.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

public record UpdateContentPlanRequest(
        @NotBlank @Size(max = 100) String name,
        @Size(max = 500) String objective,
        @NotNull LocalDateTime startTime,
        @NotNull LocalDateTime endTime,
        @NotBlank @Size(max = 1000) String taskInstruction,
        @NotBlank String creationMode,
        Integer storyboardCount,
        @NotBlank String trainingPolicy,
        List<Long> employeeIds
) {
}
