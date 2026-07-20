package com.tql.store.integration.provider;

public interface ThirdPartySyncAdapter {
    String provider();

    SyncExecutionResult sync(SyncExecutionContext context);
}
