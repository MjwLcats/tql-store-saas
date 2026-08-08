package com.tql.store.gateway.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tql.store.common.api.ApiResponse;
import com.tql.store.common.security.SessionUser;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Set;

@Component
public class SessionAuthenticationFilter implements GlobalFilter, Ordered {

    private static final String SESSION_PREFIX = "tql:store:session:";
    private static final Set<String> PUBLIC_PATHS = Set.of(
            "/api/auth/login",
            "/actuator/health"
    );

    private final ReactiveStringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public SessionAuthenticationFilter(ReactiveStringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        boolean publicContentAsset = HttpMethod.GET.equals(exchange.getRequest().getMethod())
                && path.startsWith("/api/operation/content-assets/");
        boolean publicOAuthCallback = path.startsWith("/api/operation/public/oauth/");
        if (PUBLIC_PATHS.contains(path) || path.startsWith("/api/auth/login")
                || publicContentAsset || publicOAuthCallback) {
            return chain.filter(removeSpoofedHeaders(exchange));
        }

        String token = resolveToken(exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION));
        if (token == null) {
            return unauthorized(exchange, "请先登录");
        }

        return redisTemplate.opsForValue().get(SESSION_PREFIX + token)
                .flatMap(json -> {
                    try {
                        SessionUser user = objectMapper.readValue(json, SessionUser.class);
                        ServerHttpRequest request = exchange.getRequest().mutate().headers(headers -> {
                            clearIdentityHeaders(headers);
                            headers.set("X-User-Id", String.valueOf(user.userId()));
                            headers.set("X-Tenant-Id", String.valueOf(user.tenantId()));
                            headers.set("X-Username", user.username());
                            headers.set("X-Display-Name", user.displayName());
                            headers.set("X-Client-Type", user.clientType());
                            if (user.primaryStoreId() != null) {
                                headers.set("X-Store-Id", String.valueOf(user.primaryStoreId()));
                            }
                            headers.set("X-Data-Scope",
                                    user.dataScope() == null ? "SELF" : user.dataScope());
                        }).build();
                        return chain.filter(exchange.mutate().request(request).build());
                    } catch (Exception ex) {
                        return unauthorized(exchange, "登录状态无效");
                    }
                })
                .switchIfEmpty(unauthorized(exchange, "登录已过期"));
    }

    private ServerWebExchange removeSpoofedHeaders(ServerWebExchange exchange) {
        ServerHttpRequest request = exchange.getRequest().mutate()
                .headers(SessionAuthenticationFilter::clearIdentityHeaders)
                .build();
        return exchange.mutate().request(request).build();
    }

    private static void clearIdentityHeaders(HttpHeaders headers) {
        headers.remove("X-User-Id");
        headers.remove("X-Tenant-Id");
        headers.remove("X-Username");
        headers.remove("X-Display-Name");
        headers.remove("X-Store-Id");
        headers.remove("X-Data-Scope");
    }

    private String resolveToken(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return null;
        }
        String token = authorization.substring(7).trim();
        return token.isEmpty() ? null : token;
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
        try {
            byte[] bytes = objectMapper.writeValueAsBytes(ApiResponse.failure(401, message));
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
            DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
            return exchange.getResponse().writeWith(Mono.just(buffer));
        } catch (Exception ex) {
            byte[] bytes = "{\"code\":401,\"message\":\"Unauthorized\"}"
                    .getBytes(StandardCharsets.UTF_8);
            DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
            return exchange.getResponse().writeWith(Mono.just(buffer));
        }
    }

    @Override
    public int getOrder() {
        return -100;
    }
}
