import { createRouter, createWebHistory } from 'vue-router';
import { getToken } from '@tql-store/auth';
import { fetchMenus } from '@tql-store/api';
import {
  AppShell,
  ContentPage,
  DashboardPage,
  IntegrationSyncPage,
  LoginPage,
  ProfilePage,
  RoleManagementPage,
  UserManagementPage
} from '@tql-store/ui';
import { appConfig } from './config';

export const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/login', name: 'login', component: LoginPage, meta: { public: true } },
    {
      path: '/', name: 'root', component: AppShell, children: []
    },
    { path: '/:pathMatch(.*)*', redirect: '/' }
  ]
});

const componentRegistry = {
  dashboard: DashboardPage,
  content: ContentPage,
  users: UserManagementPage,
  roles: RoleManagementPage,
  'integration-sync': IntegrationSyncPage,
  profile: ProfilePage
} as const;
let routesReady = false;

router.beforeEach(async (to) => {
  const token = getToken(appConfig.clientType);
  if (!to.meta.public && !token) return { path: '/login', query: { redirect: to.fullPath } };
  if (token && !routesReady) {
    const menus = await fetchMenus();
    menus.filter(menu => menu.type === 'MENU' && menu.path && menu.componentKey && menu.status === 1)
      .forEach(menu => {
        const component = componentRegistry[menu.componentKey as keyof typeof componentRegistry];
        if (!component || router.hasRoute(menu.routeName || `merchant-menu-${menu.id}`)) return;
        router.addRoute('root', {
          path: menu.path.replace(/^\//, ''),
          name: menu.routeName || `merchant-menu-${menu.id}`,
          component,
          meta: { permission: menu.permission }
        });
      });
    routesReady = true;
    return to.path === '/' || to.path === '/login' ? '/dashboard' : to.fullPath;
  }
  if (to.path === '/login' && token) return '/dashboard';
  if (to.path === '/') return token ? '/dashboard' : '/login';
  return true;
});
