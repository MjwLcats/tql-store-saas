<template>
  <a-select :model-value="modelValue" :disabled="disabled" placeholder="请选择数据范围" @update:model-value="update">
    <a-option v-for="option in resolvedOptions" :key="option.value" :value="option.value">
      {{ option.label }}
    </a-option>
  </a-select>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import type { ClientType, DataScope } from '@tql-store/shared';

const props = withDefaults(defineProps<{
  modelValue: DataScope;
  clientType: ClientType;
  disabled?: boolean;
}>(), { disabled: false });
const emit = defineEmits<{ 'update:modelValue': [value: DataScope] }>();

const merchantOptions: Array<{ value: DataScope; label: string }> = [
  { value: 'ALL', label: '全部门店' },
  { value: 'STORE_AND_CHILD', label: '本门店及下级门店' },
  { value: 'STORE', label: '本门店' },
  { value: 'CUSTOM', label: '自定义门店' },
  { value: 'SELF', label: '仅本人数据' }
];
const platformOptions: Array<{ value: DataScope; label: string }> = [
  { value: 'ALL', label: '全部平台数据' },
  { value: 'SELF', label: '仅本人数据' }
];
const resolvedOptions = computed(() => props.clientType === 'MERCHANT' ? merchantOptions : platformOptions);
function update(value: unknown) {
  if (typeof value === 'string') emit('update:modelValue', value as DataScope);
}
</script>
