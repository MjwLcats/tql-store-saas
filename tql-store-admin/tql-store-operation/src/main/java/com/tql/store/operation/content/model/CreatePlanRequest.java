package com.tql.store.operation.content.model;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record CreatePlanRequest(
        @NotNull(message = "所属活动不能为空")
        Long activityId,
        @NotBlank(message = "计划名称不能为空")
        @Size(max = 100, message = "计划名称不能超过100个字符")
        String name,
        @NotBlank(message = "任务说明不能为空")
        @Size(max = 1000, message = "任务说明不能超过1000个字符")
        String taskInstruction,
        @NotBlank(message = "创作模式不能为空")
        String creationMode,
        @Min(value = 1, message = "分镜数量不能少于1个")
        @Max(value = 8, message = "分镜数量不能超过8个")
        Integer storyboardCount,
        @NotBlank(message = "前置训练策略不能为空")
        String trainingPolicy,
        @NotNull(message = "任务截止时间不能为空")
        @Future(message = "任务截止时间必须晚于当前时间")
        LocalDateTime deadline
) {
}
