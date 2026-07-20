package com.tql.store.integration.hrbutler;

import java.time.LocalDateTime;

public record HrOrganizationRecord(
        String externalOrgId,
        String externalParentId,
        String orgCode,
        String orgName,
        String orgType,
        String storeCode,
        String leadUserId,
        String leadName,
        boolean valid,
        String brandId,
        String brandName,
        String province,
        String city,
        String district,
        String orgAddress,
        String costOrgCode,
        String corporationCode,
        String corporationName,
        String superLeadUserId,
        LocalDateTime sourceCreateTime,
        LocalDateTime sourceUpdateTime,
        String rawJson
) {
}
