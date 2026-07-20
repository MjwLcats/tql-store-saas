# tql-store-web

基于 Vue 3、TypeScript、Vite、Pinia、Vue Router 和 Arco Design Vue 的 pnpm workspace。平台端与商家端独立启动、独立构建，共享 API、会话、类型、主题和业务组件。

## Workspace

- `apps/platform`：平台端，端口 `3100`
- `apps/merchant`：商家端，端口 `3101`
- `packages/api`：网关请求与接口封装
- `packages/auth`：双端隔离的 Token 与登录状态
- `packages/config`：Arco Design Pro 风格主题规范
- `packages/shared`：共享类型
- `packages/ui`：布局、登录、工作台、内容列表、个人中心

## WebStorm 启动

首次安装依赖：

```powershell
pnpm install
```

在 WebStorm 中分别创建两个 pnpm Run Configuration：

```powershell
pnpm dev:platform
pnpm dev:merchant
```

启动前请先在 IDEA 中运行后端认证、系统、运营和网关服务。

## 校验与构建

```powershell
pnpm typecheck
pnpm build
```

本地演示账号为平台端 `platform_admin / Platform@123`，商家端 `TQL-DEMO / merchant_admin / Merchant@123`（商户号 / 账号 / 密码）。前端显示默认账号仅用于开发环境，生产构建前应移除。
