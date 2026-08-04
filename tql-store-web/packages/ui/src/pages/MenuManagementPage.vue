<template>
  <div class="container">
    <a-card class="search-card tql-search-card">
      <div class="search-layout tql-search-layout">
        <a-form class="search-form tql-search-form" :model="filters" label-align="left">
          <div class="search-fields tql-search-fields">
            <a-form-item label="所属商家">
              <a-select v-model="selectedTenantId" placeholder="请选择商家" @change="loadMenus">
                <a-option v-for="merchant in merchants" :key="merchant.id" :value="merchant.id">
                  {{ merchant.name }}（{{ merchant.code }}）
                </a-option>
              </a-select>
            </a-form-item>
            <a-form-item label="菜单名称">
              <a-input v-model="filters.keyword" allow-clear placeholder="菜单名称" @press-enter="applyFilters" />
            </a-form-item>
            <a-form-item label="菜单状态">
              <a-select v-model="filters.status" allow-clear placeholder="全部状态">
                <a-option :value="1">启用</a-option>
                <a-option :value="0">停用</a-option>
              </a-select>
            </a-form-item>
            <a-form-item label="显示状态">
              <a-select v-model="filters.visible" allow-clear placeholder="全部状态">
                <a-option :value="1">正常</a-option>
                <a-option :value="0">隐藏</a-option>
              </a-select>
            </a-form-item>
          </div>
        </a-form>
        <a-divider class="tql-search-divider" direction="vertical" />
        <div class="search-actions tql-search-actions">
          <a-space>
            <a-button type="primary" @click="applyFilters"><template #icon><IconSearch /></template>查询</a-button>
            <a-button @click="resetFilters"><template #icon><IconRefresh /></template>重置</a-button>
          </a-space>
        </div>
      </div>
    </a-card>

    <a-card class="general-card tql-list-card">
      <div class="table-toolbar">
        <a-space>
          <a-button v-if="can('platform:system:menu:create')" type="primary" :disabled="!selectedTenantId" @click="openCreate('DIRECTORY', 0)">
            <template #icon><IconPlus /></template>新增一级目录
          </a-button>
          <span class="record-total">共 {{ filteredFlatCount }} 个节点</span>
        </a-space>
        <a-space>
          <a-button type="text" @click="expandAll = true">展开全部</a-button>
          <a-button type="text" @click="expandAll = false">收起全部</a-button>
          <a-tooltip content="刷新">
            <a-button type="text" aria-label="刷新" @click="loadMenus"><template #icon><IconRefresh /></template></a-button>
          </a-tooltip>
        </a-space>
      </div>

      <a-table
        class="menu-table"
        :columns="columns"
        :data="filteredTree"
        :loading="loading"
        :pagination="false"
        row-key="id"
        :default-expand-all-rows="expandAll"
        :key="String(expandAll) + refreshKey"
        :bordered="{ cell: false }"
        :scroll="{ x: 1080 }"
      >
        <template #name="{ record }">
          <div class="menu-name">
            <span class="type-icon">
              <MenuIcon v-if="record.type !== 'BUTTON'" :code="record.icon" :svg="record.iconSvg" />
              <IconThunderbolt v-else />
            </span>
            <span>{{ record.name }}</span>
            <a-tag v-if="record.systemBuiltin" size="small" color="arcoblue">系统</a-tag>
          </div>
        </template>
        <template #type="{ record }">
          <a-tag color="gray">{{ typeLabels[record.type as MenuType] }}</a-tag>
        </template>
        <template #route="{ record }">
          <span v-if="record.path" class="code-text">{{ record.path }}</span>
          <span v-else class="empty-text">—</span>
        </template>
        <template #component="{ record }">
          <span v-if="record.componentKey" class="code-text">{{ record.componentKey }}</span>
          <span v-else class="empty-text">—</span>
        </template>
        <template #permission="{ record }">
          <span v-if="record.permission" class="code-text">{{ record.permission }}</span>
          <span v-else class="empty-text">—</span>
        </template>
        <template #status="{ record }">
          <a-badge :status="record.status === 1 ? 'success' : 'normal'" :text="record.status === 1 ? '启用' : '停用'" />
        </template>
        <template #visible="{ record }">
          <span v-if="record.type === 'BUTTON'" class="empty-text">—</span>
          <a-badge v-else :status="record.visible === 1 ? 'success' : 'normal'" :text="record.visible === 1 ? '正常' : '隐藏'" />
        </template>
        <template #actions="{ record }">
          <a-space>
            <a-link v-if="can('platform:system:menu:update')" :disabled="record.systemBuiltin" @click="openEdit(record)">编辑</a-link>
            <a-dropdown
              v-if="!record.systemBuiltin"
              @select="(value) => handleRowAction(record, value as string)"
            >
              <a-link>更多</a-link>
              <template #content>
                <a-doption v-if="can('platform:system:menu:create') && record.type === 'DIRECTORY'" value="create-directory">新增目录</a-doption>
                <a-doption v-if="can('platform:system:menu:create') && record.type === 'DIRECTORY'" value="create-menu">新增菜单</a-doption>
                <a-doption v-if="can('platform:system:menu:create') && record.type === 'MENU'" value="create-button">新增按钮</a-doption>
                <a-doption v-if="can('platform:system:menu:update')" value="toggle-status">{{ record.status === 1 ? '停用' : '启用' }}</a-doption>
                <a-doption v-if="can('platform:system:menu:update') && record.type !== 'BUTTON'" value="toggle-visible">{{ record.visible === 1 ? '隐藏' : '显示' }}</a-doption>
                <a-doption v-if="can('platform:system:menu:delete')" value="delete">删除</a-doption>
              </template>
            </a-dropdown>
          </a-space>
        </template>
        <template #empty>
          <a-empty :description="selectedTenantId ? '暂无菜单数据' : '请先选择商家'" />
        </template>
      </a-table>
    </a-card>

    <a-drawer
      v-model:visible="drawerVisible"
      :width="640"
      :title="editingId ? '编辑菜单节点' : '新增菜单节点'"
      :mask-closable="false"
      :esc-to-close="false"
      unmount-on-close
      @before-cancel="beforeClose"
    >
      <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item field="type" label="节点类型">
              <a-select v-model="form.type" :disabled="Boolean(editingId)" @change="normalizeFormByType">
                <a-option value="DIRECTORY">目录</a-option>
                <a-option value="MENU">菜单</a-option>
                <a-option value="BUTTON">按钮</a-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item field="parentId" label="上级节点">
              <a-tree-select
                v-model="form.parentId"
                :data="parentOptions"
                allow-clear
                placeholder="顶级节点"
              />
            </a-form-item>
          </a-col>
        </a-row>

        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item field="name" label="名称">
              <a-input v-model="form.name" :max-length="64" show-word-limit placeholder="请输入名称" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item field="order" label="排序">
              <a-input-number v-model="form.order" :min="0" :max="9999" class="full-width" />
            </a-form-item>
          </a-col>
        </a-row>

        <template v-if="form.type !== 'BUTTON'">
          <a-row :gutter="16">
            <a-col :span="12">
              <a-form-item field="path" label="路由地址">
                <a-input v-model="form.path" placeholder="/system/example" />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item field="iconId" label="菜单图标">
                <a-select v-model="form.iconId" allow-clear placeholder="请选择图标" @change="syncIconCode">
                  <a-option v-for="icon in enabledIcons" :key="icon.id" :value="icon.id">
                    <a-space><MenuIcon :code="icon.code" :svg="icon.svgContent" />{{ icon.name }}</a-space>
                  </a-option>
                </a-select>
                <a-link class="icon-maintain" @click="router.push('/system/icons')">维护图标</a-link>
              </a-form-item>
            </a-col>
          </a-row>
        </template>

        <template v-if="form.type === 'MENU'">
          <a-row :gutter="16">
            <a-col :span="12">
              <a-form-item field="routeName" label="路由名称">
                <a-input v-model="form.routeName" placeholder="MerchantExample" />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item field="componentKey" label="页面组件">
                <a-select v-model="form.componentKey" placeholder="请选择已注册组件">
                  <a-option v-for="component in componentOptions" :key="component" :value="component">
                    {{ component }}
                  </a-option>
                </a-select>
              </a-form-item>
            </a-col>
          </a-row>
        </template>

        <a-form-item v-if="form.type !== 'DIRECTORY'" field="permission" label="权限编码">
          <a-input v-model="form.permission" placeholder="merchant:module:resource:action" />
        </a-form-item>

        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item field="status" label="菜单状态">
              <a-radio-group v-model="form.status">
                <a-radio :value="1">启用</a-radio>
                <a-radio :value="0">停用</a-radio>
              </a-radio-group>
            </a-form-item>
          </a-col>
          <a-col v-if="form.type !== 'BUTTON'" :span="12">
            <a-form-item field="visible" label="显示状态">
              <a-radio-group v-model="form.visible">
                <a-radio :value="1">正常</a-radio>
                <a-radio :value="0">隐藏</a-radio>
              </a-radio-group>
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>
      <template #footer>
        <div class="drawer-footer">
          <a-space>
            <a-button @click="drawerVisible = false">取消</a-button>
            <a-button type="primary" :loading="saving" @click="submitDrawer">保存</a-button>
          </a-space>
        </div>
      </template>
    </a-drawer>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue';
import { useRouter } from 'vue-router';
import { Message, type FormInstance, type TableColumnData } from '@arco-design/web-vue';
import { IconPlus, IconRefresh, IconSearch, IconThunderbolt } from '@arco-design/web-vue/es/icon';
import {
  createMerchantMenu,
  deleteMerchantMenu,
  fetchMerchantMenu,
  fetchMerchantMenus, fetchIcons,
  fetchMerchants,
  updateMerchantMenu,
  updateMerchantMenuStatus,
  updateMerchantMenuVisibility
} from '@tql-store/api';
import type { IconItem, MerchantMenuItem, MerchantMenuSavePayload, MerchantOption } from '@tql-store/shared';
import { usePermission } from '@tql-store/auth';
import MenuIcon from '../components/MenuIcon.vue';

type MenuType = MerchantMenuItem['type'];
type MenuForm = MerchantMenuSavePayload;

const typeLabels: Record<MenuType, string> = { DIRECTORY: '目录', MENU: '菜单', BUTTON: '按钮' };
const { can } = usePermission();
const router = useRouter();
const componentOptions = ['dashboard', 'content', 'users', 'roles', 'integration-sync', 'profile'];
const icons = ref<IconItem[]>([]);
const enabledIcons = computed(() => icons.value.filter(item => item.status === 1 || item.id === form.iconId));
const columns: TableColumnData[] = [
  { title: '菜单名称', dataIndex: 'name', slotName: 'name', width: 200 },
  { title: '类型', dataIndex: 'type', slotName: 'type', width: 72 },
  { title: '路由地址', slotName: 'route', width: 140 },
  { title: '页面组件', slotName: 'component', width: 120 },
  { title: '权限编码', slotName: 'permission', ellipsis: true, tooltip: true, width: 210 },
  { title: '排序', dataIndex: 'order', width: 64 },
  { title: '状态', slotName: 'status', width: 82 },
  { title: '显示', slotName: 'visible', width: 82 },
  { title: '操作', slotName: 'actions', fixed: 'right', width: 110 }
];

const merchants = ref<MerchantOption[]>([]);
const menus = ref<MerchantMenuItem[]>([]);
const selectedTenantId = ref<number>();
const loading = ref(false);
const saving = ref(false);
const drawerVisible = ref(false);
const editingId = ref<number>();
const formRef = ref<FormInstance>();
const expandAll = ref(true);
const refreshKey = ref(0);
const dirty = ref(false);
const filters = reactive<{ keyword: string; status?: number; visible?: number }>({ keyword: '' });

const emptyForm = (): MenuForm => ({
  tenantId: selectedTenantId.value || 0,
  parentId: 0,
  name: '',
  type: 'DIRECTORY',
  routeName: '',
  path: '',
  componentKey: '',
  icon: '',
  iconId: undefined,
  permission: '',
  order: 0,
  visible: 1,
  status: 1
});
const form = reactive<MenuForm>(emptyForm());
watch(form, () => { if (drawerVisible.value) dirty.value = true; }, { deep: true });

const rules = computed(() => ({
  name: [{ required: true, message: '请输入名称' }],
  type: [{ required: true, message: '请选择节点类型' }],
  path: form.type === 'MENU'
    ? [{ required: true, message: '请输入路由地址' }, { match: /^\/.+/, message: '路由地址必须以 / 开头' }]
    : [{ match: /^(\/.*)?$/, message: '路由地址必须以 / 开头' }],
  routeName: form.type === 'MENU' ? [{ required: true, message: '请输入路由名称' }] : [],
  componentKey: form.type === 'MENU' ? [{ required: true, message: '请选择页面组件' }] : [],
  permission: form.type !== 'DIRECTORY'
    ? [{ required: true, message: '请输入权限编码' }, { match: /^[a-z][a-z0-9:-]+$/, message: '权限编码格式不正确' }]
    : []
}));

function makeTree(records: MerchantMenuItem[]): MerchantMenuItem[] {
  const map = new Map(records.map(item => [item.id, { ...item, children: [] as MerchantMenuItem[] }]));
  const roots: MerchantMenuItem[] = [];
  map.forEach(item => {
    const parent = map.get(item.parentId);
    if (parent) parent.children!.push(item);
    else roots.push(item);
  });
  const sort = (nodes: MerchantMenuItem[]) => {
    nodes.sort((a, b) => a.order - b.order || a.id - b.id);
    nodes.forEach(node => {
      sort(node.children || []);
      if (!node.children?.length) delete node.children;
    });
  };
  sort(roots);
  return roots;
}
function syncIconCode(value: unknown) {
  const iconId = typeof value === 'number' ? value : undefined;
  form.icon = icons.value.find(item => item.id === iconId)?.code || '';
}

const menuTree = computed(() => makeTree(menus.value));
const filteredTree = computed(() => {
  const keyword = filters.keyword.trim().toLowerCase();
  const filterNodes = (nodes: MerchantMenuItem[]): MerchantMenuItem[] => nodes.flatMap(node => {
    const children = filterNodes(node.children || []);
    const matches = (!keyword || node.name.toLowerCase().includes(keyword))
      && (filters.status === undefined || node.status === filters.status)
      && (filters.visible === undefined || node.visible === filters.visible || node.type === 'BUTTON');
    if (!matches && !children.length) return [];
    const result = { ...node };
    if (children.length) result.children = children;
    else delete result.children;
    return [result];
  });
  return filterNodes(menuTree.value);
});
const filteredFlatCount = computed(() => {
  const count = (nodes: MerchantMenuItem[]): number => nodes.reduce((sum, node) => sum + 1 + count(node.children || []), 0);
  return count(filteredTree.value);
});
type ParentTreeNode = { key: number; title: string; children: ParentTreeNode[] };
const parentOptions = computed(() => {
  const allowed = form.type === 'BUTTON' ? ['MENU'] : ['DIRECTORY'];
  const prune = (nodes: MerchantMenuItem[]): ParentTreeNode[] => nodes
    .filter(node => allowed.includes(node.type) && node.id !== editingId.value)
    .map(node => ({ key: node.id, title: node.name, children: prune(node.children || []) }));
  return [{ key: 0, title: '顶级节点', children: prune(menuTree.value) }];
});

onMounted(async () => {
  try {
    [merchants.value, icons.value] = await Promise.all([fetchMerchants(), fetchIcons()]);
    if (merchants.value.length) {
      selectedTenantId.value = merchants.value[0].id;
      await loadMenus();
    }
  } catch (error) {
    Message.error(error instanceof Error ? error.message : '商家加载失败');
  }
});

async function loadMenus() {
  if (!selectedTenantId.value) return;
  loading.value = true;
  try {
    menus.value = await fetchMerchantMenus(selectedTenantId.value);
    refreshKey.value += 1;
  } catch (error) {
    Message.error(error instanceof Error ? error.message : '菜单加载失败');
  } finally {
    loading.value = false;
  }
}
function applyFilters() { refreshKey.value += 1; }
function resetFilters() {
  filters.keyword = '';
  filters.status = undefined;
  filters.visible = undefined;
  refreshKey.value += 1;
}
function openCreate(type: MenuType, parentId: number) {
  editingId.value = undefined;
  Object.assign(form, emptyForm(), { type, parentId });
  dirty.value = false;
  drawerVisible.value = true;
}
async function openEdit(record: MerchantMenuItem) {
  if (record.systemBuiltin || !selectedTenantId.value) return;
  try {
    const latest = await fetchMerchantMenu(record.id, selectedTenantId.value);
    editingId.value = record.id;
    Object.assign(form, {
      tenantId: latest.tenantId,
      parentId: latest.parentId,
      name: latest.name,
      type: latest.type,
      routeName: latest.routeName || '',
      path: latest.path || '',
      componentKey: latest.componentKey || '',
      icon: latest.icon || '',
      iconId: latest.iconId,
      permission: latest.permission || '',
      order: latest.order,
      visible: latest.visible,
      status: latest.status
    });
    dirty.value = false;
    drawerVisible.value = true;
  } catch (error) {
    Message.error(error instanceof Error ? error.message : '菜单详情加载失败');
  }
}
function normalizeFormByType() {
  form.parentId = 0;
  if (form.type === 'DIRECTORY') {
    form.routeName = '';
    form.componentKey = '';
    form.permission = '';
  } else if (form.type === 'BUTTON') {
    form.routeName = '';
    form.path = '';
    form.componentKey = '';
    form.icon = '';
    form.iconId = undefined;
    form.visible = 0;
  }
  dirty.value = true;
}
function beforeClose() {
  return !dirty.value || window.confirm('存在未保存的修改，确认关闭吗？');
}
async function save(done: (closed: boolean) => void) {
  const validation = await formRef.value?.validate();
  if (validation) {
    done(false);
    return;
  }
  saving.value = true;
  try {
    const payload = {
      ...form,
      tenantId: selectedTenantId.value!,
      name: form.name.trim(),
      routeName: form.routeName?.trim() || undefined,
      path: form.path?.trim() || undefined,
      componentKey: form.componentKey || undefined,
      icon: form.icon || undefined,
      iconId: form.iconId,
      permission: form.permission?.trim() || undefined
    };
    if (editingId.value) await updateMerchantMenu(editingId.value, payload);
    else await createMerchantMenu(payload);
    Message.success(editingId.value ? '菜单修改成功' : '菜单新增成功');
    dirty.value = false;
    await loadMenus();
    done(true);
  } catch (error) {
    Message.error(error instanceof Error ? error.message : '菜单保存失败');
    done(false);
  } finally {
    saving.value = false;
  }
}
function submitDrawer() {
  void save((closed) => {
    if (closed) drawerVisible.value = false;
  });
}
async function handleRowAction(record: MerchantMenuItem, action: string) {
  if (action === 'create-directory') return openCreate('DIRECTORY', record.id);
  if (action === 'create-menu') return openCreate('MENU', record.id);
  if (action === 'create-button') return openCreate('BUTTON', record.id);
  if (action === 'toggle-status') return toggleStatus(record, record.status !== 1);
  if (action === 'toggle-visible') return toggleVisibility(record, record.visible !== 1);
  if (action === 'delete' && window.confirm(`确认删除“${record.name}”吗？存在子级时将无法删除。`)) {
    await remove(record);
  }
}
async function toggleStatus(record: MerchantMenuItem, checked: string | number | boolean) {
  if (!selectedTenantId.value) return;
  if (!checked && !window.confirm(`确认停用“${record.name}”吗？停用后该节点及其下级功能将不再生效。`)) return;
  try {
    await updateMerchantMenuStatus(record.id, selectedTenantId.value, checked ? 1 : 0);
    record.status = checked ? 1 : 0;
    Message.success(checked ? '菜单已启用' : '菜单已停用');
  } catch (error) {
    Message.error(error instanceof Error ? error.message : '状态修改失败');
  }
}
async function toggleVisibility(record: MerchantMenuItem, checked: string | number | boolean) {
  if (!selectedTenantId.value) return;
  try {
    await updateMerchantMenuVisibility(record.id, selectedTenantId.value, checked ? 1 : 0);
    record.visible = checked ? 1 : 0;
    Message.success(checked ? '菜单已设为正常显示' : '菜单已隐藏');
  } catch (error) {
    Message.error(error instanceof Error ? error.message : '显示状态修改失败');
  }
}
async function remove(record: MerchantMenuItem) {
  if (!selectedTenantId.value || record.systemBuiltin) return;
  try {
    await deleteMerchantMenu(record.id, selectedTenantId.value);
    Message.success(`“${record.name}”已删除`);
    await loadMenus();
  } catch (error) {
    Message.error(error instanceof Error ? error.message : '删除失败');
  }
}
</script>

<style scoped>
.container { width: 100%; padding: var(--tql-page-padding); }
.search-card,
.general-card {
  border-color: var(--tql-border);
  border-radius: var(--tql-radius-card);
  box-shadow: none;
}
.general-card :deep(.arco-card-body) { padding: var(--tql-card-padding); }
.general-card { min-height: 0; }
.table-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: var(--tql-space-4);
}
.record-total { color: var(--tql-text-tertiary); font-size: 12px; }
.menu-table :deep(.arco-table-th) {
  height: 44px;
  color: var(--tql-text-secondary);
  background: var(--tql-bg-subtle);
  font-weight: 500;
}
.menu-table :deep(.arco-table-td) { height: 44px; border-bottom-color: var(--tql-border-light); }
.menu-name { display: flex; align-items: center; gap: var(--tql-space-2); }
.type-icon { display: inline-flex; color: var(--tql-text-tertiary); font-size: 16px; }
.code-text {
  color: var(--tql-text-secondary);
  font-family: Consolas, monospace;
  font-size: 12px;
}
.empty-text { color: var(--tql-text-tertiary); }
.full-width { width: 100%; }
.drawer-footer { display: flex; justify-content: flex-end; }
.icon-maintain { margin-top: var(--tql-space-2); }

</style>
