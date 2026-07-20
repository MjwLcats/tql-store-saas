package com.tql.store.integration.provider;

import com.tql.store.integration.hualala.HualalaOpenApiClient;
import com.tql.store.integration.hualala.HualalaShopRecord;
import com.tql.store.integration.hualala.HualalaShopRepository;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class HualalaSyncAdapter implements ThirdPartySyncAdapter {

    private final HualalaOpenApiClient openApiClient;
    private final HualalaShopRepository shopRepository;

    public HualalaSyncAdapter(HualalaOpenApiClient openApiClient, HualalaShopRepository shopRepository) {
        this.openApiClient = openApiClient;
        this.shopRepository = shopRepository;
    }

    @Override
    public String provider() {
        return "HUALALA";
    }

    @Override
    public SyncExecutionResult sync(SyncExecutionContext context) {
        if (!"SHOP".equals(context.dataType())) {
            throw new IllegalStateException("当前仅完成哗啦啦门店数据同步");
        }
        List<HualalaShopRecord> shops = openApiClient.queryGroupShops();
        int saved = shopRepository.upsertAll(context.tenantId(), context.taskId(), shops);
        return new SyncExecutionResult(shops.size(), saved, shops.size() - saved);
    }
}
