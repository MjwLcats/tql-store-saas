package com.tql.store.system.model;

public record MerchantMenuSaveRequest(
        Long tenantId,
        Long parentId,
        String name,
        String type,
        String routeName,
        String path,
        String componentKey,
        String icon,
        Long iconId,
        String permission,
        Integer order,
        Integer visible,
        Integer status
) {
}
