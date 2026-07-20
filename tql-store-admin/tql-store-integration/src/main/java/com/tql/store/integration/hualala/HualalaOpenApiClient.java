package com.tql.store.integration.hualala;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Component
public class HualalaOpenApiClient {

    private static final DateTimeFormatter SOURCE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final HualalaProperties properties;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;

    public HualalaOpenApiClient(HualalaProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.restClient = RestClient.builder().baseUrl(properties.url()).build();
    }

    public List<HualalaShopRecord> queryGroupShops() {
        properties.validate();
        long timestamp = System.currentTimeMillis();
        String requestBody = buildRequestBody();
        String signature = md5(properties.appKey() + properties.appSecret() + timestamp + requestBody);
        String responseBody = restClient.post()
                .uri("/doc/getAllShop")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .header("traceID", UUID.randomUUID().toString())
                .header("groupID", properties.groupId())
                .body(form(timestamp, signature, requestBody))
                .retrieve()
                .body(String.class);
        return parseResponse(responseBody);
    }

    private MultiValueMap<String, String> form(long timestamp, String signature, String requestBody) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("timestamp", String.valueOf(timestamp));
        form.add("appKey", properties.appKey());
        form.add("signature", signature);
        form.add("version", properties.version());
        form.add("requestBody", requestBody);
        return form;
    }

    private String buildRequestBody() {
        try {
            return objectMapper.writeValueAsString(objectMapper.createObjectNode()
                    .put("groupID", Long.parseLong(properties.groupId())));
        } catch (JsonProcessingException | NumberFormatException exception) {
            throw new IllegalStateException("哗啦啦 groupId 配置不正确", exception);
        }
    }

    private List<HualalaShopRecord> parseResponse(String responseBody) {
        if (responseBody == null || responseBody.isBlank()) {
            throw new IllegalStateException("哗啦啦门店接口返回为空");
        }
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            if (!"000".equals(text(root, "code"))) {
                throw new IllegalStateException("哗啦啦门店接口调用失败：" + text(root, "message"));
            }
            JsonNode data = root.path("data");
            JsonNode result = data.path("result");
            if (!result.isMissingNode() && !"000".equals(text(result, "code"))) {
                throw new IllegalStateException("哗啦啦门店接口业务失败：" + text(result, "message"));
            }
            JsonNode shopList = data.path("shopInfoList");
            if (!shopList.isArray()) {
                throw new IllegalStateException("哗啦啦门店接口缺少 shopInfoList");
            }
            List<HualalaShopRecord> shops = new ArrayList<>(shopList.size());
            for (JsonNode shop : shopList) {
                shops.add(toRecord(shop));
            }
            return shops;
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("哗啦啦门店接口返回的 JSON 无法解析", exception);
        }
    }

    private HualalaShopRecord toRecord(JsonNode shop) throws JsonProcessingException {
        return new HualalaShopRecord(
                text(shop, "shopID"),
                text(shop, "groupID"),
                text(shop, "orgCode"),
                text(shop, "shopName"),
                text(shop, "brandID"),
                text(shop, "brandName"),
                text(shop, "businessModel"),
                text(shop, "operationMode"),
                text(shop, "status"),
                text(shop, "shopCity"),
                text(shop, "shopCityName"),
                text(shop, "shopAddress"),
                text(shop, "shopPhone"),
                text(shop, "shopOpenTime"),
                text(shop, "imagePath"),
                decimal(shop, "mapLongitudeValueBaiDu"),
                decimal(shop, "mapLatitudeValueBaiDu"),
                text(shop, "action"),
                sourceTime(shop, "createTime"),
                sourceTime(shop, "actionTime"),
                objectMapper.writeValueAsString(shop));
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }

    private BigDecimal decimal(JsonNode node, String field) {
        String value = text(node, field);
        if (value == null || value.isBlank()) return null;
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private LocalDateTime sourceTime(JsonNode node, String field) {
        String value = text(node, field);
        if (value == null || value.isBlank()) return null;
        try {
            return LocalDateTime.parse(value, SOURCE_TIME_FORMAT);
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }

    private String md5(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("MD5").digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("当前 JDK 不支持 MD5", exception);
        }
    }
}
