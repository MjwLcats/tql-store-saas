package com.tql.store.system.model;

public record OrganizationOption(
        Long id,
        Long parentId,
        String code,
        String name,
        boolean disabled
) {
}
