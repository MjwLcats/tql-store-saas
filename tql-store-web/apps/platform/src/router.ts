import { createRouter, createWebHistory } from 'vue-router';
import { getToken } from '@tql-store/auth';
import { AppShell, ContentPage, DashboardPage, IconManagementPage, LoginPage, MenuManagementPage, ProfilePage, RoleManagementPage, UserManagementPage } from '@tql-store/ui';
import { appConfig } from './config';

export const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/login', name: 'login', component: LoginPage, meta: { public: true } },
    {
      path: '/', component: AppShell, redirect: '/dashboard', children: [
        { path: 'dashboard', name: 'dashboard', component: DashboardPage },
        { path: 'content', name: 'content', component: ContentPage },
        { path: 'users', name: 'users', component: UserManagementPage },
        { path: 'roles', name: 'roles', component: RoleManagementPage },
        { path: 'system/menus', name: 'menu-management', component: MenuManagementPage },
        { path: 'system/icons', name: 'icon-management', component: IconManagementPage },
        { path: 'profile', name: 'profile', component: ProfilePage }
      ]
    },
    { path: '/:pathMatch(.*)*', redirect: '/dashboard' }
  ]
});

router.beforeEach((to) => {
  const token = getToken(appConfig.clientType);
  if (!to.meta.public && !token) return { path: '/login', query: { redirect: to.fullPath } };
  if (to.path === '/login' && token) return '/dashboard';
  return true;
});
