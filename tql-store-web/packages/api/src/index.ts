import axios, { AxiosError, type AxiosRequestConfig } from 'axios';
import type {
  ApiResponse,
  ChangePasswordPayload,
  ClientType,
  ContentItem,
  ContentAccountItem,
  ContentAccountPayload,
  ContentVideoPerformanceItem,
  ContentDeliveryItem,
  ContentActivityItem,
  ContentPlanItem,
  ContentPrecheckResult,
  ContentPublishResult,
  CreateContentActivityPayload,
  UpdateContentPlanPayload,
  CreateContentPlanPayload,
  ContentQuery,
  LoginResponse,
  IconItem,
  MenuItem,
  MerchantMenuItem,
  MerchantMenuSavePayload,
  MerchantOption,
  OrganizationOption,
  PageResult,
  PersonnelImportResult,
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

async function requestBlob(config: AxiosRequestConfig): Promise<Blob> {
  const response = await http.request<Blob>({ ...config, responseType: 'blob' });
  return response.data;
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

export const fetchContentAccounts = () =>
  request<ContentAccountItem[]>({ method: 'GET', url: '/api/operation/content-accounts' });
export const createContentAccount = (data: ContentAccountPayload) =>
  request<number>({ method: 'POST', url: '/api/operation/content-accounts', data });
export const updateContentAccount = (id: number, data: ContentAccountPayload) =>
  request<void>({ method: 'PUT', url: `/api/operation/content-accounts/${id}`, data });
export const importContentAccounts = (records: ContentAccountPayload[]) =>
  request<number>({ method: 'POST', url: '/api/operation/content-accounts/import', data: { records } });
export const deleteContentAccounts = (ids: number[]) =>
  request<void>({ method: 'POST', url: '/api/operation/content-accounts/delete', data: { ids } });
export const fetchContentVideoPerformance = () =>
  request<ContentVideoPerformanceItem[]>({
    method: 'GET',
    url: '/api/operation/content-reports/video-performance'
  });

export const fetchContentActivities = (params?: { keyword?: string; status?: string }) =>
  request<ContentActivityItem[]>({ method: 'GET', url: '/api/operation/marketing-activities', params });
export const terminateContentActivity = (activityId: number) =>
  request<void>({ method: 'POST', url: `/api/operation/marketing-activities/${activityId}/terminate` });
export const deleteContentActivity = (activityId: number) =>
  request<void>({ method: 'DELETE', url: `/api/operation/marketing-activities/${activityId}` });
export const updateContentActivity = (activityId: number, data: UpdateContentPlanPayload) =>
  request<void>({ method: 'PUT', url: `/api/operation/marketing-activities/${activityId}`, data });
export const createContentActivity = (data: CreateContentActivityPayload) =>
  request<number>({ method: 'POST', url: '/api/operation/marketing-activities', data });
export const fetchActivityPlans = (activityId: number) =>
  request<ContentPlanItem[]>({
    method: 'GET',
    url: `/api/operation/marketing-activities/${activityId}/content-plans`
  });
export const fetchContentDeliveryTasks = (activityId: number) =>
  request<ContentDeliveryItem[]>({
    method: 'GET',
    url: `/api/operation/marketing-activities/${activityId}/employee-tasks`
  });
export const createContentPlan = (data: CreateContentPlanPayload) =>
  request<number>({ method: 'POST', url: '/api/operation/content-plans', data });
export const uploadContentSampleVideo = (file: File) => {
  const data = new FormData();
  data.append('file', file);
  return request<{ url: string; originalName: string; size: number }>({
    method: 'POST',
    url: '/api/operation/content-assets/sample-videos',
    data
  });
};
export const precheckContentPlan = (planId: number, employeeIds: number[]) =>
  request<ContentPrecheckResult>({
    method: 'POST',
    url: `/api/operation/content-plans/${planId}/precheck`,
    data: { employeeIds }
  });
export const publishContentPlan = (planId: number, employeeIds: number[], idempotencyKey: string) =>
  request<ContentPublishResult>({
    method: 'POST',
    url: `/api/operation/content-plans/${planId}/publish`,
    headers: { 'X-Idempotency-Key': idempotencyKey },
    data: { employeeIds }
  });

export const fetchUsers = (params: UserQuery) =>
  request<PageResult<UserItem>>({ method: 'GET', url: '/api/system/users', params: { ...params, _t: Date.now() } });
export const fetchContentTaskUsers = (page = 1, pageSize = 100) =>
  request<PageResult<UserItem>>({
    method: 'GET',
    url: '/api/system/users/content-task-options',
    params: { page, pageSize, _t: Date.now() }
  });
export const fetchContentAccountUsers = (page = 1, pageSize = 100) =>
  request<PageResult<UserItem>>({
    method: 'GET',
    url: '/api/system/users/content-account-options',
    params: { page, pageSize, _t: Date.now() }
  });
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
export const fetchContentTaskOrganizations = () =>
  request<OrganizationOption[]>({
    method: 'GET',
    url: '/api/system/organizations/content-task-options',
    params: { _t: Date.now() }
  });
export const downloadPersonnelImportTemplate = () =>
  requestBlob({ method: 'GET', url: '/api/system/content-personnel-import/template' });
export const validatePersonnelImport = (file: File) => {
  const data = new FormData();
  data.append('file', file);
  return request<PersonnelImportResult[]>({
    method: 'POST',
    url: '/api/system/content-personnel-import/validate',
    data
  });
};
export const fetchContentAccountOrganizations = () =>
  request<OrganizationOption[]>({
    method: 'GET',
    url: '/api/system/organizations/content-account-options',
    params: { _t: Date.now() }
  });

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

export interface KingdeeStoreOption {
  id: number; storeCode: string; storeName: string; orgNumber: string;
}
export interface KingdeeOutboundTask {
  id: number; rangeStart: string; rangeEnd: string; status: string;
  totalItems: number; successItems: number; failedItems: number; creatorName: string;
  startedAt?: string; finishedAt?: string; createTime: string;
}
export interface KingdeeOutboundItem {
  id: number; taskId: number; storeId: number; storeName: string; orgNumber: string;
  businessDate: string; status: string; currentPage: number; remoteTotal: number;
  receivedCount: number; savedCount: number; retryCount: number; errorMessage?: string; finishedAt?: string;
}
export interface KingdeeOutboundBill {
  id: number; storeId: number; storeName: string; billNo: string; billStatusName?: string;
  orgNumber: string; businessDate: string; auditTime?: string; departmentName?: string;
  requesterName?: string; entryCount: number; totalAmount: number; syncTime: string;
}
export const fetchKingdeeOutboundStores = () =>
  request<KingdeeStoreOption[]>({ method: 'GET', url: '/api/integration/kingdee-outbound/stores' });
export const syncKingdeeOrganizations = () =>
  request<number>({ method: 'POST', url: '/api/integration/kingdee-outbound/organizations/sync' });

export interface StoreMappingItem {
  systemStoreId: number; systemStoreCode?: string; systemStoreName: string;
  mappingId?: number; kingdeeOrgId?: number; kingdeeOrgNumber?: string; kingdeeOrgName?: string;
  hrOrgId?: number; hrOrgCode?: string; hrOrgName?: string;
  operatorName?: string; updateTime?: string;
}
export interface KingdeeMappingOption {
  id: number; orgNumber: string; orgName: string; bound: boolean;
}
export interface HrOrgMappingOption {
  id: number; orgCode?: string; orgName: string; bound: boolean;
}
export const fetchStoreMappings = (params: {keyword?: string; bound?: boolean; page: number; pageSize: number}) =>
  request<PageResult<StoreMappingItem>>({method:'GET',url:'/api/integration/store-mappings',params:{...params,_t:Date.now()}});
export const fetchKingdeeMappingOptions = (keyword = '') =>
  request<KingdeeMappingOption[]>({method:'GET',url:'/api/integration/store-mappings/kingdee-options',params:{keyword,_t:Date.now()}});
export const fetchHrOrgMappingOptions = (keyword = '') =>
  request<HrOrgMappingOption[]>({method:'GET',url:'/api/integration/store-mappings/hr-org-options',params:{keyword,_t:Date.now()}});
export const bindStoreMapping = (systemStoreId:number,payload:{kingdeeOrgId?:number;hrOrgId?:number}) =>
  request<void>({method:'PUT',url:`/api/integration/store-mappings/${systemStoreId}`,data:payload});
export const unbindStoreMapping = (systemStoreId:number) =>
  request<void>({method:'DELETE',url:`/api/integration/store-mappings/${systemStoreId}`});
export const unbindHrStoreMapping = (systemStoreId:number) =>
  request<void>({method:'DELETE',url:`/api/integration/store-mappings/${systemStoreId}/hr-org`});

export interface SystemStoreItem {
  id: number;
  parentId: number;
  storeCode: string;
  storeName: string;
  sourceType: 'MANUAL' | 'HUALALA';
  hllShopId?: number;
  hllShopName?: string;
  managerUserId?: number;
  managerName?: string;
  managerPhone?: string;
  cityName?: string;
  address?: string;
  contactPhone?: string;
  status: number;
  sortOrder: number;
  createTime: string;
  updateTime: string;
}
export interface SystemStoreSavePayload {
  parentId?: number;
  storeCode: string;
  storeName: string;
  sourceType: 'MANUAL' | 'HUALALA';
  hllShopId?: number;
  managerUserId?: number;
  cityName?: string;
  address?: string;
  contactPhone?: string;
  status: number;
  sortOrder?: number;
}
export interface HllShopOption {
  id: number;
  externalShopId: string;
  shopCode?: string;
  shopName: string;
  cityName?: string;
  address?: string;
  phone?: string;
  imported: boolean;
}
export interface StoreManagerOption {
  id: number;
  name: string;
  phone?: string;
  employeeNumber?: string;
}
export const fetchSystemStores = (params: {
  keyword?: string; status?: number; sourceType?: string; page: number; pageSize: number;
}) => request<PageResult<SystemStoreItem>>({
  method: 'GET', url: '/api/system/store-management', params: { ...params, _t: Date.now() }
});
export const fetchHllShopOptions = (keyword = '', currentStoreId?: number) =>
  request<HllShopOption[]>({
    method: 'GET', url: '/api/system/store-management/hll-options',
    params: { keyword, currentStoreId, _t: Date.now() }
  });
export const fetchStoreManagerOptions = (keyword = '') =>
  request<StoreManagerOption[]>({
    method: 'GET', url: '/api/system/store-management/manager-options',
    params: { keyword, _t: Date.now() }
  });
export const createSystemStore = (data: SystemStoreSavePayload) =>
  request<number>({ method: 'POST', url: '/api/system/store-management', data });
export const updateSystemStore = (id: number, data: SystemStoreSavePayload) =>
  request<void>({ method: 'PUT', url: `/api/system/store-management/${id}`, data });
export const deleteSystemStore = (id: number) =>
  request<void>({ method: 'DELETE', url: `/api/system/store-management/${id}` });

export const createKingdeeOutboundTask = (data: { storeIds: number[]; startDate: string; endDate: string }) =>
  request<number>({ method: 'POST', url: '/api/integration/kingdee-outbound/tasks', data });
export const fetchKingdeeOutboundTasks = (page = 1, pageSize = 10) =>
  request<PageResult<KingdeeOutboundTask>>({ method: 'GET', url: '/api/integration/kingdee-outbound/tasks', params: { page, pageSize, _t: Date.now() } });
export const fetchKingdeeOutboundTaskItems = (id: number) =>
  request<KingdeeOutboundItem[]>({ method: 'GET', url: `/api/integration/kingdee-outbound/tasks/${id}/items`, params: { _t: Date.now() } });
export const fetchKingdeeOutboundBills = (params: {
  storeId?: number; startDate?: string; endDate?: string; keyword?: string; page: number; pageSize: number;
}) => request<PageResult<KingdeeOutboundBill>>({ method: 'GET', url: '/api/integration/kingdee-outbound/bills', params: { ...params, _t: Date.now() } });

export interface CostUnit {
  id: number;
  unitCode: string;
  unitName: string;
  decimalScale: number;
  status: number;
}

export interface CostMaterial {
  id: number;
  materialCode: string;
  materialName: string;
  specification?: string;
  baseUnitId: number;
  externalMaterialCode?: string;
  sourceSystem: string;
  status: number;
}

export interface CostDish {
  id: number;
  dishCode: string;
  dishName: string;
  externalDishCode?: string;
  sourceSystem: string;
  status: number;
}

export interface SyncFoodPrice {
  foodPrice: string;
  unit: string;
}

export interface SyncFoodCandidate {
  foodID: number;
  shopID: number;
  foodCode: string;
  foodName: string;
  foodPrices?: SyncFoodPrice[];
  [key: string]: unknown;
}

export interface SyncFoodPage {
  rows: SyncFoodCandidate[];
  total: number;
}

export interface SyncFoodSourceShop {
  relateid: number;
  deptName: string;
}

export interface CostBom {
  id: number;
  storeId: number;
  dishId: number;
  status: 'DRAFT' | 'PENDING' | 'PUBLISHED' | 'REJECTED' | 'DISABLED';
  currentVersion: number;
  rowVersion: number;
  updatedTime: string;
}
export interface CostBomDetail {
  id: number; storeId: number; dishId: number; status: CostBom['status'];
  bomVersion: number; rowVersion: number; remark?: string; updatedTime: string;
  items: Array<{ id: number; materialId: number; unitId: number; quantity: number; sortOrder: number }>;
}

export interface InventoryTask {
  id: number;
  storeId: number;
  taskCode: string;
  taskName: string;
  status: string;
  plannedStartTime: string;
  plannedEndTime: string;
  version: number;
}
export interface InventoryCountItem {
  snapshotId: number; materialCode: string; materialName: string; specification?: string;
  locationName: string; unitName: string; bookQuantity: number; countedQuantity?: number;
}

export const fetchCostUnits = () =>
  request<CostUnit[]>({ method: 'GET', url: '/api/cost/master-data/units' });
export const createCostUnit = (data: { unitCode: string; unitName: string; decimalScale: number }) =>
  request<number>({ method: 'POST', url: '/api/cost/master-data/units', data });
export const fetchCostMaterials = () =>
  request<CostMaterial[]>({ method: 'GET', url: '/api/cost/master-data/materials' });
export const createCostMaterial = (data: {
  materialCode: string; materialName: string; specification?: string;
  baseUnitId: number; externalMaterialCode?: string; sourceSystem: string;
}) => request<number>({ method: 'POST', url: '/api/cost/master-data/materials', data });
export const fetchCostDishes = () =>
  request<CostDish[]>({ method: 'GET', url: '/api/cost/master-data/dishes' });
export const fetchSyncFoodCandidates = (params: {
  shopId: number; foodCode?: string; foodName?: string; pageNum: number; pageSize: number;
}) => request<SyncFoodPage>({ method: 'GET', url: '/api/cost/foods/sync-candidates', params });
export const fetchSyncFoodSourceShops = () =>
  request<SyncFoodSourceShop[]>({ method: 'GET', url: '/api/cost/foods/source-shops' });
export const saveSelectedSyncFoods = (rows: SyncFoodCandidate[]) =>
  request<number>({ method: 'POST', url: '/api/cost/foods/sync-selected', data: rows });
export const saveAllSyncFoods = (data: { shopId: number; foodCode?: string; foodName?: string }) =>
  request<number>({ method: 'POST', url: '/api/cost/foods/sync-all', data });
export const createCostDish = (data: {
  dishCode: string; dishName: string; externalDishCode?: string; sourceSystem: string;
}) => request<number>({ method: 'POST', url: '/api/cost/master-data/dishes', data });
export const fetchCostBoms = (storeId: number) =>
  request<CostBom[]>({ method: 'GET', url: '/api/cost/boms', params: { storeId } });
export const createCostBom = (data: {
  storeId: number; dishId: number; remark?: string;
  items: Array<{ materialId: number; unitId: number; quantity: number; sortOrder: number }>;
}) => request<number>({ method: 'POST', url: '/api/cost/boms', data });
export const fetchCostBomDetail = (id: number) =>
  request<CostBomDetail>({ method: 'GET', url: `/api/cost/boms/${id}` });
export const updateCostBom = (id: number, data: {
  expectedVersion: number; remark?: string;
  items: Array<{ materialId: number; unitId: number; quantity: number; sortOrder: number }>;
}) => request<void>({ method: 'PUT', url: `/api/cost/boms/${id}`, data });
export const submitCostBom = (id: number, expectedVersion: number) =>
  request<void>({ method: 'POST', url: `/api/cost/boms/${id}/submit`, data: { expectedVersion } });
export const publishCostBom = (id: number, expectedVersion: number) =>
  request<void>({ method: 'POST', url: `/api/cost/boms/${id}/publish`, data: { expectedVersion } });
export const rejectCostBom = (id: number, expectedVersion: number, remark?: string) =>
  request<void>({ method: 'POST', url: `/api/cost/boms/${id}/reject`, data: { expectedVersion, remark } });
export const fetchInventoryTasks = (storeId: number) =>
  request<InventoryTask[]>({ method: 'GET', url: '/api/cost/inventory-tasks', params: { storeId } });
export const createInventoryTask = (data: {
  storeId: number; taskName: string; plannedStartTime: string; plannedEndTime: string; remark?: string;
}) => request<number>({ method: 'POST', url: '/api/cost/inventory-tasks', data });
export const fetchInventoryTaskItems = (id: number) =>
  request<InventoryCountItem[]>({ method: 'GET', url: `/api/cost/inventory-tasks/${id}/items` });
export const approveInventoryTask = (id: number, expectedVersion: number, remark?: string) =>
  request<void>({ method: 'POST', url: `/api/cost/inventory-tasks/${id}/approve`, data: { expectedVersion, remark } });
export const rejectInventoryTask = (id: number, expectedVersion: number, remark?: string) =>
  request<void>({ method: 'POST', url: `/api/cost/inventory-tasks/${id}/reject`, data: { expectedVersion, remark } });
export const closeInventoryTask = (id: number, expectedVersion: number) =>
  request<void>({ method: 'POST', url: `/api/cost/inventory-tasks/${id}/close`, data: { expectedVersion } });
