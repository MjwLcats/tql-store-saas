<template>
  <a-select
    :model-value="modelValue"
    :multiple="multiple"
    :placeholder="placeholder"
    :disabled="disabled"
    :loading="loading"
    allow-clear
    allow-search
    @update:model-value="emitValue"
  >
    <a-option v-for="store in options" :key="store.id" :value="store.id" :disabled="store.disabled">
      {{ store.name }}<template v-if="store.code">（{{ store.code }}）</template>
    </a-option>
  </a-select>
</template>

<script setup lang="ts">
import type { StoreOption } from '@tql-store/shared';

withDefaults(defineProps<{
  modelValue?: number | number[];
  options: Array<StoreOption & { disabled?: boolean }>;
  multiple?: boolean;
  disabled?: boolean;
  loading?: boolean;
  placeholder?: string;
}>(), {
  multiple: false,
  disabled: false,
  loading: false,
  placeholder: '请选择门店'
});

const emit = defineEmits<{ 'update:modelValue': [value: number | number[] | undefined] }>();
function emitValue(value: unknown) {
  if (Array.isArray(value)) emit('update:modelValue', value.map(Number));
  else if (typeof value === 'string' || typeof value === 'number') emit('update:modelValue', Number(value));
  else emit('update:modelValue', undefined);
}
</script>
