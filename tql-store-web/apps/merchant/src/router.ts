import { createRouter, createWebHistory } from 'vue-router';
import { getToken } from '@tql-store/auth';
import { fetchMenus } from '@tql-store/api';
import {
  AppShell
} from '@tql-store/ui';
import { appConfig } from './config';

const LoginPage = () => import('@tql-store/ui/pages/LoginPage.vue');
const DashboardPage = () => import('@tql-store/ui/pages/DashboardPage.vue');
const AiContentWorkspacePage = () => import('@tql-store/ui/pages/AiContentWorkspacePage.vue');
const UserManagementPage = () => import('@tql-store/ui/pages/UserManagementPage.vue');
const RoleManagementPage = () => import('@tql-store/ui/pages/RoleManagementPage.vue');
const IntegrationSyncPage = () => import('@tql-store/ui/pages/IntegrationSyncPage.vue');
const ProfilePage = () => import('@tql-store/ui/pages/ProfilePage.vue');
const CostBomPage = () => import('@tql-store/ui/pages/CostBomPage.vue');
const InventoryTaskPage = () => import('@tql-store/ui/pages/InventoryTaskPage.vue');
const CostMasterDataPage = () => import('@tql-store/ui/pages/CostMasterDataPage.vue');
const KingdeeOutboundPage = () => import('@tql-store/ui/pages/KingdeeOutboundPage.vue');
const StoreMappingPage = () => import('@tql-store/ui/pages/StoreMappingPage.vue');
const SystemStorePage = () => import('@tql-store/ui/pages/SystemStorePage.vue');

export const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/login', name: 'login', component: LoginPage, meta: { public: true } },
    {
      path: '/', name: 'root', component: AppShell, children: [
        { path: 'cost/boms', name: 'MerchantCostBom', component: CostBomPage },
        { path: 'cost/inventory-tasks', name: 'MerchantInventoryTasks', component: InventoryTaskPage },
        { path: 'cost/master-data', name: 'MerchantCostMasterData', component: CostMasterDataPage }
        ,{ path: 'integration/kingdee-outbound', name: 'MerchantKingdeeOutbound', component: KingdeeOutboundPage }
        ,{ path: 'system/store-mapping', name: 'MerchantStoreMapping', component: StoreMappingPage }
        ,{ path: 'system/stores', name: 'MerchantSystemStores', component: SystemStorePage }
      ]
    }
  ]
});

const componentRegistry = {
  dashboard: DashboardPage,
  content: AiContentWorkspacePage,
  'ai-content': AiContentWorkspacePage,
  users: UserManagementPage,
  roles: RoleManagementPage,
  'integration-sync': IntegrationSyncPage,
  profile: ProfilePage,
  MerchantCostBom: CostBomPage,
  MerchantInventoryTasks: InventoryTaskPage,
  MerchantCostMasterData: CostMasterDataPage
  ,'kingdee-outbound': KingdeeOutboundPage
  ,'store-mapping': StoreMappingPage
  ,'system-stores': SystemStorePage
} as const;

const aiContentModuleByPath = {
  '/content/plans': 'plans',
  '/content/calendar': 'calendar',
  '/content/analytics': 'analytics',
  '/content/accounts': 'accounts',
  '/content/base-data': 'bgm'
} as const;
let routesReady = false;

router.beforeEach(async (to) => {
  if (import.meta.env.DEV && to.query.preview === '1') return true;
  const token = getToken(appConfig.clientType);
  if (!to.meta.public && !token) return { path: '/login', query: { redirect: to.fullPath } };
  if (token) {
    const menus = await fetchMenus();
    menus.filter(menu => menu.type === 'MENU' && menu.path && menu.componentKey && menu.status === 1)
      .forEach(menu => {
        const component = componentRegistry[menu.componentKey as keyof typeof componentRegistry];
        if (!component || router.hasRoute(menu.routeName || `merchant-menu-${menu.id}`)) return;
        router.addRoute('root', {
          path: menu.path.replace(/^\//, ''),
          name: menu.routeName || `merchant-menu-${menu.id}`,
          component,
          props: menu.componentKey === 'ai-content'
            ? { module: aiContentModuleByPath[menu.path as keyof typeof aiContentModuleByPath] || 'plans' }
            : undefined,
          meta: { permission: menu.permission }
        });
    });
    const firstLoad = !routesReady;
    routesReady = true;
    if (firstLoad) {
      return to.path === '/' || to.path === '/login'
        ? { path: '/dashboard', replace: true }
        : { path: to.fullPath, replace: true };
    }
  }
  if (to.path === '/login' && token) return '/dashboard';
  if (to.path === '/') return token ? '/dashboard' : '/login';
  if (routesReady && to.matched.length === 0) return { path: '/dashboard', replace: true };
  return true;
});
