package com.tql.store.cost.bom.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BomStatusTest {

    @Test
    void supportsTheReviewAndPublishFlow() {
        assertDoesNotThrow(() -> BomStatus.DRAFT.requireTransitionTo(BomStatus.PENDING));
        assertDoesNotThrow(() -> BomStatus.PENDING.requireTransitionTo(BomStatus.REJECTED));
        assertDoesNotThrow(() -> BomStatus.REJECTED.requireTransitionTo(BomStatus.DRAFT));
        assertDoesNotThrow(() -> BomStatus.PENDING.requireTransitionTo(BomStatus.PUBLISHED));
        assertDoesNotThrow(() -> BomStatus.PUBLISHED.requireTransitionTo(BomStatus.DISABLED));
    }

    @Test
    void preventsSkippingReviewOrReopeningAClosedVersion() {
        assertThrows(
                IllegalArgumentException.class,
                () -> BomStatus.DRAFT.requireTransitionTo(BomStatus.PUBLISHED));
        assertThrows(
                IllegalArgumentException.class,
                () -> BomStatus.PUBLISHED.requireTransitionTo(BomStatus.DRAFT));
        assertThrows(
                IllegalArgumentException.class,
                () -> BomStatus.DISABLED.requireTransitionTo(BomStatus.DRAFT));
    }
}
