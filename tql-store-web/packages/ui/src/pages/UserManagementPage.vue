<template>
  <div class="container">
    <div class="user-layout">
      <a-card class="organization-card">
        <OrganizationTree v-model="query.organizationId" :options="organizations" :loading="optionsLoading" @change="search" />
      </a-card>
      <main class="user-main">
    <a-card class="search-card tql-search-card">
      <div class="search-layout tql-search-layout">
        <a-form class="search-form tql-search-form" :model="query" label-align="left">
          <div class="search-fields tql-search-fields">
            <a-form-item field="keyword" label="关键词">
              <a-input v-model="query.keyword" placeholder="账号、工号、姓名或手机号" allow-clear @press-enter="search" />
            </a-form-item>
            <a-form-item v-if="isMerchant" field="storeId" label="所属门店">
              <StoreSelector v-model="query.storeId" :options="stores" placeholder="全部门店" />
            </a-form-item>
            <a-form-item field="status" label="状态">
              <a-select v-model="query.status" placeholder="全部状态" allow-clear>
                <a-option :value="1">启用</a-option>
                <a-option :value="0">停用</a-option>
              </a-select>
            </a-form-item>
          </div>
        </a-form>
        <a-divider class="tql-search-divider" direction="vertical" />
        <div class="search-actions tql-search-actions">
          <a-space>
            <a-button type="primary" @click="search"><template #icon><IconSearch /></template>查询</a-button>
            <a-button @click="reset"><template #icon><IconRefresh /></template>重置</a-button>
          </a-space>
        </div>
      </div>
    </a-card>

    <a-card class="general-card tql-list-card">
      <a-row class="table-toolbar">
        <a-col :span="12">
          <a-space>
            <a-button type="primary" @click="openCreate"><template #icon><IconPlus /></template>新建用户</a-button>
            <span class="record-total">共 {{ total }} 条</span>
          </a-space>
        </a-col>
        <a-col :span="12" class="table-actions">
          <a-button type="text" @click="load"><template #icon><IconRefresh /></template>刷新</a-button>
        </a-col>
      </a-row>

      <a-table class="user-table" row-key="id" :columns="columns" :data="records" :loading="loading" :pagination="false" :scroll="{ x: isMerchant ? 1500 : 1100, y: 'calc(100vh - 340px)' }">
        <template #account="{ record }">
          <div class="account-cell">
            <a-avatar class="account-avatar" :size="32">{{ record.displayName.slice(0, 1) }}</a-avatar>
            <div class="account-details">
              <strong>{{ record.displayName }}</strong>
              <span class="account-subtitle">{{ record.username || record.employeeNumber || '未开通账号' }}</span>
            </div>
          </div>
        </template>
        <template #sourceType="{ record }">
          <a-tag :color="record.sourceType === 'HR_BUTLER' ? 'arcoblue' : 'gray'">
            {{ record.sourceType === 'HR_BUTLER' ? '人力管家' : '本地' }}
          </a-tag>
        </template>
        <template #roles="{ record }">
          <a-space wrap><a-tag v-for="role in record.roleNames" :key="role">{{ role }}</a-tag><span v-if="!record.roleNames.length">—</span></a-space>
        </template>
        <template #dataScope="{ record }">
          <a-tag color="arcoblue">{{ dataScopeLabel(record.dataScope) }}</a-tag>
        </template>
        <template #status="{ record }">
          <a-badge :status="record.status === 1 ? 'success' : 'normal'" :text="record.status === 1 ? '启用' : '停用'" />
        </template>
        <template #loginEnabled="{ record }">
          <a-badge
            :status="record.loginEnabled ? 'success' : 'normal'"
            :text="record.loginEnabled ? '已开通' : '未开通'"
          />
        </template>
        <template #actions="{ record }">
          <a-space>
            <a-link @click="openEdit(record.id)">编辑</a-link>
            <a-popconfirm v-if="record.sourceType !== 'HR_BUTLER'" content="确定删除该用户吗？" @ok="remove(record.id)">
              <a-link status="danger">删除</a-link>
            </a-popconfirm>
          </a-space>
        </template>
      </a-table>

      <div class="pagination-row">
        <a-pagination
          v-model:current="query.page"
          v-model:page-size="query.pageSize"
          :total="total"
          show-total
          show-more
          show-jumper
          show-page-size
          :page-size-options="[10, 20, 50, 100]"
          @change="load"
          @page-size-change="handlePageSizeChange"
        />
      </div>
    </a-card>
      </main>
    </div>

    <a-drawer v-model:visible="modalVisible" :title="editingId ? '编辑用户' : '新建用户'" :width="800" :mask-closable="false" :esc-to-close="false" unmount-on-close>
      <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
        <a-row :gutter="16">
          <template v-if="isMerchant && editingId">
            <a-col :span="12">
              <a-form-item label="数据来源">
                <a-input :model-value="form.sourceType === 'HR_BUTLER' ? '人力管家' : '本地创建'" disabled />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="所属组织">
                <a-input :model-value="form.organizationName || '—'" disabled />
              </a-form-item>
            </a-col>
            <a-col v-if="form.employeeNumber" :span="12">
              <a-form-item label="员工工号">
                <a-input :model-value="form.employeeNumber" disabled />
              </a-form-item>
            </a-col>
          </template>
          <a-col v-if="isMerchant" :span="12">
            <a-form-item field="loginEnabled" label="系统登录">
              <a-switch v-model="form.loginEnabled" checked-text="开通" unchecked-text="关闭" />
            </a-form-item>
          </a-col>
          <a-col v-if="!isMerchant || form.loginEnabled" :span="12">
            <a-form-item field="username" label="登录账号">
              <a-input v-model="form.username" placeholder="请输入登录账号" :max-length="64" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item field="displayName" label="用户姓名">
              <a-input v-model="form.displayName" placeholder="请输入用户姓名" :max-length="64" />
            </a-form-item>
          </a-col>
          <a-col v-if="!isMerchant || form.loginEnabled" :span="12">
            <a-form-item field="password" :label="editingId ? '登录密码（不修改请留空）' : '登录密码'">
              <a-input-password v-model="form.password" placeholder="8-64 位密码" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item field="status" label="状态">
              <a-radio-group v-model="form.status"><a-radio :value="1">启用</a-radio><a-radio :value="0">停用</a-radio></a-radio-group>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item field="phone" label="手机号">
              <a-input v-model="form.phone" placeholder="请输入手机号" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item field="email" label="邮箱">
              <a-input v-model="form.email" placeholder="请输入邮箱" />
            </a-form-item>
          </a-col>
          <a-col v-if="!isMerchant || form.loginEnabled" :span="24">
            <a-form-item field="roleIds" label="所属角色">
              <a-select v-model="form.roleIds" multiple placeholder="请选择角色" allow-search>
                <a-option v-for="role in roleOptions" :key="role.id" :value="role.id" :disabled="role.status !== 1">{{ role.name }}</a-option>
              </a-select>
            </a-form-item>
          </a-col>
          <template v-if="isMerchant && form.loginEnabled">
            <a-col :span="12">
              <a-form-item field="primaryStoreId" label="所属主门店">
                <StoreSelector v-model="form.primaryStoreId" :options="stores" placeholder="请选择主门店" />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item field="dataScope" label="数据权限">
                <DataScopeSelector v-model="form.dataScope" :client-type="config.clientType" />
              </a-form-item>
            </a-col>
            <a-col :span="24">
              <a-form-item field="storeIds" label="授权门店" extra="主门店会自动加入；自定义数据权限以此门店集合为准">
                <StoreSelector v-model="form.storeIds" :options="stores" multiple placeholder="请选择授权门店" />
              </a-form-item>
            </a-col>
          </template>
          <a-col v-else :span="12">
            <a-form-item field="dataScope" label="数据权限">
              <DataScopeSelector v-model="form.dataScope" :client-type="config.clientType" />
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>
      <template #footer><div class="drawer-footer">
        <a-space><a-button @click="modalVisible = false">取消</a-button><a-button type="primary" :loading="saving" @click="submit">保存</a-button></a-space>
      </div></template>
    </a-drawer>
  </div>
</template>

<script setup lang="ts">
import { computed, inject, onMounted, reactive, ref } from 'vue';
import { Message, type FormInstance, type TableColumnData } from '@arco-design/web-vue';
import { IconPlus, IconRefresh, IconSearch } from '@arco-design/web-vue/es/icon';
import { createUser, deleteUser, fetchOrganizations, fetchRoles, fetchStores, fetchUser, fetchUsers, updateUser } from '@tql-store/api';
import type { DataScope, OrganizationOption, RoleItem, StoreOption, UserDetail, UserItem, UserSavePayload } from '@tql-store/shared';
import { APP_CONFIG_KEY } from '../context';
import DataScopeSelector from '../business/DataScopeSelector.vue';
import OrganizationTree from '../business/OrganizationTree.vue';
import StoreSelector from '../business/StoreSelector.vue';

const config = inject(APP_CONFIG_KEY)!;
const isMerchant = computed(() => config.clientType === 'MERCHANT');
const loading = ref(false);
const optionsLoading = ref(false);
const saving = ref(false);
const modalVisible = ref(false);
const editingId = ref<number>();
const formRef = ref<FormInstance>();
const records = ref<UserItem[]>([]);
const roleOptions = ref<RoleItem[]>([]);
const stores = ref<StoreOption[]>([]);
const organizations = ref<OrganizationOption[]>([]);
const total = ref(0);
const originalLoginEnabled = ref(false);
const query = reactive<{ keyword: string; status?: number; storeId?: number; organizationId?: number; page: number; pageSize: number }>({ keyword: '', page: 1, pageSize: 10 });
type UserForm = UserSavePayload & Pick<UserDetail, 'employeeNumber' | 'sourceType' | 'organizationName'>;
const emptyForm = (): UserForm => ({
  username: '', password: '', loginEnabled: true, displayName: '', email: '', phone: '',
  status: 1, dataScope: isMerchant.value ? 'STORE' : 'ALL', roleIds: [], storeIds: [],
  employeeNumber: undefined, sourceType: 'LOCAL', organizationName: undefined
});
const form = reactive<UserForm>(emptyForm());

const merchantDataScopes: { value: DataScope; label: string }[] = [
  { value: 'ALL', label: '全部门店' },
  { value: 'STORE_AND_CHILD', label: '本门店及下级门店' },
  { value: 'STORE', label: '本门店' },
  { value: 'CUSTOM', label: '自定义门店' },
  { value: 'SELF', label: '仅本人数据' }
];
const scopeMap = Object.fromEntries(merchantDataScopes.map(item => [item.value, item.label])) as Record<string, string>;
const dataScopeLabel = (scope: DataScope) => scopeMap[scope] || (scope === 'ALL' ? '全部数据' : scope);
const rules = computed(() => ({
  username: form.loginEnabled ? [{ required: true, message: '请输入登录账号' }] : [],
  displayName: [{ required: true, message: '请输入用户姓名' }],
  password: form.loginEnabled && (!editingId.value || !originalLoginEnabled.value)
    ? [{ required: true, message: '请输入登录密码' }, { minLength: 8, message: '密码至少 8 位' }]
    : [],
  roleIds: form.loginEnabled ? [{ required: true, message: '请选择所属角色' }] : [],
  primaryStoreId: isMerchant.value && form.loginEnabled ? [{ required: true, message: '请选择所属主门店' }] : []
}));
const columns = computed<TableColumnData[]>(() => [
  { title: '用户', slotName: 'account', width: 190 },
  ...(isMerchant.value ? [
    { title: '所属组织', dataIndex: 'organizationName', width: 180 },
    { title: '数据来源', slotName: 'sourceType', width: 110 }
  ] : []),
  { title: '手机号', dataIndex: 'phone', width: 140 },
  ...(isMerchant.value ? [{ title: '所属主门店', dataIndex: 'primaryStoreName', width: 180 }] : []),
  { title: '角色', slotName: 'roles', width: 220 },
  { title: '数据权限', slotName: 'dataScope', width: 150 },
  ...(isMerchant.value ? [{ title: '登录权限', slotName: 'loginEnabled', width: 110 }] : []),
  { title: '人员状态', slotName: 'status', width: 100 },
  { title: '操作', slotName: 'actions', width: 120, fixed: 'right' }
]);

onMounted(async () => {
  await Promise.all([loadOptions(), load()]);
});

async function loadOptions() {
  optionsLoading.value = true;
  try {
    const [roles, storeOptions, organizationOptions] = await Promise.all([
      fetchRoles(), isMerchant.value ? fetchStores() : Promise.resolve([]), fetchOrganizations()
    ]);
    roleOptions.value = roles;
    stores.value = storeOptions;
    organizations.value = buildOrganizationTree(organizationOptions);
  } catch (error) {
    Message.error(error instanceof Error ? error.message : '用户选项加载失败');
  } finally {
    optionsLoading.value = false;
  }
}

function buildOrganizationTree(options: OrganizationOption[]): OrganizationOption[] {
  const map = new Map(options.map(option => [option.id, { ...option, children: [] as OrganizationOption[] }]));
  const roots: OrganizationOption[] = [];
  for (const node of map.values()) {
    const parent = node.parentId ? map.get(node.parentId) : undefined;
    if (parent) parent.children!.push(node); else roots.push(node);
  }
  return roots;
}

async function load() {
  loading.value = true;
  try {
    const result = await fetchUsers(query);
    records.value = result.records;
    total.value = result.total;
  } catch (error) {
    Message.error(error instanceof Error ? error.message : '用户列表加载失败');
  } finally {
    loading.value = false;
  }
}

function search() { query.page = 1; load(); }
function reset() { query.keyword = ''; query.status = undefined; query.storeId = undefined; query.organizationId = undefined; query.page = 1; load(); }
function handlePageSizeChange(size: number) { query.pageSize = size; query.page = 1; load(); }
function resetForm() { Object.assign(form, emptyForm()); editingId.value = undefined; originalLoginEnabled.value = false; formRef.value?.clearValidate(); }
function openCreate() { resetForm(); modalVisible.value = true; }

async function openEdit(id: number) {
  resetForm();
  try {
    const detail = await fetchUser(id);
    editingId.value = id;
    originalLoginEnabled.value = detail.loginEnabled;
    Object.assign(form, detail, { password: '' });
    modalVisible.value = true;
  } catch (error) {
    Message.error(error instanceof Error ? error.message : '用户信息加载失败');
  }
}

async function submit() {
  const errors = await formRef.value?.validate();
  if (errors) return;
  saving.value = true;
  try {
    const payload: UserSavePayload = {
      username: form.loginEnabled ? form.username : undefined,
      password: form.password,
      loginEnabled: form.loginEnabled,
      organizationId: form.organizationId,
      displayName: form.displayName,
      email: form.email,
      phone: form.phone,
      status: form.status,
      dataScope: form.dataScope,
      primaryStoreId: form.loginEnabled ? form.primaryStoreId : undefined,
      roleIds: form.loginEnabled ? [...form.roleIds] : [],
      storeIds: form.loginEnabled ? [...form.storeIds] : []
    };
    if (!payload.password) delete payload.password;
    if (editingId.value) await updateUser(editingId.value, payload);
    else await createUser(payload);
    Message.success(editingId.value ? '用户已更新' : '用户已创建');
    modalVisible.value = false;
    await load();
  } catch (error) {
    Message.error(error instanceof Error ? error.message : '用户保存失败');
  } finally {
    saving.value = false;
  }
}

async function remove(id: number) {
  try { await deleteUser(id); Message.success('用户已删除'); await load(); }
  catch (error) { Message.error(error instanceof Error ? error.message : '用户删除失败'); }
}
</script>

<style scoped>
.container { width: 100%; padding: var(--tql-page-padding); }
.user-layout { display: grid; height: calc(100vh - 92px); min-height: 0; grid-template-columns: var(--tql-tree-width) minmax(0, 1fr); gap: var(--tql-card-gap); }
.user-main { display: flex; min-width: 0; min-height: 0; flex-direction: column; }
.organization-card, .search-card, .general-card { border-color: var(--tql-border); border-radius: var(--tql-radius-card); box-shadow: none; }
.organization-card { height: 100%; min-height: 0; }
.organization-card :deep(.arco-card-body) { height: 100%; min-height: 0; padding: var(--tql-card-padding); }
.organization-card :deep(.organization-tree) { height: 100%; }
.organization-card :deep(.tree-loading) { flex: 1; }
.general-card { min-height: 0; flex: 1; border-color: var(--tql-border); }
.general-card :deep(.arco-card-body) { display: flex; height: 100%; min-height: 0; flex-direction: column; }
.table-toolbar { margin-bottom: 16px; }
.table-actions { display: flex; align-items: center; justify-content: flex-end; }
.record-total { color: var(--tql-text-tertiary); font-size: 12px; }
.account-cell { display: flex; align-items: center; gap: 10px; }
.account-avatar {
  flex: 0 0 32px;
  justify-content: center;
  color: var(--tql-primary);
  background: var(--tql-primary-soft);
  font-weight: 600;
}
.account-details { display: flex; min-width: 0; flex-direction: column; gap: 3px; }
.account-details strong { color: var(--tql-text-primary); font-weight: 500; }
.account-subtitle { color: var(--tql-text-tertiary); font-size: 12px; }
.user-table { min-height: 0; flex: 1; }
.pagination-row { display: flex; flex: 0 0 auto; justify-content: flex-end; margin-top: var(--tql-space-3); padding-top: var(--tql-space-3); background: var(--tql-bg-card); border-top: 1px solid var(--tql-border-light); }
.drawer-footer { display: flex; justify-content: flex-end; }
:deep(.arco-table-th) { background: var(--tql-bg-subtle); }
:deep(.arco-table-td), :deep(.arco-table-th) { height: var(--tql-table-row-height); }
</style>
