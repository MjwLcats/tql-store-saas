package com.tql.store.system.model;

public record StoreOption(
        Long id,
        Long parentId,
        String code,
        String name
) {
}
