package com.tql.store.system.model;

public record MenuView(
        Long id,
        String name,
        String path,
        String componentKey,
        String icon,
        String permission,
        int order
) {
}
