import { createRouter, createWebHistory } from 'vue-router';
import { getToken } from '@tql-store/auth';
import { AppShell, ContentPage, DashboardPage, LoginPage, ProfilePage, RoleManagementPage, UserManagementPage } from '@tql-store/ui';
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
