<template>
  <div class="organization-tree">
    <a-input-search v-model="keyword" placeholder="搜索组织" allow-clear />
    <a-spin :loading="loading" class="tree-loading">
      <a-tree
        v-if="filtered.length"
        :data="filtered"
        :selected-keys="selectedKeys"
        :expanded-keys="displayExpandedKeys"
        :field-names="{ key: 'id', title: 'name', children: 'children' }"
        block-node
        @update:expanded-keys="updateExpandedKeys"
        @select="selectNode"
      />
      <a-empty v-else description="未找到组织" />
    </a-spin>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue';
import type { OrganizationOption } from '@tql-store/shared';

const props = withDefaults(defineProps<{
  modelValue?: number;
  options: OrganizationOption[];
  loading?: boolean;
}>(), { loading: false });
const emit = defineEmits<{ 'update:modelValue': [value: number | undefined]; change: [value: number | undefined] }>();
const keyword = ref('');
const expandedKeys = ref<Array<string | number>>([]);
const selectedKeys = computed(() => props.modelValue === undefined ? [] : [props.modelValue]);

function filterNodes(nodes: OrganizationOption[], term: string): OrganizationOption[] {
  if (!term) return nodes;
  return nodes.flatMap((node) => {
    const children = filterNodes(node.children || [], term);
    return node.name.toLowerCase().includes(term) || children.length ? [{ ...node, children }] : [];
  });
}

const filtered = computed(() => filterNodes(props.options, keyword.value.trim().toLowerCase()));
function collectExpandableKeys(nodes: OrganizationOption[]): Array<string | number> {
  return nodes.flatMap(node => node.children?.length ? [node.id, ...collectExpandableKeys(node.children)] : []);
}
const displayExpandedKeys = computed(() => keyword.value.trim()
  ? collectExpandableKeys(filtered.value)
  : expandedKeys.value);

function updateExpandedKeys(keys: Array<string | number>) {
  if (!keyword.value.trim()) expandedKeys.value = keys;
}

function selectNode(keys: Array<string | number>) {
  const value = keys.length ? Number(keys[0]) : undefined;
  emit('update:modelValue', value);
  emit('change', value);
}
</script>

<style scoped>
.organization-tree { display: flex; width: 100%; min-height: 0; flex-direction: column; }
.tree-loading { display: block; min-height: 0; margin-top: var(--tql-space-3); overflow: auto; }
:deep(.arco-tree) { display: inline-block; width: max-content; min-width: 100%; }
:deep(.arco-tree-node) { width: max-content; min-width: 100%; }
.organization-tree :deep(.arco-tree-node-title),
.organization-tree :deep(.arco-tree-node-title *),
.organization-tree :deep(.arco-tree-node-title-text) { overflow: visible; white-space: nowrap; text-overflow: clip; }
:deep(.arco-tree-node-title-block) { border-radius: var(--tql-radius-control); }
:deep(.arco-tree-node-selected .arco-tree-node-title) { color: var(--tql-primary); background: var(--tql-primary-soft); }
</style>
