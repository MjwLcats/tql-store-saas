# tql-store-admin

Spring Boot 3.2、Spring Cloud 2023 与 Spring Cloud Alibaba 组成的多服务 Maven 工程，JDK 版本为 17。

## 初始化数据库

确认本机 MySQL 8 和 Redis 已启动，然后在项目根目录执行：

```powershell
mysql --host=localhost --port=3306 --user=root --password=123456 --default-character-set=utf8mb4 --execute="source D:/Project/tql-store-saas/tql-store-admin/sql/init/tql_store_saas.sql"
```

脚本会创建 `tql_store_saas` 数据库、基础用户、菜单及内容演示数据，可重复执行。MySQL 密码可通过各服务的 `MYSQL_PASSWORD` 环境变量覆盖，默认值仅用于本地开发。

已有第一阶段数据库升级到 RBAC 版本时，按顺序执行：

```powershell
mysql --host=localhost --port=3306 --user=root --password=123456 --default-character-set=utf8mb4 --execute="source D:/Project/tql-store-saas/tql-store-admin/sql/migration/V002__rbac_user_role_store.sql"
mysql --host=localhost --port=3306 --user=root --password=123456 --default-character-set=utf8mb4 --execute="source D:/Project/tql-store-saas/tql-store-admin/sql/migration/V003__operation_content_data_scope.sql"
mysql --host=localhost --port=3306 --user=root --password=123456 --default-character-set=utf8mb4 --execute="source D:/Project/tql-store-saas/tql-store-admin/sql/migration/V004__integration_sync_task.sql"
mysql --host=localhost --port=3306 --user=root --password=123456 --default-character-set=utf8mb4 --execute="source D:/Project/tql-store-saas/tql-store-admin/sql/migration/V005__integration_hll_shop.sql"
mysql --host=localhost --port=3306 --user=root --password=123456 --default-character-set=utf8mb4 --execute="source D:/Project/tql-store-saas/tql-store-admin/sql/migration/V006__repair_complete_chinese_comments.sql"
mysql --host=localhost --port=3306 --user=root --password=123456 --default-character-set=utf8mb4 --execute="source D:/Project/tql-store-saas/tql-store-admin/sql/migration/V007__hr_butler_master_data.sql"
mysql --host=localhost --port=3306 --user=root --password=123456 --default-character-set=utf8mb4 --execute="source D:/Project/tql-store-saas/tql-store-admin/sql/migration/V008__split_platform_merchant_user_rbac.sql"
mysql --host=localhost --port=3306 --user=root --password=123456 --default-character-set=utf8mb4 --execute="source D:/Project/tql-store-saas/tql-store-admin/sql/migration/V013__content_activity_plan_employee_task.sql"
mysql --host=localhost --port=3306 --user=root --password=123456 --default-character-set=utf8mb4 --execute="source D:/Project/tql-store-saas/tql-store-admin/sql/migration/V014__ai_content_menu_and_button_permissions.sql"
mysql --host=localhost --port=3306 --user=root --password=123456 --default-character-set=utf8mb4 --execute="source D:/Project/tql-store-saas/tql-store-admin/sql/migration/V015__fix_merchant_dashboard_route.sql"
mysql --host=localhost --port=3306 --user=root --password=123456 --default-character-set=utf8mb4 --execute="source D:/Project/tql-store-saas/tql-store-admin/sql/migration/V016__content_platform_account_crud.sql"
mysql --host=localhost --port=3306 --user=root --password=123456 --default-character-set=utf8mb4 --execute="source D:/Project/tql-store-saas/tql-store-admin/sql/migration/V017__content_account_automatic_verification.sql"
mysql --host=localhost --port=3306 --user=root --password=123456 --default-character-set=utf8mb4 --execute="source D:/Project/tql-store-saas/tql-store-admin/sql/migration/V018__content_video_performance_report.sql"
mysql --host=localhost --port=3306 --user=root --password=123456 --default-character-set=utf8mb4 --execute="source D:/Project/tql-store-saas/tql-store-admin/sql/migration/V019__merchant_admin_all_permissions.sql"
mysql --host=localhost --port=3306 --user=root --password=123456 --default-character-set=utf8mb4 --execute="source D:/Project/tql-store-saas/tql-store-admin/sql/migration/V020__content_personnel_import_permissions.sql"
mysql --host=localhost --port=3306 --user=root --password=123456 --default-character-set=utf8mb4 --execute="source D:/Project/tql-store-saas/tql-store-admin/sql/migration/V021__content_storyboard_count.sql"
mysql --host=localhost --port=3306 --user=root --password=123456 --default-character-set=utf8mb4 --execute="source D:/Project/tql-store-saas/tql-store-admin/sql/migration/V022__content_plan_edit_and_personnel_label.sql"
mysql --host=localhost --port=3306 --user=root --password=123456 --default-character-set=utf8mb4 --execute="source D:/Project/tql-store-saas/tql-store-admin/sql/migration/V023__complete_ai_content_button_permissions.sql"
```

当前本机数据库已执行上述迁移。第三方同步模块联调需要启动集成服务，并在修改网关路由后重新启动网关。

所有建表 SQL 必须为每张表和每个字段提供完整中文注释。Windows 环境不要使用 `Get-Content ... | mysql` 执行中文 SQL，必须使用上面的 MySQL `source` 方式，并始终指定 `--default-character-set=utf8mb4`，否则中文注释可能被转换为问号。

建表或迁移完成后，执行 `sql/check/validate_database_comments.sql` 验收；`invalid_table_comments` 和 `invalid_column_comments` 必须都为 `0`。

## IDEA 启动

1. 使用 IDEA 打开本目录并导入根 `pom.xml`，Project SDK 选择 JDK 17。
2. 确认 MySQL、Redis 已运行。当前开发配置默认关闭 Nacos 服务发现；本机启动 Nacos 后可设置环境变量 `NACOS_DISCOVERY_ENABLED=true`。
3. 依次运行 `AuthApplication`、`SystemApplication`、`OperationApplication`、`IntegrationApplication`、`GatewayApplication`。
4. 其余第一阶段服务按需运行：`TrainingApplication`、`ForecastApplication`、`AiApplication`、`FileApplication`、`MessageApplication`、`SchedulerApplication`。

启动 `IntegrationApplication` 前，在 IDEA Run Configuration 的 Environment variables 中配置 `HUALALA_APP_SECRET` 和 `HR_BUTLER_APP_SECRET`。两个第三方平台的非敏感参数均可通过 `application.yml` 中声明的环境变量覆盖，任何密钥都禁止提交到仓库。

人力管家还支持以下非敏感环境变量：`HR_BUTLER_URL`、`HR_BUTLER_CORP_NAME`、`HR_BUTLER_APP_ID`、`HR_BUTLER_POST_CONCURRENCY`、`HR_BUTLER_MAX_ATTEMPTS`。PC 商家端可分别手动触发“部门组织”“岗位职位”“员工用户”全量同步；同步用户默认不开放登录权限。

用户体系已完成端侧拆分：平台认证和 RBAC 使用 `sys_platform_user`、`sys_platform_user_role`；商家认证和 RBAC 使用 `sys_merchant_user`、`sys_merchant_user_role`、`sys_merchant_user_store`。历史统一表已归档为 `_legacy` 表，运行时代码不得访问。执行 V008 后必须重启认证、系统、运营、集成和网关服务，使旧会话后的权限检查全部切换到新表。

核心端口：网关 `8080`、认证 `9200`、系统 `9201`、运营内容 `9202`、第三方集成 `9206`。`tql-store-common` 是公共模块，无需启动。

## 构建

```powershell
mvn -DskipTests package
```

本地联调的所有前端请求均通过 `http://localhost:8080` 网关访问。
