<template>
  <div class="container">
    <a-card class="role-config-card">
      <div class="role-config-layout">
        <aside class="role-sidebar">
          <div class="role-search-row">
            <a-input-search v-model="query.keyword" placeholder="搜索角色" allow-clear @search="load" @press-enter="load" />
            <a-tooltip content="新建角色">
              <a-button class="add-role-button" type="primary" shape="circle" aria-label="新建角色" @click="openCreate">
                <template #icon><IconPlus /></template>
              </a-button>
            </a-tooltip>
          </div>
          <a-spin :loading="loading" class="role-list-loading">
            <div v-if="records.length" class="role-list">
              <button
                v-for="role in records"
                :key="role.id"
                type="button"
                class="role-item"
                :class="{ 'role-item--active': role.id === editingId }"
                @click="openEdit(role)"
              >
                <span class="role-item-copy"><strong>{{ role.name }}</strong><small>{{ role.code }}</small></span>
                <a-badge :status="role.status === 1 ? 'success' : 'normal'" />
              </button>
            </div>
            <a-empty v-else description="暂无角色" />
          </a-spin>
        </aside>

        <main class="role-editor">
          <template v-if="editorVisible">
            <div class="editor-toolbar">
              <div>
                <strong>{{ editingId ? form.roleName || '编辑角色' : '新建角色' }}</strong>
                <span>{{ editingId ? '修改角色信息与菜单权限' : '创建角色并分配菜单权限' }}</span>
              </div>
              <a-space>
                <a-popconfirm
                  v-if="activeRole"
                  :content="`确定删除角色“${activeRole.name}”吗？`"
                  @ok="remove(activeRole.id)"
                >
                  <a-button status="danger" :disabled="activeRole.userCount > 0">删除</a-button>
                </a-popconfirm>
                <a-button type="primary" :loading="saving" @click="submit">保存</a-button>
              </a-space>
            </div>

            <a-form ref="formRef" :model="form" :rules="rules" layout="vertical" class="role-form">
              <a-row :gutter="16">
                <a-col :span="12"><a-form-item field="roleName" label="角色名称"><a-input v-model="form.roleName" placeholder="请输入角色名称" /></a-form-item></a-col>
                <a-col :span="12"><a-form-item field="roleCode" label="角色编码"><a-input v-model="form.roleCode" placeholder="如 STORE_MANAGER" /></a-form-item></a-col>
                <a-col :span="12"><a-form-item field="status" label="状态"><a-radio-group v-model="form.status"><a-radio :value="1">启用</a-radio><a-radio :value="0">停用</a-radio></a-radio-group></a-form-item></a-col>
                <a-col :span="24"><a-form-item field="remark" label="角色说明"><a-textarea v-model="form.remark" placeholder="说明该角色的业务职责" :max-length="255" show-word-limit /></a-form-item></a-col>
              </a-row>

              <a-tabs default-active-key="menus" class="permission-tabs">
                <a-tab-pane key="menus" title="功能权限">
                  <div class="permission-toolbar">
                    <span>已选择 {{ form.menuIds.length }} 项</span>
                    <a-space><a-link @click="selectAll">全选</a-link><a-link @click="form.menuIds = []">清空</a-link></a-space>
                  </div>
                  <a-checkbox-group v-model="form.menuIds" class="permission-grid">
                    <a-grid :cols="3" :col-gap="16" :row-gap="12">
                      <a-grid-item v-for="menu in menus" :key="menu.id"><a-checkbox :value="menu.id">{{ menu.name }}</a-checkbox></a-grid-item>
                    </a-grid>
                  </a-checkbox-group>
                </a-tab-pane>
                <a-tab-pane key="data" title="数据权限" disabled />
                <a-tab-pane key="fields" title="字段权限" disabled />
              </a-tabs>
            </a-form>
          </template>
          <a-empty v-else description="选择左侧角色开始配置">
            <template #extra><a-button type="primary" @click="openCreate">新建角色</a-button></template>
          </a-empty>
        </main>
      </div>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { Message, type FormInstance } from '@arco-design/web-vue';
import { IconPlus } from '@arco-design/web-vue/es/icon';
import { createRole, deleteRole, fetchAssignableMenus, fetchRoles, updateRole } from '@tql-store/api';
import type { MenuItem, RoleItem, RoleSavePayload } from '@tql-store/shared';

const loading = ref(false);
const saving = ref(false);
const editorVisible = ref(false);
const editingId = ref<number>();
const formRef = ref<FormInstance>();
const records = ref<RoleItem[]>([]);
const menus = ref<MenuItem[]>([]);
const query = reactive<{ keyword: string }>({ keyword: '' });
const emptyForm = (): RoleSavePayload => ({ roleCode: '', roleName: '', status: 1, remark: '', menuIds: [] });
const form = reactive<RoleSavePayload>(emptyForm());
const activeRole = computed(() => records.value.find(role => role.id === editingId.value));
const rules = {
  roleName: [{ required: true, message: '请输入角色名称' }],
  roleCode: [{ required: true, message: '请输入角色编码' }, { match: /^[A-Z][A-Z0-9_]*$/, message: '仅支持大写字母、数字和下划线' }]
};

onMounted(async () => {
  await Promise.all([load(), loadMenus()]);
  if (records.value[0]) openEdit(records.value[0]);
});

async function load() {
  loading.value = true;
  try { records.value = await fetchRoles(query); }
  catch (error) { Message.error(error instanceof Error ? error.message : '角色列表加载失败'); }
  finally { loading.value = false; }
}

async function loadMenus() {
  try { menus.value = await fetchAssignableMenus(); }
  catch (error) { Message.error(error instanceof Error ? error.message : '菜单权限加载失败'); }
}

function resetForm() { Object.assign(form, emptyForm()); editingId.value = undefined; formRef.value?.clearValidate(); }
function openCreate() { resetForm(); editorVisible.value = true; }
function openEdit(role: RoleItem) {
  editingId.value = role.id;
  Object.assign(form, { roleCode: role.code, roleName: role.name, status: role.status, remark: role.remark || '', menuIds: [...role.menuIds] });
  editorVisible.value = true;
}
function selectAll() { form.menuIds = menus.value.map(menu => menu.id); }

async function submit() {
  const errors = await formRef.value?.validate();
  if (errors) return;
  saving.value = true;
  try {
    const payload = { ...form, roleCode: form.roleCode.trim().toUpperCase(), menuIds: [...form.menuIds] };
    if (editingId.value) await updateRole(editingId.value, payload); else await createRole(payload);
    Message.success(editingId.value ? '角色已更新' : '角色已创建');
    const savedCode = payload.roleCode;
    await load();
    const saved = records.value.find(role => role.code === savedCode);
    if (saved) openEdit(saved);
  } catch (error) { Message.error(error instanceof Error ? error.message : '角色保存失败'); }
  finally { saving.value = false; }
}

async function remove(id: number) {
  try {
    await deleteRole(id);
    Message.success('角色已删除');
    editorVisible.value = false;
    editingId.value = undefined;
    await load();
    if (records.value[0]) openEdit(records.value[0]);
  } catch (error) { Message.error(error instanceof Error ? error.message : '角色删除失败'); }
}
</script>

<style scoped>
.container { width: 100%; padding: var(--tql-page-padding); }
.role-config-card { min-height: calc(100vh - 92px); border-color: var(--tql-border); border-radius: var(--tql-radius-card); box-shadow: none; }
.role-config-card :deep(.arco-card-body) { padding: 0; }
.role-config-layout { display: grid; grid-template-columns: var(--tql-tree-width) minmax(0, 1fr); min-height: calc(100vh - 94px); }
.role-sidebar { padding: var(--tql-card-padding); border-right: 1px solid var(--tql-border); }
.role-search-row { display: flex; gap: var(--tql-space-2); }
.add-role-button { flex: 0 0 32px; }
.role-list-loading { display: block; margin-top: var(--tql-space-3); }
.role-list { display: flex; flex-direction: column; gap: var(--tql-space-1); }
.role-item { display: flex; width: 100%; align-items: center; justify-content: space-between; padding: var(--tql-space-2) var(--tql-space-3); color: var(--tql-text-primary); background: transparent; border: 0; border-radius: var(--tql-radius-control); cursor: pointer; text-align: left; }
.role-item:hover { background: var(--tql-bg-subtle); }
.role-item--active { color: var(--tql-primary); background: var(--tql-primary-soft); }
.role-item-copy { display: flex; min-width: 0; flex-direction: column; gap: var(--tql-space-1); }
.role-item-copy strong, .role-item-copy small { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.role-item-copy strong { font-weight: 500; }
.role-item-copy small { color: var(--tql-text-tertiary); }
.role-editor { min-width: 0; padding: var(--tql-card-padding); }
.editor-toolbar { display: flex; align-items: flex-start; justify-content: space-between; padding-bottom: var(--tql-space-4); border-bottom: 1px solid var(--tql-border); }
.editor-toolbar > div { display: flex; flex-direction: column; gap: var(--tql-space-1); }
.editor-toolbar strong { font-size: 16px; }
.editor-toolbar span { color: var(--tql-text-tertiary); font-size: 12px; }
.role-form { max-width: 960px; padding-top: var(--tql-space-4); }
.permission-tabs { margin-top: var(--tql-space-2); }
.permission-toolbar { display: flex; align-items: center; justify-content: space-between; margin-bottom: var(--tql-space-4); color: var(--tql-text-secondary); }
.permission-grid { display: block; padding: var(--tql-space-4); background: var(--tql-bg-subtle); border: 1px solid var(--tql-border); border-radius: var(--tql-radius-control); }
</style>
