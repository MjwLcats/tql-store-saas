package com.tql.store.integration.hrbutler;

public record HrPositionRecord(
        String externalPositionId,
        String positionName,
        String rawJson
) {
}
