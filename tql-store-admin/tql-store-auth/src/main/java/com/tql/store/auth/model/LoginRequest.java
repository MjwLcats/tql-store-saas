package com.tql.store.auth.model;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "请输入用户名") String username,
        @NotBlank(message = "请输入密码") String password,
        @NotBlank(message = "缺少客户端类型") String clientType,
        String merchantNo
) {
}
