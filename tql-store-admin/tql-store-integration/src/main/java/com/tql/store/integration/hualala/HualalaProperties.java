package com.tql.store.integration.hualala;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "integration.hualala")
public record HualalaProperties(
        String url,
        String version,
        String appKey,
        String appSecret,
        String groupId
) {
    public void validate() {
        requireText(url, "请配置哗啦啦开放平台地址");
        requireText(version, "请配置哗啦啦开放平台版本");
        requireText(appKey, "请配置哗啦啦 appKey");
        requireText(appSecret, "请通过 HUALALA_APP_SECRET 配置哗啦啦 appSecret");
        requireText(groupId, "请配置哗啦啦 groupId");
    }

    private void requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(message);
        }
    }
}
