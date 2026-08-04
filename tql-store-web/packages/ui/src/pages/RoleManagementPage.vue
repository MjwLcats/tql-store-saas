<template>
  <div class="role-page">
    <section class="role-workbench">
      <aside class="role-rail">
        <div class="role-search">
          <a-input-search
            v-model="query.keyword"
            placeholder="搜索角色名称/编码"
            allow-clear
            @search="loadRoles"
            @press-enter="loadRoles"
          />
          <a-tooltip content="新增角色">
            <a-button type="primary" aria-label="新增角色" @click="openCreate">
              <template #icon><IconPlus /></template>
            </a-button>
          </a-tooltip>
        </div>
        <a-spin :loading="loading" class="role-list-spin">
          <div v-if="records.length" class="role-list">
            <button
              v-for="role in records"
              :key="role.id"
              type="button"
              class="role-row"
              :class="{ active: role.id === editingId }"
              @click="openEdit(role)"
            >
              <span class="role-row-main">
                <strong>{{ role.name }}</strong>
                <small>{{ role.code }}</small>
              </span>
              <span class="role-row-meta">
                <a-badge :status="role.status === 1 ? 'success' : 'normal'" />
                <small>{{ role.userCount }}人</small>
              </span>
            </button>
          </div>
          <a-empty v-else description="暂无角色" />
        </a-spin>
      </aside>

      <main class="role-content">
        <template v-if="editorVisible">
          <header class="role-header">
            <div>
              <h2>{{ editingId ? form.roleName : '新增角色' }}</h2>
              <p>{{ editingId ? '配置角色信息、功能权限及关联用户' : '创建角色并分配功能权限' }}</p>
            </div>
            <a-space>
              <a-popconfirm
                v-if="activeRole"
                :content="`确定删除角色“${activeRole.name}”吗？`"
                @ok="remove(activeRole.id)"
              >
                <a-button status="danger" :disabled="activeRole.userCount > 0">删除</a-button>
              </a-popconfirm>
              <a-button type="primary" :loading="saving" @click="submit">
                <template #icon><IconSave /></template>
                保存
              </a-button>
            </a-space>
          </header>

          <a-form ref="formRef" :model="form" :rules="rules" layout="vertical" class="role-form">
            <div class="role-basic">
              <a-form-item field="roleName" label="角色名称">
                <a-input v-model="form.roleName" placeholder="请输入角色名称" />
              </a-form-item>
              <a-form-item field="roleCode" label="角色编码">
                <a-input v-model="form.roleCode" placeholder="例如：STORE_MANAGER" />
              </a-form-item>
              <a-form-item field="status" label="状态">
                <a-radio-group v-model="form.status">
                  <a-radio :value="1">启用</a-radio>
                  <a-radio :value="0">停用</a-radio>
                </a-radio-group>
              </a-form-item>
              <a-form-item field="remark" label="角色说明" class="role-description">
                <a-textarea
                  v-model="form.remark"
                  placeholder="说明该角色的业务职责"
                  :max-length="255"
                  show-word-limit
                />
              </a-form-item>
            </div>

            <a-tabs v-model:active-key="activeTab" class="role-tabs" @change="handleTabChange">
              <a-tab-pane key="permissions" title="功能权限">
                <div class="permission-actions">
                  <span>已选择 <strong>{{ form.menuIds.length }}</strong> 项权限</span>
                  <a-space>
                    <a-button size="small" @click="selectAll">全选</a-button>
                    <a-button size="small" @click="form.menuIds = []">清空</a-button>
                    <a-button size="small" @click="collapseAll">折叠</a-button>
                  </a-space>
                </div>

                <div class="permission-matrix">
                  <a-collapse v-model:active-key="expandedGroups" :bordered="false">
                    <a-collapse-item
                      v-for="group in permissionGroups"
                      :key="String(group.id)"
                      :header="group.name"
                    >
                      <template #extra>
                        <a-checkbox
                          :model-value="isAllSelected(group.allIds)"
                          :indeterminate="isPartSelected(group.allIds)"
                          @click.stop
                          @change="toggleIds(group.allIds, $event as boolean)"
                        >
                          本组全选
                        </a-checkbox>
                      </template>
                      <div class="permission-table">
                        <div class="permission-table-head">
                          <span>菜单</span>
                          <span>按钮权限</span>
                        </div>
                        <div v-for="row in group.rows" :key="row.id" class="permission-table-row">
                          <div class="permission-menu-cell">
                            <a-checkbox
                              :model-value="form.menuIds.includes(row.id)"
                              @change="toggleIds([row.id, ...row.buttons.map(item => item.id)], $event as boolean)"
                            >
                              {{ row.name }}
                            </a-checkbox>
                          </div>
                          <div class="permission-buttons-cell">
                            <a-checkbox
                              v-for="button in row.buttons"
                              :key="button.id"
                              :model-value="form.menuIds.includes(button.id)"
                              @change="toggleIds([button.id], $event as boolean)"
                            >
                              {{ button.name }}
                            </a-checkbox>
                            <span v-if="!row.buttons.length" class="permission-empty">页面访问</span>
                          </div>
                        </div>
                      </div>
                    </a-collapse-item>
                  </a-collapse>
                </div>
              </a-tab-pane>

              <a-tab-pane key="users" :title="`角色用户（${activeRole?.userCount || 0}）`" :disabled="!editingId">
                <div class="user-toolbar">
                  <a-space>
                    <a-button type="primary" @click="openAssignUsers">
                      <template #icon><IconPlus /></template>
                      分配用户
                    </a-button>
                    <a-button
                      status="danger"
                      :disabled="!selectedRoleUserIds.length"
                      :loading="updatingUsers"
                      @click="removeSelectedUsers"
                    >
                      取消分配
                    </a-button>
                  </a-space>
                  <a-input-search
                    v-model="userKeyword"
                    class="user-search"
                    placeholder="搜索姓名、账号或手机号"
                    allow-clear
                    @search="loadRoleUsers"
                    @press-enter="loadRoleUsers"
                  />
                </div>
                <a-table
                  v-model:selected-keys="selectedRoleUserIds"
                  :data="roleUsers"
                  :loading="usersLoading"
                  row-key="id"
                  :pagination="{ pageSize: 10, showTotal: true }"
                  :row-selection="{ type: 'checkbox', showCheckedAll: true }"
                >
                  <template #columns>
                    <a-table-column title="姓名" data-index="displayName" />
                    <a-table-column title="账号" data-index="username">
                      <template #cell="{ record }">{{ record.username || '-' }}</template>
                    </a-table-column>
                    <a-table-column title="手机号" data-index="phone">
                      <template #cell="{ record }">{{ record.phone || '-' }}</template>
                    </a-table-column>
                    <a-table-column title="所属组织" data-index="organizationName">
                      <template #cell="{ record }">{{ record.organizationName || '-' }}</template>
                    </a-table-column>
                    <a-table-column title="状态" :width="110">
                      <template #cell="{ record }">
                        <a-badge :status="record.status === 1 ? 'success' : 'normal'" :text="record.status === 1 ? '启用' : '停用'" />
                      </template>
                    </a-table-column>
                    <a-table-column title="操作" :width="110">
                      <template #cell="{ record }">
                        <a-link status="danger" @click="removeRoleFromUsers([record.id])">取消分配</a-link>
                      </template>
                    </a-table-column>
                  </template>
                </a-table>
              </a-tab-pane>
            </a-tabs>
          </a-form>
        </template>

        <a-empty v-else description="请选择左侧角色开始配置">
          <template #extra><a-button type="primary" @click="openCreate">新增角色</a-button></template>
        </a-empty>
      </main>
    </section>

    <a-modal
      v-model:visible="assignVisible"
      title="分配角色用户"
      width="760px"
      :ok-loading="updatingUsers"
      ok-text="确认分配"
      @ok="assignSelectedUsers"
    >
      <a-input-search
        v-model="assignKeyword"
        placeholder="搜索姓名、账号或手机号"
        allow-clear
        @search="loadAssignableUsers"
        @press-enter="loadAssignableUsers"
      />
      <a-table
        v-model:selected-keys="selectedAssignableUserIds"
        class="assign-user-table"
        :data="assignableUsers"
        :loading="usersLoading"
        row-key="id"
        :pagination="{ pageSize: 8, showTotal: true }"
        :row-selection="{ type: 'checkbox', showCheckedAll: true }"
      >
        <template #columns>
          <a-table-column title="姓名" data-index="displayName" />
          <a-table-column title="账号" data-index="username">
            <template #cell="{ record }">{{ record.username || '-' }}</template>
          </a-table-column>
          <a-table-column title="手机号" data-index="phone">
            <template #cell="{ record }">{{ record.phone || '-' }}</template>
          </a-table-column>
          <a-table-column title="所属组织" data-index="organizationName">
            <template #cell="{ record }">{{ record.organizationName || '-' }}</template>
          </a-table-column>
        </template>
      </a-table>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { Message, type FormInstance } from '@arco-design/web-vue';
import { IconPlus, IconSave } from '@arco-design/web-vue/es/icon';
import {
  createRole,
  deleteRole,
  fetchAssignableMenus,
  fetchRoles,
  fetchUser,
  fetchUsers,
  updateRole,
  updateUser
} from '@tql-store/api';
import type { MenuItem, RoleItem, RoleSavePayload, UserDetail, UserItem, UserSavePayload } from '@tql-store/shared';

type PermissionRow = { id: number; name: string; buttons: MenuItem[] };
type PermissionGroup = { id: number; name: string; allIds: number[]; rows: PermissionRow[] };

const loading = ref(false);
const saving = ref(false);
const usersLoading = ref(false);
const updatingUsers = ref(false);
const editorVisible = ref(false);
const editingId = ref<number>();
const formRef = ref<FormInstance>();
const records = ref<RoleItem[]>([]);
const menus = ref<MenuItem[]>([]);
const query = reactive({ keyword: '' });
const activeTab = ref('permissions');
const expandedGroups = ref<string[]>([]);
const roleUsers = ref<UserItem[]>([]);
const assignableUsers = ref<UserItem[]>([]);
const selectedRoleUserIds = ref<number[]>([]);
const selectedAssignableUserIds = ref<number[]>([]);
const userKeyword = ref('');
const assignKeyword = ref('');
const assignVisible = ref(false);

const emptyForm = (): RoleSavePayload => ({ roleCode: '', roleName: '', status: 1, remark: '', menuIds: [] });
const form = reactive<RoleSavePayload>(emptyForm());
const activeRole = computed(() => records.value.find(role => role.id === editingId.value));
const menuMap = computed(() => new Map(menus.value.map(item => [item.id, item])));

const permissionGroups = computed<PermissionGroup[]>(() => {
  const childrenOf = (id: number) => menus.value.filter(item => item.parentId === id);
  const descendants = (id: number): MenuItem[] =>
    childrenOf(id).flatMap(item => [item, ...descendants(item.id)]);
  const roots = menus.value.filter(item => !menuMap.value.has(item.parentId));
  return roots.map(root => {
    const branch = descendants(root.id);
    const menuRows = branch.filter(item => item.type === 'MENU');
    const rows = menuRows.map(item => ({
      id: item.id,
      name: item.name,
      buttons: descendants(item.id).filter(child => child.type === 'BUTTON')
    }));
    if (!rows.length) {
      rows.push({
        id: root.id,
        name: root.name,
        buttons: branch.filter(item => item.type === 'BUTTON')
      });
    }
    return { id: root.id, name: root.name, allIds: [root.id, ...branch.map(item => item.id)], rows };
  });
});

const rules = {
  roleName: [{ required: true, message: '请输入角色名称' }],
  roleCode: [
    { required: true, message: '请输入角色编码' },
    { match: /^[A-Z][A-Z0-9_]*$/, message: '仅支持大写字母、数字和下划线' }
  ]
};

onMounted(async () => {
  await Promise.all([loadRoles(), loadMenus()]);
  if (records.value[0]) openEdit(records.value[0]);
});

async function loadRoles() {
  loading.value = true;
  try {
    records.value = await fetchRoles(query);
  } catch (error) {
    Message.error(error instanceof Error ? error.message : '角色列表加载失败');
  } finally {
    loading.value = false;
  }
}

async function loadMenus() {
  try {
    menus.value = await fetchAssignableMenus();
    expandedGroups.value = permissionGroups.value.map(group => String(group.id));
  } catch (error) {
    Message.error(error instanceof Error ? error.message : '菜单权限加载失败');
  }
}

function resetForm() {
  Object.assign(form, emptyForm());
  editingId.value = undefined;
  activeTab.value = 'permissions';
  roleUsers.value = [];
  formRef.value?.clearValidate();
}

function openCreate() {
  resetForm();
  editorVisible.value = true;
}

function openEdit(role: RoleItem) {
  editingId.value = role.id;
  Object.assign(form, {
    roleCode: role.code,
    roleName: role.name,
    status: role.status,
    remark: role.remark || '',
    menuIds: [...role.menuIds]
  });
  editorVisible.value = true;
  if (activeTab.value === 'users') loadRoleUsers();
}

function selectAll() {
  form.menuIds = menus.value.map(menu => menu.id);
}

function collapseAll() {
  expandedGroups.value = [];
}

function isAllSelected(ids: number[]) {
  return ids.length > 0 && ids.every(id => form.menuIds.includes(id));
}

function isPartSelected(ids: number[]) {
  const count = ids.filter(id => form.menuIds.includes(id)).length;
  return count > 0 && count < ids.length;
}

function toggleIds(ids: number[], checked: boolean) {
  const selected = new Set(form.menuIds);
  ids.forEach(id => checked ? selected.add(id) : selected.delete(id));
  form.menuIds = [...selected];
}

async function submit() {
  const errors = await formRef.value?.validate();
  if (errors) return;
  saving.value = true;
  try {
    const payload = { ...form, roleCode: form.roleCode.trim().toUpperCase(), menuIds: [...form.menuIds] };
    if (editingId.value) await updateRole(editingId.value, payload);
    else await createRole(payload);
    Message.success(editingId.value ? '角色已更新' : '角色已创建');
    const savedCode = payload.roleCode;
    await loadRoles();
    const saved = records.value.find(role => role.code === savedCode);
    if (saved) openEdit(saved);
  } catch (error) {
    Message.error(error instanceof Error ? error.message : '角色保存失败');
  } finally {
    saving.value = false;
  }
}

async function remove(id: number) {
  try {
    await deleteRole(id);
    Message.success('角色已删除');
    resetForm();
    await loadRoles();
    if (records.value[0]) openEdit(records.value[0]);
  } catch (error) {
    Message.error(error instanceof Error ? error.message : '角色删除失败');
  }
}

function handleTabChange(key: string | number) {
  if (String(key) === 'users') loadRoleUsers();
}

async function queryAllUsers(keyword: string) {
  const first = await fetchUsers({ keyword: keyword || undefined, page: 1, pageSize: 100 });
  const users = [...first.records];
  const pages = Math.ceil(first.total / 100);
  for (let page = 2; page <= pages; page++) {
    const result = await fetchUsers({ keyword: keyword || undefined, page, pageSize: 100 });
    users.push(...result.records);
  }
  return users;
}

async function loadRoleUsers() {
  if (!activeRole.value) return;
  usersLoading.value = true;
  try {
    const users = await queryAllUsers(userKeyword.value);
    roleUsers.value = users.filter(user => user.roleNames.includes(activeRole.value!.name));
    selectedRoleUserIds.value = [];
  } catch (error) {
    Message.error(error instanceof Error ? error.message : '角色用户加载失败');
  } finally {
    usersLoading.value = false;
  }
}

async function openAssignUsers() {
  if (!activeRole.value) return;
  assignVisible.value = true;
  selectedAssignableUserIds.value = [];
  assignKeyword.value = '';
  await loadAssignableUsers();
}

async function loadAssignableUsers() {
  if (!activeRole.value) return;
  usersLoading.value = true;
  try {
    const users = await queryAllUsers(assignKeyword.value);
    assignableUsers.value = users.filter(user => !user.roleNames.includes(activeRole.value!.name));
  } catch (error) {
    Message.error(error instanceof Error ? error.message : '可分配用户加载失败');
  } finally {
    usersLoading.value = false;
  }
}

function toUserPayload(detail: UserDetail, roleIds: number[]): UserSavePayload {
  return {
    username: detail.username,
    loginEnabled: detail.loginEnabled,
    organizationId: detail.organizationId,
    displayName: detail.displayName,
    email: detail.email,
    phone: detail.phone,
    status: detail.status,
    dataScope: detail.dataScope,
    primaryStoreId: detail.primaryStoreId,
    roleIds,
    storeIds: [...detail.storeIds]
  };
}

async function assignSelectedUsers() {
  if (!activeRole.value || !selectedAssignableUserIds.value.length) {
    Message.warning('请先选择用户');
    return false;
  }
  updatingUsers.value = true;
  try {
    for (const id of selectedAssignableUserIds.value) {
      const detail = await fetchUser(id);
      const roleIds = [...new Set([...detail.roleIds, activeRole.value.id])];
      await updateUser(id, toUserPayload(detail, roleIds));
    }
    Message.success(`已分配 ${selectedAssignableUserIds.value.length} 名用户`);
    assignVisible.value = false;
    await Promise.all([loadRoles(), loadRoleUsers()]);
    return true;
  } catch (error) {
    Message.error(error instanceof Error ? error.message : '用户分配失败');
    return false;
  } finally {
    updatingUsers.value = false;
  }
}

async function removeSelectedUsers() {
  await removeRoleFromUsers(selectedRoleUserIds.value);
}

async function removeRoleFromUsers(userIds: number[]) {
  if (!activeRole.value || !userIds.length) return;
  updatingUsers.value = true;
  try {
    for (const id of userIds) {
      const detail = await fetchUser(id);
      await updateUser(id, toUserPayload(detail, detail.roleIds.filter(roleId => roleId !== activeRole.value!.id)));
    }
    Message.success(`已取消 ${userIds.length} 名用户的角色`);
    await Promise.all([loadRoles(), loadRoleUsers()]);
  } catch (error) {
    Message.error(error instanceof Error ? error.message : '取消分配失败');
  } finally {
    updatingUsers.value = false;
  }
}
</script>

<style scoped>
.role-page {
  width: 100%;
  padding: var(--tql-page-padding);
}

.role-workbench {
  display: grid;
  grid-template-columns: 300px minmax(0, 1fr);
  min-height: calc(100vh - 92px);
  overflow: hidden;
  background: var(--tql-color-white);
  border: 1px solid var(--tql-border);
  border-radius: var(--tql-radius-card);
}

.role-rail {
  padding: 16px 12px;
  border-right: 1px solid var(--tql-border);
  background: var(--tql-color-white);
}

.role-search {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 34px;
  gap: 8px;
}

.role-search :deep(.arco-btn) {
  width: 34px;
  padding: 0;
}

.role-list-spin {
  display: block;
  margin-top: 12px;
}

.role-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.role-row {
  display: flex;
  width: 100%;
  min-height: 54px;
  align-items: center;
  justify-content: space-between;
  padding: 8px 12px;
  color: var(--tql-text-primary);
  text-align: left;
  background: transparent;
  border: 0;
  border-radius: 4px;
  cursor: pointer;
}

.role-row:hover {
  background: var(--tql-bg-subtle);
}

.role-row.active {
  color: rgb(var(--primary-6));
  background: rgb(var(--primary-1));
}

.role-row-main,
.role-row-meta {
  display: flex;
  min-width: 0;
}

.role-row-main {
  flex-direction: column;
  gap: 3px;
}

.role-row-main strong {
  overflow: hidden;
  font-weight: 500;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.role-row-main small,
.role-row-meta small {
  color: var(--tql-text-tertiary);
}

.role-row-meta {
  align-items: center;
  gap: 6px;
}

.role-content {
  min-width: 0;
  background: var(--tql-color-white);
}

.role-header {
  display: flex;
  min-height: 78px;
  align-items: center;
  justify-content: space-between;
  padding: 14px 20px;
  border-bottom: 1px solid var(--tql-border);
}

.role-header h2 {
  margin: 0 0 4px;
  font-size: 16px;
  font-weight: 600;
  line-height: 24px;
}

.role-header p {
  margin: 0;
  color: var(--tql-text-tertiary);
  font-size: 12px;
}

.role-form {
  padding: 16px 20px 24px;
}

.role-basic {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 0 16px;
  max-width: 1120px;
}

.role-description {
  grid-column: span 3;
}

.role-tabs {
  margin-top: 2px;
}

.permission-actions,
.user-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 48px;
}

.permission-actions {
  color: var(--tql-text-secondary);
}

.permission-actions strong {
  color: rgb(var(--primary-6));
}

.permission-matrix {
  overflow: hidden;
  border: 1px solid var(--tql-border);
  border-radius: 4px;
}

.permission-matrix :deep(.arco-collapse-item-header) {
  min-height: 42px;
  padding: 8px 14px 8px 38px;
  font-weight: 500;
  background: var(--tql-bg-subtle);
  border-bottom: 1px solid var(--tql-border);
}

.permission-matrix :deep(.arco-collapse-item-icon-hover) {
  left: 14px;
  display: inline-flex;
  width: 18px;
  height: 18px;
  align-items: center;
  justify-content: center;
}

.permission-matrix :deep(.arco-collapse-item-header-title) {
  min-width: 0;
  padding-right: 16px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.permission-matrix :deep(.arco-collapse-item-header-extra) {
  flex: 0 0 auto;
}

.permission-matrix :deep(.arco-collapse-item-content) {
  padding: 0;
  background: var(--tql-color-white);
}

.permission-table-head,
.permission-table-row {
  display: grid;
  grid-template-columns: 190px minmax(0, 1fr);
}

.permission-table-head {
  min-height: 38px;
  align-items: center;
  color: var(--tql-text-secondary);
  font-size: 12px;
  background: var(--tql-bg-muted);
  border-bottom: 1px solid var(--tql-border);
}

.permission-table-head span,
.permission-menu-cell,
.permission-buttons-cell {
  padding: 9px 14px;
}

.permission-table-row {
  min-height: 44px;
  border-bottom: 1px solid var(--tql-border);
}

.permission-table-row:last-child {
  border-bottom: 0;
}

.permission-menu-cell {
  border-right: 1px solid var(--tql-border);
}

.permission-buttons-cell {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px 20px;
}

.permission-empty {
  color: var(--tql-text-tertiary);
  font-size: 12px;
}

.user-search {
  width: 280px;
}

.assign-user-table {
  margin-top: 16px;
}

@media (max-width: 1100px) {
  .role-workbench {
    grid-template-columns: 240px minmax(0, 1fr);
  }

  .role-basic {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .role-description {
    grid-column: span 2;
  }
}
</style>
