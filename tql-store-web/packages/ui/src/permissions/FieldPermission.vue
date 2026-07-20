<template>
  <slot v-if="allowed" :readonly="false" />
  <slot v-else-if="behavior === 'readonly'" :readonly="true" />
  <span v-else-if="behavior === 'masked'" class="field-mask">{{ mask }}</span>
  <slot v-else-if="behavior === 'fallback'" name="fallback">{{ fallback }}</slot>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { usePermission, type PermissionRequirement } from '@tql-store/auth';

const props = withDefaults(defineProps<{
  permission?: PermissionRequirement;
  behavior?: 'hidden' | 'masked' | 'readonly' | 'fallback';
  mask?: string;
  fallback?: string;
}>(), { behavior: 'hidden', mask: '******', fallback: '—' });
const { can } = usePermission();
const allowed = computed(() => can(props.permission));
</script>

<style scoped>
.field-mask { color: var(--tql-text-secondary); letter-spacing: 1px; }
</style>
