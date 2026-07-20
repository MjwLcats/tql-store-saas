package com.tql.store.integration.provider;

import com.tql.store.integration.hrbutler.HrButlerOpenApiClient;
import com.tql.store.integration.hrbutler.HrButlerRepository;
import com.tql.store.integration.hrbutler.HrOrganizationRecord;
import com.tql.store.integration.hrbutler.HrPositionRecord;
import com.tql.store.integration.hrbutler.HrPostRecord;
import com.tql.store.integration.hrbutler.HrUserRecord;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class HrButlerSyncAdapter implements ThirdPartySyncAdapter {

    private final HrButlerOpenApiClient openApiClient;
    private final HrButlerRepository repository;

    public HrButlerSyncAdapter(HrButlerOpenApiClient openApiClient, HrButlerRepository repository) {
        this.openApiClient = openApiClient;
        this.repository = repository;
    }

    @Override
    public String provider() {
        return "HR_BUTLER";
    }

    @Override
    public SyncExecutionResult sync(SyncExecutionContext context) {
        return switch (context.dataType()) {
            case "ORGANIZATION" -> syncOrganizations(context);
            case "POSITION" -> syncPositionsAndPosts(context);
            case "USER" -> syncUsers(context);
            default -> throw new IllegalStateException("不支持的人力管家数据类型：" + context.dataType());
        };
    }

    private SyncExecutionResult syncOrganizations(SyncExecutionContext context) {
        List<HrOrganizationRecord> organizations = openApiClient.queryOrganizations();
        int saved = repository.upsertOrganizations(
                context.tenantId(), context.taskId(), context.syncMode(), organizations);
        return new SyncExecutionResult(organizations.size(), saved, organizations.size() - saved);
    }

    private SyncExecutionResult syncPositionsAndPosts(SyncExecutionContext context) {
        List<HrOrganizationRecord> organizations = openApiClient.queryOrganizations();
        List<HrPositionRecord> positions = openApiClient.queryPositions();
        List<HrPostRecord> posts = openApiClient.queryPosts(organizations);
        int total = positions.size() + posts.size();
        int saved = repository.upsertPositionsAndPosts(
                context.tenantId(), context.taskId(), context.syncMode(), positions, posts);
        return new SyncExecutionResult(total, saved, total - saved);
    }

    private SyncExecutionResult syncUsers(SyncExecutionContext context) {
        List<HrUserRecord> users = openApiClient.queryUsers();
        int saved = repository.upsertUsers(context.tenantId(), context.taskId(), context.syncMode(), users);
        return new SyncExecutionResult(users.size(), saved, users.size() - saved);
    }
}
