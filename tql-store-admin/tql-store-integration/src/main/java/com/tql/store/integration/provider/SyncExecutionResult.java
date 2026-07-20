package com.tql.store.integration.provider;

public record SyncExecutionResult(int totalCount, int successCount, int failedCount) {
}
