package com.tql.store.integration.hrbutler;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public class HrButlerRepository {

    private static final String ORGANIZATION_UPSERT = """
            INSERT INTO integration_rlgj_organization
                (tenant_id, external_org_id, external_parent_id, org_code, org_name, org_type,
                 store_code, lead_user_id, lead_name, valid_status, brand_id, brand_name,
                 province, city, district, org_address, cost_org_code, corporation_code,
                 corporation_name, super_lead_user_id, source_create_time, source_update_time,
                 raw_json, last_sync_task_id, last_sync_time, deleted, create_by, update_by)
            VALUES
                (:tenantId, :externalOrgId, :externalParentId, :orgCode, :orgName, :orgType,
                 :storeCode, :leadUserId, :leadName, :validStatus, :brandId, :brandName,
                 :province, :city, :district, :orgAddress, :costOrgCode, :corporationCode,
                 :corporationName, :superLeadUserId, :sourceCreateTime, :sourceUpdateTime,
                 CAST(:rawJson AS JSON), :taskId, :syncTime, :deleted, :operatorId, :operatorId)
            ON DUPLICATE KEY UPDATE
                external_parent_id = VALUES(external_parent_id), org_code = VALUES(org_code),
                org_name = VALUES(org_name), org_type = VALUES(org_type), store_code = VALUES(store_code),
                lead_user_id = VALUES(lead_user_id), lead_name = VALUES(lead_name),
                valid_status = VALUES(valid_status), brand_id = VALUES(brand_id),
                brand_name = VALUES(brand_name), province = VALUES(province), city = VALUES(city),
                district = VALUES(district), org_address = VALUES(org_address),
                cost_org_code = VALUES(cost_org_code), corporation_code = VALUES(corporation_code),
                corporation_name = VALUES(corporation_name),
                super_lead_user_id = VALUES(super_lead_user_id),
                source_create_time = VALUES(source_create_time), source_update_time = VALUES(source_update_time),
                raw_json = VALUES(raw_json), last_sync_task_id = VALUES(last_sync_task_id),
                last_sync_time = VALUES(last_sync_time), deleted = VALUES(deleted),
                update_by = VALUES(update_by)
            """;

    private static final String MERCHANT_ORGANIZATION_UPSERT = """
            INSERT INTO sys_merchant_organization
                (tenant_id, parent_id, org_code, org_name, org_type, store_code,
                 source_type, source_id, status, sort_order, deleted, create_by, update_by)
            VALUES
                (:tenantId, 0, :merchantOrgCode, :orgName, :orgType, :storeCode,
                 'HR_BUTLER', :externalOrgId, :validStatus, 0, :deleted, :operatorId, :operatorId)
            ON DUPLICATE KEY UPDATE
                org_name = VALUES(org_name), org_type = VALUES(org_type), store_code = VALUES(store_code),
                status = VALUES(status), deleted = VALUES(deleted), update_by = VALUES(update_by)
            """;

    private static final String POSITION_UPSERT = """
            INSERT INTO integration_rlgj_position
                (tenant_id, external_position_id, position_name, raw_json,
                 last_sync_task_id, last_sync_time, deleted, create_by, update_by)
            VALUES
                (:tenantId, :externalPositionId, :positionName, CAST(:rawJson AS JSON),
                 :taskId, :syncTime, 0, :operatorId, :operatorId)
            ON DUPLICATE KEY UPDATE
                position_name = VALUES(position_name), raw_json = VALUES(raw_json),
                last_sync_task_id = VALUES(last_sync_task_id), last_sync_time = VALUES(last_sync_time),
                deleted = 0, update_by = VALUES(update_by)
            """;

    private static final String POST_UPSERT = """
            INSERT INTO integration_rlgj_post
                (tenant_id, external_org_id, external_post_id, external_parent_id,
                 external_position_id, post_name, post_type, post_type_name, raw_json,
                 last_sync_task_id, last_sync_time, deleted, create_by, update_by)
            VALUES
                (:tenantId, :externalOrgId, :externalPostId, :externalParentId,
                 :externalPositionId, :postName, :postType, :postTypeName, CAST(:rawJson AS JSON),
                 :taskId, :syncTime, 0, :operatorId, :operatorId)
            ON DUPLICATE KEY UPDATE
                external_parent_id = VALUES(external_parent_id),
                external_position_id = VALUES(external_position_id), post_name = VALUES(post_name),
                post_type = VALUES(post_type), post_type_name = VALUES(post_type_name),
                raw_json = VALUES(raw_json), last_sync_task_id = VALUES(last_sync_task_id),
                last_sync_time = VALUES(last_sync_time), deleted = 0, update_by = VALUES(update_by)
            """;

    private static final String USER_UPSERT = """
            INSERT INTO integration_rlgj_user
                (tenant_id, external_user_id, external_org_id, user_number, user_name,
                 user_name_pinyin, gender_code, mobile, email, user_type, user_status,
                 external_position_id, position_name, external_post_id, post_name, post_type_name,
                 external_rank_id, rank_name, leader_user_ids, offer_date, start_date, raw_json,
                 last_sync_task_id, last_sync_time, deleted, create_by, update_by)
            VALUES
                (:tenantId, :externalUserId, :externalOrgId, :userNumber, :userName,
                 :userNamePinyin, :genderCode, :mobile, :email, :userType, :userStatus,
                 :externalPositionId, :positionName, :externalPostId, :postName, :postTypeName,
                 :externalRankId, :rankName, :leaderUserIds, :offerDate, :startDate, CAST(:rawJson AS JSON),
                 :taskId, :syncTime, 0, :operatorId, :operatorId)
            ON DUPLICATE KEY UPDATE
                external_org_id = VALUES(external_org_id), user_number = VALUES(user_number),
                user_name = VALUES(user_name), user_name_pinyin = VALUES(user_name_pinyin),
                gender_code = VALUES(gender_code), mobile = VALUES(mobile), email = VALUES(email),
                user_type = VALUES(user_type), user_status = VALUES(user_status),
                external_position_id = VALUES(external_position_id), position_name = VALUES(position_name),
                external_post_id = VALUES(external_post_id), post_name = VALUES(post_name),
                post_type_name = VALUES(post_type_name), external_rank_id = VALUES(external_rank_id),
                rank_name = VALUES(rank_name), leader_user_ids = VALUES(leader_user_ids),
                offer_date = VALUES(offer_date), start_date = VALUES(start_date), raw_json = VALUES(raw_json),
                last_sync_task_id = VALUES(last_sync_task_id), last_sync_time = VALUES(last_sync_time),
                deleted = 0, update_by = VALUES(update_by)
            """;

    private static final String MERCHANT_USER_UPSERT = """
            INSERT INTO sys_merchant_user
                (tenant_id, organization_id, username, password_hash, employee_number,
                 display_name, name_pinyin, gender_code, email, phone,
                 position_id, position_name, post_id, post_name, source_type, source_id,
                 login_enabled, data_scope, status, deleted, create_by, update_by)
            VALUES
                (:tenantId,
                 (SELECT id FROM sys_merchant_organization
                  WHERE tenant_id = :tenantId AND source_type = 'HR_BUTLER'
                    AND source_id = :externalOrgId LIMIT 1),
                 NULL, NULL, :userNumber, :userName, :userNamePinyin, :genderCode, :email, :mobile,
                 :externalPositionId, :positionName, :externalPostId, :postName,
                 'HR_BUTLER', :externalUserId, 0, 'SELF', :activeStatus, 0, :operatorId, :operatorId)
            ON DUPLICATE KEY UPDATE
                organization_id = VALUES(organization_id), employee_number = VALUES(employee_number),
                display_name = VALUES(display_name), name_pinyin = VALUES(name_pinyin),
                gender_code = VALUES(gender_code), email = VALUES(email), phone = VALUES(phone),
                position_id = VALUES(position_id), position_name = VALUES(position_name),
                post_id = VALUES(post_id), post_name = VALUES(post_name),
                status = VALUES(status), deleted = 0, update_by = VALUES(update_by)
            """;

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public HrButlerRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public int upsertOrganizations(
            Long tenantId, Long taskId, String syncMode, List<HrOrganizationRecord> organizations) {
        if (organizations.isEmpty()) {
            return 0;
        }
        Long operatorId = operatorId(tenantId, taskId);
        LocalDateTime syncTime = LocalDateTime.now();
        MapSqlParameterSource[] parameters = organizations.stream()
                .map(item -> organizationParameters(tenantId, taskId, operatorId, syncTime, item))
                .toArray(MapSqlParameterSource[]::new);
        jdbcTemplate.batchUpdate(ORGANIZATION_UPSERT, parameters);
        jdbcTemplate.batchUpdate(MERCHANT_ORGANIZATION_UPSERT, parameters);
        jdbcTemplate.update("""
                UPDATE sys_merchant_organization child
                JOIN integration_rlgj_organization source
                  ON source.tenant_id = child.tenant_id
                 AND child.source_type = 'HR_BUTLER'
                 AND child.source_id = source.external_org_id
                LEFT JOIN sys_merchant_organization parent
                  ON parent.tenant_id = child.tenant_id
                 AND parent.source_type = 'HR_BUTLER'
                 AND parent.source_id = source.external_parent_id
                SET child.parent_id = COALESCE(parent.id, 0)
                WHERE child.tenant_id = :tenantId AND source.last_sync_task_id = :taskId
                """, taskParameters(tenantId, taskId));
        if ("FULL".equalsIgnoreCase(syncMode)) {
            jdbcTemplate.update("""
                    UPDATE integration_rlgj_organization
                    SET deleted = 1, valid_status = 0, update_by = :operatorId
                    WHERE tenant_id = :tenantId AND last_sync_task_id <> :taskId
                    """, taskParameters(tenantId, taskId).addValue("operatorId", operatorId));
            jdbcTemplate.update("""
                    UPDATE sys_merchant_organization target
                    JOIN integration_rlgj_organization source
                      ON source.tenant_id = target.tenant_id
                     AND target.source_type = 'HR_BUTLER'
                     AND target.source_id = source.external_org_id
                    SET target.status = IF(source.deleted = 1, 0, source.valid_status),
                        target.deleted = source.deleted, target.update_by = :operatorId
                    WHERE target.tenant_id = :tenantId
                    """, taskParameters(tenantId, taskId).addValue("operatorId", operatorId));
        }
        return organizations.size();
    }

    @Transactional
    public int upsertPositionsAndPosts(
            Long tenantId, Long taskId, String syncMode,
            List<HrPositionRecord> positions, List<HrPostRecord> posts) {
        Long operatorId = operatorId(tenantId, taskId);
        LocalDateTime syncTime = LocalDateTime.now();
        if (!positions.isEmpty()) {
            jdbcTemplate.batchUpdate(POSITION_UPSERT, positions.stream()
                    .map(item -> positionParameters(tenantId, taskId, operatorId, syncTime, item))
                    .toArray(MapSqlParameterSource[]::new));
        }
        if (!posts.isEmpty()) {
            jdbcTemplate.batchUpdate(POST_UPSERT, posts.stream()
                    .map(item -> postParameters(tenantId, taskId, operatorId, syncTime, item))
                    .toArray(MapSqlParameterSource[]::new));
        }
        if ("FULL".equalsIgnoreCase(syncMode)) {
            MapSqlParameterSource parameters = taskParameters(tenantId, taskId).addValue("operatorId", operatorId);
            jdbcTemplate.update("""
                    UPDATE integration_rlgj_position SET deleted = 1, update_by = :operatorId
                    WHERE tenant_id = :tenantId AND last_sync_task_id <> :taskId
                    """, parameters);
            jdbcTemplate.update("""
                    UPDATE integration_rlgj_post SET deleted = 1, update_by = :operatorId
                    WHERE tenant_id = :tenantId AND last_sync_task_id <> :taskId
                    """, parameters);
        }
        return positions.size() + posts.size();
    }

    @Transactional
    public int upsertUsers(Long tenantId, Long taskId, String syncMode, List<HrUserRecord> users) {
        if (users.isEmpty()) {
            return 0;
        }
        Long operatorId = operatorId(tenantId, taskId);
        LocalDateTime syncTime = LocalDateTime.now();
        MapSqlParameterSource[] parameters = users.stream()
                .map(item -> userParameters(tenantId, taskId, operatorId, syncTime, item))
                .toArray(MapSqlParameterSource[]::new);
        jdbcTemplate.batchUpdate(USER_UPSERT, parameters);
        jdbcTemplate.batchUpdate(MERCHANT_USER_UPSERT, parameters);
        if ("FULL".equalsIgnoreCase(syncMode)) {
            MapSqlParameterSource taskParameters = taskParameters(tenantId, taskId).addValue("operatorId", operatorId);
            jdbcTemplate.update("""
                    UPDATE integration_rlgj_user SET deleted = 1, update_by = :operatorId
                    WHERE tenant_id = :tenantId AND last_sync_task_id <> :taskId
                    """, taskParameters);
            jdbcTemplate.update("""
                    UPDATE sys_merchant_user target
                    JOIN integration_rlgj_user source
                      ON source.tenant_id = target.tenant_id
                     AND target.source_type = 'HR_BUTLER'
                     AND target.source_id = source.external_user_id
                    SET target.status = IF(source.deleted = 1 OR source.user_status = '120003', 0, 1),
                        target.deleted = source.deleted, target.update_by = :operatorId
                    WHERE target.tenant_id = :tenantId
                    """, taskParameters);
        }
        return users.size();
    }

    private MapSqlParameterSource organizationParameters(
            Long tenantId, Long taskId, Long operatorId, LocalDateTime syncTime, HrOrganizationRecord item) {
        requireText(item.externalOrgId(), "人力管家组织ID不能为空");
        requireText(item.orgName(), "人力管家组织名称不能为空");
        requireText(item.orgType(), "人力管家组织类型不能为空");
        return baseParameters(tenantId, taskId, operatorId, syncTime)
                .addValue("externalOrgId", item.externalOrgId())
                .addValue("externalParentId", item.externalParentId())
                .addValue("orgCode", item.orgCode())
                .addValue("merchantOrgCode", "HR-" + item.externalOrgId())
                .addValue("orgName", item.orgName())
                .addValue("orgType", item.orgType().toUpperCase())
                .addValue("storeCode", item.storeCode())
                .addValue("leadUserId", item.leadUserId())
                .addValue("leadName", item.leadName())
                .addValue("validStatus", item.valid() ? 1 : 0)
                .addValue("deleted", item.valid() ? 0 : 1)
                .addValue("brandId", item.brandId())
                .addValue("brandName", item.brandName())
                .addValue("province", item.province())
                .addValue("city", item.city())
                .addValue("district", item.district())
                .addValue("orgAddress", item.orgAddress())
                .addValue("costOrgCode", item.costOrgCode())
                .addValue("corporationCode", item.corporationCode())
                .addValue("corporationName", item.corporationName())
                .addValue("superLeadUserId", item.superLeadUserId())
                .addValue("sourceCreateTime", item.sourceCreateTime())
                .addValue("sourceUpdateTime", item.sourceUpdateTime())
                .addValue("rawJson", item.rawJson());
    }

    private MapSqlParameterSource positionParameters(
            Long tenantId, Long taskId, Long operatorId, LocalDateTime syncTime, HrPositionRecord item) {
        requireText(item.externalPositionId(), "人力管家职位ID不能为空");
        requireText(item.positionName(), "人力管家职位名称不能为空");
        return baseParameters(tenantId, taskId, operatorId, syncTime)
                .addValue("externalPositionId", item.externalPositionId())
                .addValue("positionName", item.positionName())
                .addValue("rawJson", item.rawJson());
    }

    private MapSqlParameterSource postParameters(
            Long tenantId, Long taskId, Long operatorId, LocalDateTime syncTime, HrPostRecord item) {
        requireText(item.externalOrgId(), "人力管家岗位所属组织ID不能为空");
        requireText(item.externalPostId(), "人力管家岗位ID不能为空");
        requireText(item.postName(), "人力管家岗位名称不能为空");
        return baseParameters(tenantId, taskId, operatorId, syncTime)
                .addValue("externalOrgId", item.externalOrgId())
                .addValue("externalPostId", item.externalPostId())
                .addValue("externalParentId", item.externalParentId())
                .addValue("externalPositionId", item.externalPositionId())
                .addValue("postName", item.postName())
                .addValue("postType", item.postType())
                .addValue("postTypeName", item.postTypeName())
                .addValue("rawJson", item.rawJson());
    }

    private MapSqlParameterSource userParameters(
            Long tenantId, Long taskId, Long operatorId, LocalDateTime syncTime, HrUserRecord item) {
        requireText(item.externalUserId(), "人力管家用户ID不能为空");
        requireText(item.userName(), "人力管家用户姓名不能为空");
        return baseParameters(tenantId, taskId, operatorId, syncTime)
                .addValue("externalUserId", item.externalUserId())
                .addValue("externalOrgId", item.externalOrgId())
                .addValue("userNumber", item.userNumber())
                .addValue("userName", item.userName())
                .addValue("userNamePinyin", item.userNamePinyin())
                .addValue("genderCode", item.genderCode())
                .addValue("mobile", item.mobile())
                .addValue("email", item.email())
                .addValue("userType", item.userType())
                .addValue("userStatus", item.userStatus())
                .addValue("externalPositionId", item.externalPositionId())
                .addValue("positionName", item.positionName())
                .addValue("externalPostId", item.externalPostId())
                .addValue("postName", item.postName())
                .addValue("postTypeName", item.postTypeName())
                .addValue("externalRankId", item.externalRankId())
                .addValue("rankName", item.rankName())
                .addValue("leaderUserIds", item.leaderUserIds())
                .addValue("offerDate", item.offerDate())
                .addValue("startDate", item.startDate())
                .addValue("rawJson", item.rawJson())
                .addValue("activeStatus", "120003".equals(item.userStatus()) ? 0 : 1);
    }

    private MapSqlParameterSource baseParameters(
            Long tenantId, Long taskId, Long operatorId, LocalDateTime syncTime) {
        return new MapSqlParameterSource()
                .addValue("tenantId", tenantId)
                .addValue("taskId", taskId)
                .addValue("operatorId", operatorId)
                .addValue("syncTime", syncTime);
    }

    private MapSqlParameterSource taskParameters(Long tenantId, Long taskId) {
        return new MapSqlParameterSource("tenantId", tenantId).addValue("taskId", taskId);
    }

    private Long operatorId(Long tenantId, Long taskId) {
        return jdbcTemplate.queryForObject("""
                SELECT created_by FROM integration_sync_task
                WHERE id = :taskId AND tenant_id = :tenantId
                """, taskParameters(tenantId, taskId), Long.class);
    }

    private void requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
    }
}
