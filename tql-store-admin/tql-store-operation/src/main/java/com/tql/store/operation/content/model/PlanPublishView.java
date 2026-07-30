package com.tql.store.operation.content.model;

import java.util.List;

public record PlanPublishView(
        Long planId,
        int planVersionNo,
        String result,
        int createdCount,
        int failedCount,
        List<PlanPrecheckView.TargetFailure> failures
) {
}
