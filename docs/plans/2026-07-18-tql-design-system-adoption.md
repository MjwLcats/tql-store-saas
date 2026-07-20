# TQL Design System Adoption Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Make the TQL frontend design system automatically discoverable and migrate the current frontend onto reusable tokens, business selectors, permission primitives, and standard page templates.

**Architecture:** Keep Arco Design Vue as the base layer. Put cross-application visual tokens in `@tql-store/config`, business UI and permission presentation components in `@tql-store/ui`, and permission evaluation in `@tql-store/auth`. Add the smallest backend organization query needed for the P02 user-management layout; backend RBAC remains authoritative.

**Tech Stack:** Vue 3, TypeScript, Pinia, Arco Design Vue, Vite, Spring Boot 3.2, JdbcTemplate, pnpm workspace.

---

### Task 1: Activate repository instructions

**Files:**
- Create: `AGENTS.md`

1. Point frontend tasks to the sibling TQL design-system source.
2. Encode page classification, token, component reuse, and review requirements.
3. Verify the file is rooted above all application source directories.

### Task 2: Complete the theme foundation

**Files:**
- Modify: `tql-store-web/packages/config/src/theme.css`

1. Add semantic colors, complete text/background tokens, spacing, radius, shadow, layout, and table-density tokens.
2. Add shared page/card/table utility styles.
3. Keep compatibility aliases for tokens already used by the application shell.
4. Run both application typechecks.

### Task 3: Add permission primitives

**Files:**
- Modify: `tql-store-web/packages/auth/src/index.ts`
- Create: `tql-store-web/packages/ui/src/permissions/PermissionGate.vue`
- Create: `tql-store-web/packages/ui/src/permissions/FieldPermission.vue`
- Create: `tql-store-web/packages/ui/src/permissions/directive.ts`
- Modify: `tql-store-web/packages/ui/src/index.ts`
- Modify: both application `main.ts` files

1. Derive granted permission codes from the authenticated menu context.
2. Implement any/all permission evaluation.
3. Add component, directive, and field fallback/read-only presentation APIs.
4. Register `v-permission` after Pinia is installed.
5. Typecheck both applications.

### Task 4: Add reusable business selectors

**Files:**
- Create: `tql-store-web/packages/ui/src/business/StoreSelector.vue`
- Create: `tql-store-web/packages/ui/src/business/DataScopeSelector.vue`
- Create: `tql-store-web/packages/ui/src/business/OrganizationTree.vue`
- Modify: `tql-store-web/packages/shared/src/index.ts`
- Modify: `tql-store-web/packages/ui/src/index.ts`

1. Define stable option contracts.
2. Implement controlled single/multiple selection and historical-value-friendly rendering.
3. Implement searchable, highlighted organization navigation without coupling it to a page API.
4. Typecheck exports and consumers.

### Task 5: Supply organization data for P02

**Files:**
- Add an organization view model in `tql-store-system`.
- Modify system controller/service, shared types, and API package.

1. Add a tenant/client-scoped read-only organization endpoint.
2. Filter user queries by the selected organization.
3. Never trust a tenant identifier supplied by the browser.
4. Compile the system module and typecheck the frontend.

### Task 6: Migrate current pages

**Files:**
- Modify: current shared UI pages and layout.

1. Convert user management to P02 and replace its complex modal with an 800px drawer.
2. Reuse StoreSelector and DataScopeSelector in queries and forms.
3. Convert role permission maintenance to the P03 select-and-configure structure where supported by current APIs.
4. Replace hard-coded shared colors, spacing, radius, and table height with tokens.
5. Add permission wrappers to destructive and create actions without weakening backend checks.

### Task 7: Verify

1. Run `pnpm typecheck`.
2. Run `pnpm build`.
3. Run the Maven system-module compile/test command.
4. Inspect rendered platform and merchant pages if the local services are available.
5. Report any remaining backend-dependent gaps rather than adding mock production data.
