# PC Login Preferences And Password Change Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Make the merchant login remember the merchant number together with the account, and let the signed-in user securely change their own login password from the profile page.

**Architecture:** Persist only the merchant number and username under a client-scoped browser key, while using standard autocomplete attributes for the browser password manager instead of storing plaintext passwords. Add an authenticated auth-service endpoint that verifies the current password, hashes the replacement with the existing PBKDF2 utility, updates only the session user, and revokes the current session after success.

**Tech Stack:** Vue 3, TypeScript, Arco Design Vue, Axios, Spring Boot 3, JdbcTemplate, Redis, JUnit 5, Mockito.

---

### Task 1: Merchant login preferences

**Files:**
- Modify: `tql-store-web/packages/ui/src/pages/LoginPage.vue`

**Steps:**
1. Add client-scoped parsing and writing for remembered merchant number and username.
2. Restore saved values on mount and remove them when remembering is disabled.
3. Add browser autocomplete metadata for merchant number, username, and current password.
4. Run `pnpm --filter @tql-store/merchant typecheck` and expect success.

### Task 2: Authenticated password change API

**Files:**
- Create: `tql-store-admin/tql-store-auth/src/main/java/com/tql/store/auth/model/ChangePasswordRequest.java`
- Modify: `tql-store-admin/tql-store-auth/src/main/java/com/tql/store/auth/controller/AuthController.java`
- Modify: `tql-store-admin/tql-store-auth/src/main/java/com/tql/store/auth/service/AuthService.java`
- Modify: `tql-store-admin/tql-store-auth/pom.xml`
- Create: `tql-store-admin/tql-store-auth/src/test/java/com/tql/store/auth/service/AuthServiceTest.java`

**Steps:**
1. Write tests for rejecting a wrong current password and updating a merchant user's hash with tenant scoping.
2. Run the auth-module tests and verify they fail before the endpoint exists.
3. Add the validated request model and authenticated controller method.
4. Implement current-password verification, 8–64 character validation, PBKDF2 hashing, scoped update, and current-session revocation.
5. Run `mvn -pl tql-store-auth -am test` and expect all tests to pass.

### Task 3: Profile password dialog

**Files:**
- Modify: `tql-store-web/packages/shared/src/index.ts`
- Modify: `tql-store-web/packages/api/src/index.ts`
- Modify: `tql-store-web/packages/ui/src/pages/ProfilePage.vue`

**Steps:**
1. Add the shared password-change payload and API client method.
2. Add a security settings section and password modal to the profile page.
3. Validate current password, new password, confirmation, length, and non-reuse before submission.
4. On success, clear the local token and store, then redirect to login with a success message.
5. Run merchant typecheck/build and verify the rendered login and profile flows in a desktop browser.

### Task 4: Final verification

**Files:**
- Verify all files above.

**Steps:**
1. Run backend tests, frontend typecheck, frontend build, and whitespace checks.
2. Verify login preference restoration and the profile modal at desktop viewport.
3. Confirm no relevant browser console warnings or errors.
4. Commit the implementation with a focused feature commit.
