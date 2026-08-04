<template>
  <a-collapse
    class="organization-collapse"
    :bordered="false"
    :default-active-key="[]"
    :destroy-on-hide="true"
  >
    <a-collapse-item v-for="node in nodes" :key="String(node.key)">
      <template #header>
        <div class="organization-header">
          <a-checkbox
            :model-value="organizationState(node).checked"
            :indeterminate="organizationState(node).indeterminate"
            @click.stop
            @change="checked => toggleOrganization(node, checked === true)"
          />
          <span class="organization-name">{{ node.title }}</span>
          <a-tag size="small" color="arcoblue">{{ node.employeeCount || 0 }} 人</a-tag>
        </div>
      </template>

      <div class="organization-content">
        <div v-if="employeeChildren(node).length" class="organization-employees">
          <label v-for="employee in employeeChildren(node)" :key="employee.key" class="employee-option">
            <a-checkbox
              :model-value="selectedKeys.includes(Number(employee.key))"
              @change="checked => toggleEmployee(Number(employee.key), checked === true)"
            />
            <span>{{ employee.title }}</span>
            <small>{{ employee.employeeNumber || '未设置工号' }}</small>
          </label>
        </div>

        <OrganizationCollapseNode
          v-if="organizationChildren(node).length"
          :nodes="organizationChildren(node)"
          :selected-keys="selectedKeys"
          @update:selected-keys="emit('update:selectedKeys', $event)"
        />
      </div>
    </a-collapse-item>
  </a-collapse>
</template>

<script setup lang="ts">
defineOptions({ name: 'OrganizationCollapseNode' });

export interface OrganizationSelectorNode {
  key: string | number;
  title: string;
  employeeNumber?: string;
  employeeCount?: number;
  isEmployee?: boolean;
  children?: OrganizationSelectorNode[];
}

const props = defineProps<{
  nodes: OrganizationSelectorNode[];
  selectedKeys: number[];
}>();
const emit = defineEmits<{
  'update:selectedKeys': [keys: number[]];
}>();

const organizationChildren = (node: OrganizationSelectorNode) =>
  (node.children || []).filter(child => !child.isEmployee);
const employeeChildren = (node: OrganizationSelectorNode) =>
  (node.children || []).filter(child => child.isEmployee);

function descendantEmployeeIds(node: OrganizationSelectorNode): number[] {
  if (node.isEmployee) return [Number(node.key)];
  return (node.children || []).flatMap(descendantEmployeeIds);
}

function organizationState(node: OrganizationSelectorNode) {
  const ids = descendantEmployeeIds(node);
  const selectedCount = ids.filter(id => props.selectedKeys.includes(id)).length;
  return {
    checked: ids.length > 0 && selectedCount === ids.length,
    indeterminate: selectedCount > 0 && selectedCount < ids.length
  };
}

function toggleEmployee(id: number, checked: boolean) {
  const next = new Set(props.selectedKeys);
  if (checked) next.add(id);
  else next.delete(id);
  emit('update:selectedKeys', [...next]);
}

function toggleOrganization(node: OrganizationSelectorNode, checked: boolean) {
  const next = new Set(props.selectedKeys);
  descendantEmployeeIds(node).forEach(id => {
    if (checked) next.add(id);
    else next.delete(id);
  });
  emit('update:selectedKeys', [...next]);
}
</script>

<style scoped>
.organization-collapse { width:100%; background:transparent; }
.organization-collapse :deep(.arco-collapse-item) { overflow:hidden; margin-bottom:8px; border:1px solid var(--tql-border); border-radius:6px; background:var(--tql-color-white); }
.organization-collapse :deep(.arco-collapse-item-header) { min-height:42px; padding:0 12px; color:var(--tql-text-primary); background:var(--tql-color-white); }
.organization-collapse :deep(.arco-collapse-item-header:hover) { background:var(--tql-bg-subtle); }
.organization-collapse :deep(.arco-collapse-item-active > .arco-collapse-item-header) { background:var(--tql-primary-subtle); }
.organization-collapse :deep(.arco-collapse-item-icon-hover) { color:var(--tql-text-secondary); }
.organization-collapse :deep(.arco-collapse-item-content) { padding:8px 10px 10px; background:var(--tql-bg-panel); }
.organization-header { display:flex; min-width:0; width:100%; align-items:center; gap:10px; }
.organization-name { min-width:0; flex:1; overflow:hidden; font-weight:500; text-overflow:ellipsis; white-space:nowrap; }
.organization-content { display:grid; gap:8px; }
.organization-employees { display:grid; gap:4px; }
.employee-option { display:flex; min-height:36px; align-items:center; gap:10px; padding:0 10px; border-radius:4px; cursor:pointer; }
.employee-option:hover { background:var(--tql-primary-soft); }
.employee-option span { min-width:0; flex:1; overflow:hidden; color:var(--tql-text-primary); text-overflow:ellipsis; white-space:nowrap; }
.employee-option small { color:var(--tql-text-tertiary); font-size:12px; }
.organization-content > .organization-collapse { padding-left:14px; border-left:1px solid var(--tql-border); }
</style>
