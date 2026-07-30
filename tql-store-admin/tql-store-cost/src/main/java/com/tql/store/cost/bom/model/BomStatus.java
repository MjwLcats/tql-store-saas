package com.tql.store.cost.bom.model;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public enum BomStatus {
    DRAFT,
    PENDING,
    PUBLISHED,
    REJECTED,
    DISABLED;

    private static final Map<BomStatus, Set<BomStatus>> TRANSITIONS = Map.of(
            DRAFT, EnumSet.of(PENDING),
            PENDING, EnumSet.of(PUBLISHED, REJECTED),
            REJECTED, EnumSet.of(DRAFT),
            PUBLISHED, EnumSet.of(DISABLED),
            DISABLED, EnumSet.noneOf(BomStatus.class)
    );

    public void requireTransitionTo(BomStatus target) {
        if (!TRANSITIONS.get(this).contains(target)) {
            throw new IllegalArgumentException("BOM状态不允许从" + this + "变更为" + target);
        }
    }
}
