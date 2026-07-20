package com.tql.store.integration.hrbutler;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "integration.hr-butler")
public record HrButlerProperties(
        String url,
        String corpName,
        String appId,
        String appSecret,
        Integer postConcurrency,
        Integer maxAttempts
) {
    public void validate() {
        requireText(url, "请配置人力管家接口地址");
        requireText(corpName, "请配置人力管家 corpName");
        requireText(appId, "请配置人力管家 appId");
        requireText(appSecret, "请通过 HR_BUTLER_APP_SECRET 配置人力管家 appSecret");
    }

    public int safePostConcurrency() {
        return Math.min(Math.max(postConcurrency == null ? 8 : postConcurrency, 1), 16);
    }

    public int safeMaxAttempts() {
        return Math.min(Math.max(maxAttempts == null ? 3 : maxAttempts, 1), 5);
    }

    private void requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(message);
        }
    }
}
