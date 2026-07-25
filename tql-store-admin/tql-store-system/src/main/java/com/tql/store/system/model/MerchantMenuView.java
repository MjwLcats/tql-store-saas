package com.tql.store.system.model;

public record MerchantMenuView(
        Long id,
        Long tenantId,
        Long parentId,
        String name,
        String type,
        String routeName,
        String path,
        String componentKey,
        String icon,
        Long iconId,
        String iconSvg,
        String permission,
        int order,
        int visible,
        int status,
        boolean systemBuiltin
) {
}
