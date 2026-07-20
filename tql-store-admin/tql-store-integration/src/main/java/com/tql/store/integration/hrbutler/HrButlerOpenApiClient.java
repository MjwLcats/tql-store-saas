package com.tql.store.integration.hrbutler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
public class HrButlerOpenApiClient {

    private static final List<DateTimeFormatter> DATE_TIME_FORMATTERS = List.of(
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
            DateTimeFormatter.ISO_LOCAL_DATE_TIME
    );

    private final HrButlerProperties properties;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;

    public HrButlerOpenApiClient(HrButlerProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.restClient = RestClient.builder().baseUrl(properties.url()).build();
    }

    public List<HrOrganizationRecord> queryOrganizations() {
        JsonNode list = resultList(invoke("/api/org/list", Map.of()), "org_list", "组织");
        List<HrOrganizationRecord> records = new ArrayList<>(list.size());
        for (JsonNode node : list) {
            String id = text(node, "org_id");
            records.add(new HrOrganizationRecord(
                    id,
                    text(node, "parent_id"),
                    firstText(node, "orgCode", "org_code"),
                    text(node, "org_name"),
                    text(node, "org_type"),
                    text(node, "storeCode"),
                    text(node, "lead_user_id"),
                    text(node, "lead_name"),
                    "1".equals(text(node, "valid")),
                    text(node, "brandId"),
                    text(node, "brandName"),
                    text(node, "province"),
                    text(node, "city"),
                    text(node, "district"),
                    text(node, "org_addr"),
                    text(node, "costOrgCode"),
                    text(node, "corporationCode"),
                    text(node, "corporationName"),
                    text(node, "super_lead_user_id"),
                    dateTime(node, "createTime"),
                    dateTime(node, "updateTime"),
                    safeJson(node, List.of(
                            "org_id", "parent_id", "orgCode", "org_name", "org_type", "storeCode",
                            "lead_user_id", "lead_name", "valid", "brandId", "brandName", "province",
                            "city", "district", "org_addr", "costOrgCode", "corporationCode",
                            "corporationName", "super_lead_user_id", "createTime", "updateTime"))));
        }
        return records;
    }

    public List<HrPositionRecord> queryPositions() {
        JsonNode list = resultList(invoke("/api/position/list", Map.of()), "position_list", "职位");
        List<HrPositionRecord> records = new ArrayList<>(list.size());
        for (JsonNode node : list) {
            records.add(new HrPositionRecord(
                    text(node, "position_id"),
                    text(node, "position_name"),
                    safeJson(node, List.of("position_id", "position_name"))));
        }
        return records;
    }

    public List<HrPostRecord> queryPosts(List<HrOrganizationRecord> organizations) {
        List<HrOrganizationRecord> validOrganizations = organizations.stream()
                .filter(HrOrganizationRecord::valid)
                .filter(item -> item.externalOrgId() != null && !item.externalOrgId().isBlank())
                .toList();
        ExecutorService executor = Executors.newFixedThreadPool(properties.safePostConcurrency());
        try {
            List<CompletableFuture<List<HrPostRecord>>> futures = validOrganizations.stream()
                    .map(org -> CompletableFuture.supplyAsync(
                            () -> queryPostsForOrganization(org.externalOrgId()), executor))
                    .toList();
            List<HrPostRecord> records = new ArrayList<>();
            for (CompletableFuture<List<HrPostRecord>> future : futures) {
                records.addAll(future.join());
            }
            return records;
        } catch (CompletionException exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new IllegalStateException("人力管家岗位数据查询失败", cause);
        } finally {
            executor.shutdownNow();
        }
    }

    public List<HrUserRecord> queryUsers() {
        JsonNode list = resultList(
                invoke("/api/user/simplelist", Map.of("queryExtField", "false")), "user_list", "用户");
        List<HrUserRecord> records = new ArrayList<>(list.size());
        for (JsonNode node : list) {
            records.add(new HrUserRecord(
                    text(node, "user_id"),
                    firstText(node, "org_id", "department"),
                    text(node, "user_num"),
                    text(node, "user_name"),
                    text(node, "user_name_py"),
                    text(node, "gender"),
                    text(node, "mobile"),
                    text(node, "email"),
                    text(node, "user_type"),
                    text(node, "user_status"),
                    text(node, "position_id"),
                    text(node, "position_name"),
                    text(node, "post_id"),
                    text(node, "post_name"),
                    text(node, "post_type_name"),
                    text(node, "rank_id"),
                    text(node, "rank_name"),
                    text(node, "lead_id"),
                    date(node, "offer_time"),
                    date(node, "start_time"),
                    safeJson(node, List.of(
                            "user_id", "org_id", "department", "user_num", "user_name", "user_name_py",
                            "gender", "mobile", "email", "user_type", "user_status", "position_id",
                            "position_name", "post_id", "post_name", "post_type_name", "rank_id",
                            "rank_name", "lead_id", "offer_time", "start_time"))));
        }
        return records;
    }

    private List<HrPostRecord> queryPostsForOrganization(String organizationId) {
        JsonNode list = resultList(
                invoke("/api/post/list", Map.of("org_id", organizationId)), "post_list", "岗位");
        List<HrPostRecord> records = new ArrayList<>(list.size());
        for (JsonNode node : list) {
            String sourceOrgId = firstText(node, "org_id");
            if (sourceOrgId == null || sourceOrgId.isBlank()) {
                sourceOrgId = organizationId;
            }
            ObjectNode safeNode = selectedNode(node, List.of(
                    "org_id", "post_id", "parent_id", "position_id", "post_name", "post_type",
                    "post_type_name"));
            safeNode.put("org_id", sourceOrgId);
            records.add(new HrPostRecord(
                    sourceOrgId,
                    text(node, "post_id"),
                    text(node, "parent_id"),
                    text(node, "position_id"),
                    text(node, "post_name"),
                    text(node, "post_type"),
                    text(node, "post_type_name"),
                    writeJson(safeNode)));
        }
        return records;
    }

    private JsonNode invoke(String path, Map<String, String> extraParameters) {
        properties.validate();
        RuntimeException lastFailure = null;
        for (int attempt = 1; attempt <= properties.safeMaxAttempts(); attempt++) {
            try {
                long timestamp = Instant.now().getEpochSecond();
                MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
                form.add("corp_name", properties.corpName());
                form.add("app_id", properties.appId());
                form.add("timestamp", String.valueOf(timestamp));
                form.add("token", sha256(properties.corpName() + properties.appId()
                        + properties.appSecret() + timestamp));
                extraParameters.forEach(form::add);
                String responseBody = restClient.post()
                        .uri(path)
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .body(form)
                        .retrieve()
                        .body(String.class);
                return parseResponse(path, responseBody);
            } catch (RuntimeException exception) {
                lastFailure = exception;
                if (attempt < properties.safeMaxAttempts()) {
                    sleep(attempt * 200L);
                }
            }
        }
        throw lastFailure == null ? new IllegalStateException("人力管家接口调用失败") : lastFailure;
    }

    private JsonNode parseResponse(String path, String responseBody) {
        if (responseBody == null || responseBody.isBlank()) {
            throw new IllegalStateException("人力管家接口返回为空：" + path);
        }
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            if (root.path("status").asInt() != 1) {
                String errorCode = text(root, "error_code");
                String message = text(root, "message");
                throw new IllegalStateException("人力管家接口调用失败[" + errorCode + "]：" + message);
            }
            return root.path("result");
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("人力管家接口返回的 JSON 无法解析：" + path, exception);
        }
    }

    private JsonNode resultList(JsonNode result, String field, String label) {
        JsonNode list = result.path(field);
        if (!list.isArray()) {
            throw new IllegalStateException("人力管家" + label + "接口缺少 " + field);
        }
        return list;
    }

    private ObjectNode selectedNode(JsonNode source, List<String> fields) {
        ObjectNode target = objectMapper.createObjectNode();
        for (String field : fields) {
            JsonNode value = source.get(field);
            if (value != null && !value.isNull()) {
                target.set(field, value);
            }
        }
        return target;
    }

    private String safeJson(JsonNode source, List<String> fields) {
        return writeJson(selectedNode(source, fields));
    }

    private String writeJson(JsonNode node) {
        try {
            return objectMapper.writeValueAsString(node);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("人力管家数据序列化失败", exception);
        }
    }

    private String firstText(JsonNode node, String... fields) {
        for (String field : fields) {
            String value = text(node, field);
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }

    private LocalDate date(JsonNode node, String field) {
        String value = text(node, field);
        if (value == null || value.isBlank() || value.length() < 10) {
            return null;
        }
        try {
            return LocalDate.parse(value.substring(0, 10));
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }

    private LocalDateTime dateTime(JsonNode node, String field) {
        String value = text(node, field);
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            long timestamp = Long.parseLong(value);
            Instant instant = value.length() > 10 ? Instant.ofEpochMilli(timestamp) : Instant.ofEpochSecond(timestamp);
            return LocalDateTime.ofInstant(instant, ZoneId.of("Asia/Shanghai"));
        } catch (NumberFormatException ignored) {
            for (DateTimeFormatter formatter : DATE_TIME_FORMATTERS) {
                try {
                    return LocalDateTime.parse(value, formatter);
                } catch (DateTimeParseException ignoredFormat) {
                    // Try the next documented time format.
                }
            }
            return null;
        }
    }

    private String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("当前 JDK 不支持 SHA-256", exception);
        }
    }

    private void sleep(long milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("人力管家接口重试被中断", exception);
        }
    }
}
