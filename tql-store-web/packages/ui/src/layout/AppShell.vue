<template>
  <a-layout class="app-layout" :class="{ 'is-collapsed': store.collapsed }">
    <a-layout-header class="layout-navbar">
      <div class="navbar">
        <div class="navbar-brand">
          <BrandMark :title="config.shortTitle" tone="light" />
        </div>

        <div class="navbar-actions" aria-label="顶部工具栏">
          <a-tooltip v-for="tool in utilityTools" :key="tool.name" :content="tool.name">
            <a-button
              class="nav-action"
              type="outline"
              shape="circle"
              :aria-label="tool.name"
              @click="handleUtility(tool.name)"
            >
              <template #icon><component :is="tool.icon" /></template>
            </a-button>
          </a-tooltip>

          <a-tooltip content="消息通知">
            <a-badge dot :count="1">
              <a-button
                class="nav-action"
                type="outline"
                shape="circle"
                aria-label="消息通知"
                @click="handleUtility('消息通知')"
              >
                <template #icon><IconNotification /></template>
              </a-button>
            </a-badge>
          </a-tooltip>

          <a-tooltip content="全屏">
            <a-button class="nav-action" type="outline" shape="circle" aria-label="全屏" @click="toggleFullscreen">
              <template #icon><IconFullscreen /></template>
            </a-button>
          </a-tooltip>

          <a-tooltip content="页面设置">
            <a-button class="nav-action" type="outline" shape="circle" aria-label="页面设置" @click="handleUtility('页面设置')">
              <template #icon><IconSettings /></template>
            </a-button>
          </a-tooltip>

          <a-tooltip content="主题配置">
            <a-button class="nav-action" type="outline" shape="circle" aria-label="主题配置" @click="handleUtility('主题配置')">
              <template #icon><IconPalette /></template>
            </a-button>
          </a-tooltip>

          <a-dropdown trigger="click" @select="handleUserAction">
            <button class="avatar-trigger" type="button" aria-label="用户菜单">
              <a-avatar :size="32" class="user-avatar">{{ avatarText }}</a-avatar>
            </button>
            <template #content>
              <a-doption value="profile"><template #icon><IconUser /></template>个人中心</a-doption>
              <a-doption value="logout"><template #icon><IconExport /></template>退出登录</a-doption>
            </template>
          </a-dropdown>
        </div>
      </div>
    </a-layout-header>

    <a-layout-sider
      class="layout-sider"
      :class="{ collapsed: store.collapsed }"
      :width="220"
      :collapsed-width="48"
      :collapsed="store.collapsed"
      :hide-trigger="true"
    >
      <div class="menu-wrapper">
        <a-menu
          class="app-menu"
          :collapsed="store.collapsed"
          :selected-keys="selectedKeys"
          v-model:open-keys="openKeys"
          @menu-item-click="handleMenuClick"
        >
          <SidebarMenuNode v-for="item in menuTree" :key="item.id" :item="item" />
        </a-menu>
      </div>

      <a-tooltip :content="store.collapsed ? '展开菜单' : '收起菜单'" position="right">
        <button class="menu-collapse-button" type="button" @click="store.toggleCollapsed">
          <IconMenuFold v-if="!store.collapsed" />
          <IconMenuUnfold v-else />
        </button>
      </a-tooltip>
    </a-layout-sider>

    <a-layout class="layout-main">
      <a-layout-content class="app-content">
        <a-spin :loading="store.loading" tip="正在加载工作空间" class="content-spin">
          <router-view />
        </a-spin>
      </a-layout-content>
    </a-layout>
  </a-layout>
</template>

<script setup lang="ts">
import { computed, inject, onMounted, ref, watch } from 'vue';
import { Message } from '@arco-design/web-vue';
import {
  IconExport,
  IconFullscreen,
  IconLanguage,
  IconMenuFold,
  IconMenuUnfold,
  IconMoon,
  IconNotification,
  IconPalette,
  IconSearch,
  IconSettings,
  IconUser
} from '@arco-design/web-vue/es/icon';
import { useRoute, useRouter } from 'vue-router';
import { logout } from '@tql-store/api';
import { clearToken, useAppStore } from '@tql-store/auth';
import { APP_CONFIG_KEY } from '../context';
import BrandMark from '../components/BrandMark.vue';
import SidebarMenuNode from './SidebarMenuNode.vue';
import type { MenuItem } from '@tql-store/shared';

const config = inject(APP_CONFIG_KEY)!;
const store = useAppStore();
const route = useRoute();
const router = useRouter();

const normalizePath = (path?: string) => {
  if (!path) return '/';
  const normalized = path.replace(/\/+$/, '');
  return normalized || '/';
};
const selectedKeys = computed(() => [normalizePath(route.path)]);
const openKeys = ref<string[]>([]);
const avatarText = computed(() => store.profile?.displayName?.slice(0, 1) || '同');
const menuTree = computed(() => {
  const records = store.menus;
  const map = new Map(records.map(item => [item.id, { ...item, children: [] as MenuItem[] }]));
  const roots: MenuItem[] = [];
  map.forEach(item => {
    const parent = map.get(item.parentId);
    if (parent) parent.children!.push(item);
    else roots.push(item);
  });
  const sort = (nodes: MenuItem[]) => {
    nodes.sort((a, b) => a.order - b.order || a.id - b.id);
    nodes.forEach(node => sort(node.children || []));
  };
  sort(roots);
  const filterEnabled = (nodes: MenuItem[]): MenuItem[] => nodes
    .filter(item => item.status === 1 && item.visible === 1 && item.type !== 'BUTTON')
    .map(item => ({ ...item, children: filterEnabled(item.children || []) }));
  return filterEnabled(roots);
});
const utilityTools = [
  { name: '搜索', icon: IconSearch },
  { name: '多语言', icon: IconLanguage },
  { name: '主题模式', icon: IconMoon }
];

watch(
  [() => route.path, () => store.menus],
  () => {
    const currentPath = normalizePath(route.path);
    const records = store.menus;
    const currentMenu = records.find(item =>
      item.type === 'MENU' && normalizePath(item.path) === currentPath
    );
    if (!currentMenu) return;

    const byId = new Map(records.map(item => [item.id, item]));
    const ancestorKeys: string[] = [];
    const visited = new Set<number>();
    let parentId = currentMenu.parentId;
    while (parentId && !visited.has(parentId)) {
      visited.add(parentId);
      const parent = byId.get(parentId);
      if (!parent) break;
      if (parent.type === 'DIRECTORY') ancestorKeys.unshift(`directory-${parent.id}`);
      parentId = parent.parentId;
    }
    openKeys.value = ancestorKeys;
  },
  { immediate: true }
);

onMounted(async () => {
  if (import.meta.env.DEV && route.query.preview === '1') return;
  try {
    await store.loadContext();
  } catch (error) {
    Message.error(error instanceof Error ? error.message : '工作空间加载失败');
  }
});

function handleMenuClick(key: string) {
  if (key !== route.path) router.push(key);
}

function handleUtility(name: string) {
  Message.info(`${name}功能将在后续业务阶段接入`);
}

async function toggleFullscreen() {
  try {
    if (document.fullscreenElement) {
      await document.exitFullscreen();
    } else {
      await document.documentElement.requestFullscreen();
    }
  } catch {
    Message.warning('当前浏览器未允许全屏操作');
  }
}

async function handleUserAction(value: string | number | Record<string, unknown> | undefined) {
  if (value === 'profile') {
    await router.push('/profile');
    return;
  }
  if (value !== 'logout') return;
  try {
    await logout();
  } catch {
    // 即使远端会话已失效，也需要清理本地状态。
  } finally {
    clearToken(config.clientType);
    store.reset();
    await router.replace('/login');
    Message.success('已安全退出');
  }
}
</script>

<style scoped>
.app-layout {
  min-height: 100vh;
  background: var(--tql-page);
}

.layout-navbar {
  position: fixed;
  z-index: 100;
  top: 0;
  right: 0;
  left: 0;
  height: 60px;
  padding: 0;
  background: var(--tql-bg-card);
  border-bottom: 1px solid var(--tql-border);
  line-height: normal;
}

.navbar {
  display: flex;
  height: 100%;
  align-items: center;
  justify-content: space-between;
}

.navbar-brand {
  display: flex;
  min-width: 220px;
  height: 60px;
  align-items: center;
  padding: 0 20px;
}

.navbar-actions {
  display: flex;
  height: 60px;
  align-items: center;
  gap: 20px;
  padding-right: 20px;
}

.nav-action {
  width: 32px;
  height: 32px;
  padding: 0;
  color: var(--tql-text-secondary);
  background: var(--tql-bg-card);
  border-color: var(--tql-border-light);
}

.nav-action:hover {
  color: var(--tql-primary);
  background: var(--tql-bg-subtle);
  border-color: var(--tql-border);
}

.avatar-trigger {
  display: flex;
  width: 32px;
  height: 32px;
  align-items: center;
  justify-content: center;
  padding: 0;
  background: transparent;
  border: 0;
  border-radius: 50%;
  cursor: pointer;
}

.avatar-trigger:hover { box-shadow: 0 0 0 4px var(--tql-border-light); }
.user-avatar { color: var(--tql-primary); background: var(--tql-primary-soft); font-weight: 600; }

.layout-sider {
  position: fixed;
  z-index: 99;
  top: 0;
  bottom: 0;
  left: 0;
  height: 100vh;
  overflow: hidden;
  padding-top: 60px;
  background: var(--tql-bg-card);
  border-right: 1px solid var(--tql-border);
  box-shadow: none;
}

.menu-wrapper {
  height: 100%;
  overflow: hidden auto;
  padding: 4px 0 48px;
  scrollbar-width: none;
}

.menu-wrapper::-webkit-scrollbar { display: none; }
.app-menu { width: 100%; background: var(--tql-bg-card); }
.app-menu :deep(.arco-menu-inner) { overflow: visible; background: var(--tql-bg-card); }
.app-menu :deep(.arco-menu-item),
.app-menu :deep(.arco-menu-inline-header) {
  height: 40px;
  margin: 0 8px 4px;
  padding: 0 12px;
  color: var(--tql-text-secondary);
  background: transparent;
  border-radius: 2px;
  line-height: 40px;
}

.app-menu :deep(.arco-menu-inline-header) { padding-right: 28px; }
.app-menu :deep(.arco-menu-item:hover),
.app-menu :deep(.arco-menu-inline-header:hover) {
  color: var(--tql-text-primary);
  background: var(--tql-bg-subtle);
}
.app-menu :deep(.arco-menu-selected),
.app-menu :deep(.arco-menu-selected:hover) { color: var(--tql-primary); background: var(--tql-bg-hover); }
.app-menu :deep(.arco-menu-icon) { color: var(--tql-text-tertiary); font-size: 16px; }
.app-menu :deep(.arco-menu-selected .arco-menu-icon) { color: var(--tql-primary); }

.layout-sider.collapsed .app-menu :deep(.arco-menu-item) {
  display: flex;
  width: 40px;
  align-items: center;
  justify-content: center;
  margin-right: 0;
  margin-left: 0;
  padding-right: 0 !important;
  padding-left: 0 !important;
}

.layout-sider.collapsed .app-menu :deep(.arco-menu-inner) { padding: 4px !important; }
.layout-sider.collapsed .app-menu :deep(.arco-menu-title) { display: none; }
.layout-sider.collapsed .app-menu :deep(.arco-menu-icon),
.layout-sider.collapsed .app-menu :deep(.arco-icon) { margin-right: 0 !important; }

.layout-sider.collapsed .app-menu :deep(.arco-menu-selected),
.layout-sider.collapsed .app-menu :deep(.arco-menu-selected:hover) { background: var(--tql-bg-card); }

.menu-collapse-button {
  position: absolute;
  right: 12px;
  bottom: 12px;
  display: flex;
  width: 24px;
  height: 24px;
  align-items: center;
  justify-content: center;
  padding: 0;
  color: var(--tql-text-tertiary);
  background: var(--tql-bg-subtle);
  border: 0;
  border-radius: 2px;
  cursor: pointer;
}

.menu-collapse-button:hover { color: var(--tql-primary); background: var(--tql-bg-hover); }
.layout-sider.collapsed .menu-collapse-button { right: 12px; }

.layout-main {
  min-width: 0;
  min-height: 100vh;
  padding-top: 60px;
  padding-left: 220px;
  background: var(--tql-page);
  transition: padding-left .2s cubic-bezier(.34, .69, .1, 1);
}

.app-layout.is-collapsed .layout-main { padding-left: 48px; }
.app-content { min-width: 0; overflow: auto; padding: 0; }
.content-spin { display: block; min-height: calc(100vh - 60px); }

@media (max-width: 900px) {
  .navbar-actions { gap: 8px; padding-right: 12px; }
  .navbar-actions :deep(.arco-tooltip-popup-trigger):nth-child(-n+3) { display: none; }
}
</style>
