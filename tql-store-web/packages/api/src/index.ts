import axios, { AxiosError, type AxiosRequestConfig } from 'axios';
import type {
  ApiResponse,
  ChangePasswordPayload,
  ClientType,
  ContentItem,
  ContentQuery,
  LoginResponse,
  IconItem,
  MenuItem,
  MerchantMenuItem,
  MerchantMenuSavePayload,
  MerchantOption,
  OrganizationOption,
  PageResult,
  RoleItem,
  RoleSavePayload,
  StoreOption,
  SyncLogItem,
  SyncTaskCreatePayload,
  SyncTaskItem,
  SyncTaskQuery,
  UserDetail,
  UserItem,
  UserProfile,
  UserQuery,
  UserSavePayload
} from '@tql-store/shared';

interface ApiConfig {
  baseURL: string;
  clientType: ClientType;
  getToken: () => string | null;
  onUnauthorized: () => void;
}

let apiConfig: ApiConfig;

const http = axios.create({ timeout: 12000 });

export function configureApi(config: ApiConfig) {
  apiConfig = config;
  http.defaults.baseURL = config.baseURL;
}

http.interceptors.request.use((config) => {
  if (!apiConfig) throw new Error('API 尚未初始化');
  config.headers.set('X-Client-Type', apiConfig.clientType);
  const token = apiConfig.getToken();
  if (token) config.headers.set('Authorization', `Bearer ${token}`);
  if (config.method?.toLowerCase() === 'get') {
    config.headers.set('Cache-Control', 'no-cache');
    config.headers.set('Pragma', 'no-cache');
  }
  return config;
});

http.interceptors.response.use(
  (response) => response,
  (error: AxiosError<ApiResponse<unknown>>) => {
    if (error.response?.status === 401 && apiConfig?.getToken()) {
      apiConfig.onUnauthorized();
    }
    const message = error.response?.data?.message || error.message || '网络请求失败';
    return Promise.reject(new Error(message));
  }
);

async function request<T>(config: AxiosRequestConfig): Promise<T> {
  const response = await http.request<ApiResponse<T>>(config);
  if (response.data.code !== 200) throw new Error(response.data.message || '请求失败');
  return response.data.data;
}

export const login = (username: string, password: string, clientType: ClientType, merchantNo?: string) =>
  request<LoginResponse>({
    method: 'POST',
    url: '/api/auth/login',
    data: { username, password, clientType, merchantNo }
  });

export const logout = () => request<void>({ method: 'POST', url: '/api/auth/logout' });
export const changePassword = (data: ChangePasswordPayload) =>
  request<void>({ method: 'POST', url: '/api/auth/change-password', data });
export const fetchProfile = () => request<UserProfile>({ method: 'GET', url: '/api/system/profile' });
export const fetchMenus = () => request<MenuItem[]>({ method: 'GET', url: '/api/system/menus' });
export const fetchContents = (params: ContentQuery) =>
  request<PageResult<ContentItem>>({ method: 'GET', url: '/api/operation/contents', params });

export const fetchUsers = (params: UserQuery) =>
  request<PageResult<UserItem>>({ method: 'GET', url: '/api/system/users', params: { ...params, _t: Date.now() } });
export const fetchUser = (id: number) =>
  request<UserDetail>({ method: 'GET', url: `/api/system/users/${id}` });
export const createUser = (data: UserSavePayload) =>
  request<number>({ method: 'POST', url: '/api/system/users', data });
export const updateUser = (id: number, data: UserSavePayload) =>
  request<void>({ method: 'PUT', url: `/api/system/users/${id}`, data });
export const deleteUser = (id: number) =>
  request<void>({ method: 'DELETE', url: `/api/system/users/${id}` });

export const fetchRoles = (params?: { keyword?: string; status?: number }) =>
  request<RoleItem[]>({ method: 'GET', url: '/api/system/roles', params });
export const createRole = (data: RoleSavePayload) =>
  request<number>({ method: 'POST', url: '/api/system/roles', data });
export const updateRole = (id: number, data: RoleSavePayload) =>
  request<void>({ method: 'PUT', url: `/api/system/roles/${id}`, data });
export const deleteRole = (id: number) =>
  request<void>({ method: 'DELETE', url: `/api/system/roles/${id}` });
export const fetchAssignableMenus = () =>
  request<MenuItem[]>({ method: 'GET', url: '/api/system/roles/menus' });
export const fetchStores = () =>
  request<StoreOption[]>({ method: 'GET', url: '/api/system/stores', params: { _t: Date.now() } });
export const fetchOrganizations = () =>
  request<OrganizationOption[]>({ method: 'GET', url: '/api/system/organizations', params: { _t: Date.now() } });

export const fetchMerchants = () =>
  request<MerchantOption[]>({ method: 'GET', url: '/api/system/merchant-menus/merchants' });
export const fetchMerchantMenus = (tenantId: number) =>
  request<MerchantMenuItem[]>({ method: 'GET', url: '/api/system/merchant-menus', params: { tenantId, _t: Date.now() } });
export const fetchMerchantMenu = (id: number, tenantId: number) =>
  request<MerchantMenuItem>({ method: 'GET', url: `/api/system/merchant-menus/${id}`, params: { tenantId } });
export const createMerchantMenu = (data: MerchantMenuSavePayload) =>
  request<number>({ method: 'POST', url: '/api/system/merchant-menus', data });
export const updateMerchantMenu = (id: number, data: MerchantMenuSavePayload) =>
  request<void>({ method: 'PUT', url: `/api/system/merchant-menus/${id}`, data });
export const updateMerchantMenuStatus = (id: number, tenantId: number, status: number) =>
  request<void>({ method: 'PUT', url: `/api/system/merchant-menus/${id}/status`, params: { tenantId }, data: { status } });
export const updateMerchantMenuVisibility = (id: number, tenantId: number, visible: number) =>
  request<void>({ method: 'PUT', url: `/api/system/merchant-menus/${id}/visibility`, params: { tenantId }, data: { visible } });
export const deleteMerchantMenu = (id: number, tenantId: number) =>
  request<void>({ method: 'DELETE', url: `/api/system/merchant-menus/${id}`, params: { tenantId } });

export const fetchIcons = (params?: { keyword?: string; category?: string; status?: number }) =>
  request<IconItem[]>({ method: 'GET', url: '/api/system/icons', params });
export const uploadIcon = (data: FormData) =>
  request<number>({ method: 'POST', url: '/api/system/icons/upload', data });
export const updateIcon = (id: number, data: { name: string; category: string; order: number }) =>
  request<void>({ method: 'PUT', url: `/api/system/icons/${id}`, data });
export const updateIconStatus = (id: number, status: number) =>
  request<void>({ method: 'PUT', url: `/api/system/icons/${id}/status`, data: { status } });
export const deleteIcon = (id: number) =>
  request<void>({ method: 'DELETE', url: `/api/system/icons/${id}` });

export const fetchSyncTasks = (params: SyncTaskQuery) =>
  request<PageResult<SyncTaskItem>>({ method: 'GET', url: '/api/integration/sync-tasks', params });
export const createSyncTask = (data: SyncTaskCreatePayload) =>
  request<number>({ method: 'POST', url: '/api/integration/sync-tasks', data });
export const retrySyncTask = (id: number) =>
  request<number>({ method: 'POST', url: `/api/integration/sync-tasks/${id}/retry` });
export const fetchSyncLogs = (id: number) =>
  request<SyncLogItem[]>({ method: 'GET', url: `/api/integration/sync-tasks/${id}/logs` });
