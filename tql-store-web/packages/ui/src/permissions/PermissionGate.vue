<template>
  <slot v-if="allowed" />
  <slot v-else name="fallback" />
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { usePermission, type PermissionMatchMode, type PermissionRequirement } from '@tql-store/auth';

const props = withDefaults(defineProps<{
  permission?: PermissionRequirement;
  mode?: PermissionMatchMode;
}>(), { mode: 'any' });
const { can } = usePermission();
const allowed = computed(() => can(props.permission, props.mode));
</script>
