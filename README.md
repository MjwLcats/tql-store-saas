# TQL Store SaaS

同庆楼门店运营 SaaS 单体仓库，包含 Spring Cloud 后端、Vue 3 PC 端和 uni-app 移动端。

## 目录

- `tql-store-admin`：Java 17、Spring Boot 3、Spring Cloud 多服务后端。
- `tql-store-web`：pnpm workspace，包含平台端、商家端和共享包。
- `tql-store-app`：Vue 3 uni-app 移动端，支持 iOS、Android 和 H5。
- `docs`：架构、命名和本地开发文档。

## 环境要求

- JDK 17、Maven 3.9+
- Node.js 20、pnpm 11.13.1
- HBuilderX 5.15+
- MySQL 8、Redis、Nacos

## 本地地址

| 应用 | 地址 |
| --- | --- |
| API 网关 | `http://localhost:8080` |
| 平台端 | `http://localhost:3100` |
| 商家端 | `http://localhost:3101` |

## 快速开始

详细初始化和启动步骤见：

- [后端说明](tql-store-admin/README.md)
- [PC 前端说明](tql-store-web/README.md)
- [框架搭建开发文档](docs/框架搭建开发文档.md)

本地环境变量从各子项目的 `.env.example` 复制生成，真实账号、密码和密钥不得提交到 Git。

## 协作流程

功能开发从最新 `main` 创建独立分支，通过 Pull Request 合并。分支命名、提交信息、验证要求和安全约束见 [CONTRIBUTING.md](CONTRIBUTING.md)。

## 安全说明

仓库不保存生产凭据或真实客户数据。示例配置只用于本地开发，部署环境必须通过密钥管理或环境变量注入凭据。
