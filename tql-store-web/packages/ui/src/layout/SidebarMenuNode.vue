<template>
  <a-sub-menu v-if="item.type === 'DIRECTORY' && visibleChildren.length" :key="`directory-${item.id}`">
    <template #icon><MenuIcon :code="item.icon" :svg="item.iconSvg" /></template>
    <template #title>{{ item.name }}</template>
    <SidebarMenuNode v-for="child in visibleChildren" :key="child.id" :item="child" />
  </a-sub-menu>
  <a-menu-item v-else-if="item.type === 'MENU'" :key="item.path">
    <template #icon><MenuIcon :code="item.icon" :svg="item.iconSvg" /></template>
    {{ item.name }}
  </a-menu-item>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import type { MenuItem } from '@tql-store/shared';
import MenuIcon from '../components/MenuIcon.vue';

defineOptions({ name: 'SidebarMenuNode' });
const props = defineProps<{ item: MenuItem }>();
const visibleChildren = computed(() =>
  (props.item.children || []).filter(child => child.visible === 1 && child.type !== 'BUTTON'));
</script>
