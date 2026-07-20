package com.tql.store.integration.hrbutler;

public record HrPostRecord(
        String externalOrgId,
        String externalPostId,
        String externalParentId,
        String externalPositionId,
        String postName,
        String postType,
        String postTypeName,
        String rawJson
) {
}
