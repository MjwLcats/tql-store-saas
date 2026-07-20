import { createApp } from 'vue';
import { createPinia } from 'pinia';
import ArcoVue from '@arco-design/web-vue';
import '@arco-design/web-vue/dist/arco.css';
import '@tql-store/config/theme.css';
import { configureApi } from '@tql-store/api';
import { clearToken, getToken } from '@tql-store/auth';
import { APP_CONFIG_KEY, permissionDirective } from '@tql-store/ui';
import App from './App.vue';
import { appConfig } from './config';
import { router } from './router';

configureApi({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://127.0.0.1:8080',
  clientType: appConfig.clientType,
  getToken: () => getToken(appConfig.clientType),
  onUnauthorized: () => { clearToken(appConfig.clientType); window.location.assign('/login'); }
});

const app = createApp(App);
app.provide(APP_CONFIG_KEY, appConfig);
app.use(createPinia());
app.directive('permission', permissionDirective);
app.use(router).use(ArcoVue).mount('#app');
