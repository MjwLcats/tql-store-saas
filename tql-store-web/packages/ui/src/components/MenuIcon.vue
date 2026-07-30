<template>
  <span class="menu-icon" aria-hidden="true">
    <span v-if="svg" class="custom-svg" v-html="svg" />
    <component :is="component" v-else />
  </span>
</template>
<script setup lang="ts">
import { computed, type Component } from 'vue';
import { IconApps, IconDashboard, IconFile, IconMenu, IconSettings, IconSync, IconUser } from '@arco-design/web-vue/es/icon';
const props = defineProps<{ code?: string; svg?: string }>();
const icons: Record<string, Component> = { IconApps, IconDashboard, IconFile, IconMenu, IconSettings, IconSync, IconUser };
const component = computed(() => icons[props.code || ''] || IconApps);
</script>
<style scoped>
.menu-icon,.custom-svg { display:inline-flex; width:16px; height:16px; align-items:center; justify-content:center; }
.custom-svg :deep(svg) { width:16px; height:16px; color:currentColor; fill:none; stroke:currentColor; }
</style>
