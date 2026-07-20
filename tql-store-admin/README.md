# tql-store-admin

Spring Boot 3.2、Spring Cloud 2023 与 Spring Cloud Alibaba 组成的多服务 Maven 工程，JDK 版本为 17。

## 初始化数据库

复制环境变量模板并填写当前环境的连接信息。`.env.local` 已被 Git 忽略，禁止提交真实密码：

```bash
cp .env.example .env.local
```

macOS/Linux 可通过安全启动脚本加载变量，例如：

```bash
scripts/with-env.sh mvn -pl tql-store-auth spring-boot:run
```

IDEA 启动时，将 `.env.local` 中的变量录入 Run Configuration 的 Environment variables。数据库账号和密码没有代码默认值，未配置时服务会拒绝启动。

初始化或迁移数据库前必须确认目标环境并完成备份。脚本会创建 `tql_store_saas` 数据库、基础用户、菜单及内容演示数据，可重复执行；生产环境禁止直接执行初始化脚本。

已有第一阶段数据库升级到 RBAC 版本时，按顺序执行：

```bash
set -a
source .env.local
set +a
export MYSQL_PWD="${MYSQL_PASSWORD}"
mysql --host="${MYSQL_HOST}" --port="${MYSQL_PORT}" --user="${MYSQL_USERNAME}" --default-character-set=utf8mb4 "${MYSQL_DATABASE}" < sql/migration/V002__rbac_user_role_store.sql
```

其余迁移按版本号顺序执行，禁止跳版；密码只通过进程环境传递，不写入命令、脚本或提交记录。

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
