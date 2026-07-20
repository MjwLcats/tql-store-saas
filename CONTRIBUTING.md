# 贡献指南

## 分支模型

- `main`：始终保持可发布状态，禁止直接提交业务变更。
- `feature/<scope>-<description>`：功能开发，例如 `feature/app-task-center`。
- `fix/<scope>-<description>`：缺陷修复。
- `chore/<description>`：工程、依赖和文档维护。

开发前从最新 `main` 创建分支：

```bash
git switch main
git pull --ff-only origin main
git switch -c feature/<scope>-<description>
```

## 提交规范

提交信息采用 Conventional Commits：

```text
<type>(<scope>): <summary>
```

常用类型：

- `feat`：新增功能
- `fix`：修复缺陷
- `refactor`：不改变行为的重构
- `test`：测试相关
- `docs`：文档相关
- `build`：构建或依赖相关
- `chore`：其他工程维护

示例：

```text
feat(app): add interruptible tab navigation animation
fix(auth): prevent expired session reuse
```

## Pull Request

1. 一个 PR 只解决一个主题，避免混入无关格式化或重构。
2. 合并前同步 `main` 并处理冲突。
3. 描述变更背景、验证方式、风险与回滚方案。
4. UI 变更提供同尺寸的前后截图或录屏。
5. 至少通过对应子项目的构建、类型检查或测试。
6. 使用 Squash Merge 保持主分支历史清晰。

## 本地验证

### 后端

```bash
cd tql-store-admin
mvn -B -ntp test
```

### PC 前端

```bash
cd tql-store-web
pnpm install --frozen-lockfile
pnpm typecheck
pnpm build
```

### 移动端

使用 HBuilderX 分别运行到 iOS、Android 或 H5。提交前至少验证登录、主导航和本次修改涉及的核心流程。

## 安全要求

- 禁止提交真实密码、Token、私钥、证书、生产地址和客户数据。
- 本地配置写入 `.env`，仓库只保留 `.env.example`。
- 发现密钥误提交后必须立即轮换，删除 Git 历史不能替代密钥轮换。
