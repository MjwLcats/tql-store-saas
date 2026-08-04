import { createRouter, createWebHistory } from 'vue-router';
import { getToken } from '@tql-store/auth';
import { AppShell } from '@tql-store/ui';
import { appConfig } from './config';

const LoginPage = () => import('@tql-store/ui/pages/LoginPage.vue');
const DashboardPage = () => import('@tql-store/ui/pages/DashboardPage.vue');
const AiContentWorkspacePage = () => import('@tql-store/ui/pages/AiContentWorkspacePage.vue');
const UserManagementPage = () => import('@tql-store/ui/pages/UserManagementPage.vue');
const RoleManagementPage = () => import('@tql-store/ui/pages/RoleManagementPage.vue');
const MenuManagementPage = () => import('@tql-store/ui/pages/MenuManagementPage.vue');
const IconManagementPage = () => import('@tql-store/ui/pages/IconManagementPage.vue');
const ProfilePage = () => import('@tql-store/ui/pages/ProfilePage.vue');

export const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/login', name: 'login', component: LoginPage, meta: { public: true } },
    {
      path: '/', component: AppShell, redirect: '/dashboard', children: [
        { path: 'dashboard', name: 'dashboard', component: DashboardPage },
        { path: 'content', redirect: '/content/plans' },
        { path: 'content/plans', name: 'content-plans', component: AiContentWorkspacePage, props: { module: 'plans' } },
        { path: 'content/calendar', name: 'content-calendar', component: AiContentWorkspacePage, props: { module: 'calendar' } },
        { path: 'content/analytics', name: 'content-analytics', component: AiContentWorkspacePage, props: { module: 'analytics' } },
        { path: 'content/accounts', name: 'content-accounts', component: AiContentWorkspacePage, props: { module: 'accounts' } },
        { path: 'ai-content', redirect: '/content/plans' },
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
