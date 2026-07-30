package com.tql.store.operation.content.model;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record CreateActivityRequest(
        @NotBlank(message = "活动名称不能为空")
        @Size(max = 100, message = "活动名称不能超过100个字符")
        String name,
        @Size(max = 500, message = "活动目标不能超过500个字符")
        String objective,
        @NotNull(message = "活动开始时间不能为空")
        LocalDateTime startTime,
        @NotNull(message = "活动结束时间不能为空")
        @Future(message = "活动结束时间必须晚于当前时间")
        LocalDateTime endTime,
        Long ownerId
) {
}
