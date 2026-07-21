export type ClientType = 'PLATFORM' | 'MERCHANT';

export interface AppConfig {
  clientType: ClientType;
  appName: string;
  appTitle: string;
  shortTitle: string;
  loginTitle: string;
  loginDescription: string;
  defaultMerchantNo?: string;
  defaultUsername: string;
  defaultPassword: string;
}

export interface ApiResponse<T> {
  code: number;
  message: string;
  data: T;
  traceId?: string;
}

export interface LoginResponse {
  token: string;
  expiresIn: number;
  user: {
    id: number;
    tenantId: number;
    username: string;
    displayName: string;
    clientType: ClientType;
  };
}

export interface ChangePasswordPayload {
  currentPassword: string;
  newPassword: string;
}

export interface UserProfile {
  id: number;
  tenantId: number;
  tenantName: string;
  primaryStoreId?: number;
  primaryStoreName?: string;
  dataScope: DataScope;
  username: string;
  displayName: string;
  email?: string;
  phone?: string;
  clientType: ClientType;
}

export interface MenuItem {
  id: number;
  name: string;
  path: string;
  componentKey: string;
  icon?: string;
  permission?: string;
  order: number;
}

export type ContentStatus = 'DRAFT' | 'PUBLISHED' | 'OFFLINE';

export interface ContentItem {
  id: number;
  tenantId: number;
  storeId?: number;
  storeName?: string;
  title: string;
  category: string;
  status: ContentStatus;
  owner: string;
  publishTime?: string;
}

export interface PageResult<T> {
  records: T[];
  total: number;
  page: number;
  pageSize: number;
}

export interface ContentQuery {
  keyword?: string;
  category?: string;
  status?: string;
  page: number;
  pageSize: number;
}

export type DataScope =
  | 'ALL'
  | 'DEPT_AND_CHILD'
  | 'DEPT'
  | 'STORE_AND_CHILD'
  | 'STORE'
  | 'SELF'
  | 'CUSTOM';

export interface StoreOption {
  id: number;
  parentId: number;
  code: string;
  name: string;
}

export interface OrganizationOption {
  id: number;
  parentId?: number;
  code?: string;
  name: string;
  disabled?: boolean;
  children?: OrganizationOption[];
}

export interface RoleItem {
  id: number;
  code: string;
  name: string;
  status: number;
  remark?: string;
  menuIds: number[];
  menuCount: number;
  userCount: number;
}

export interface RoleSavePayload {
  roleCode: string;
  roleName: string;
  status: number;
  remark?: string;
  menuIds: number[];
}

export interface UserItem {
  id: number;
  username?: string;
  employeeNumber?: string;
  displayName: string;
  organizationName?: string;
  email?: string;
  phone?: string;
  loginEnabled: boolean;
  sourceType: 'LOCAL' | 'HR_BUTLER';
  status: number;
  dataScope: DataScope;
  primaryStoreId?: number;
  primaryStoreName?: string;
  roleNames: string[];
}

export interface UserDetail {
  id: number;
  username?: string;
  employeeNumber?: string;
  loginEnabled: boolean;
  sourceType: 'LOCAL' | 'HR_BUTLER';
  organizationId?: number;
  organizationName?: string;
  displayName: string;
  email?: string;
  phone?: string;
  status: number;
  dataScope: DataScope;
  primaryStoreId?: number;
  roleIds: number[];
  storeIds: number[];
}

export interface UserSavePayload {
  username?: string;
  password?: string;
  loginEnabled: boolean;
  organizationId?: number;
  displayName: string;
  email?: string;
  phone?: string;
  status: number;
  dataScope: DataScope;
  primaryStoreId?: number;
  roleIds: number[];
  storeIds: number[];
}

export interface UserQuery {
  keyword?: string;
  status?: number;
  storeId?: number;
  organizationId?: number;
  page: number;
  pageSize: number;
}

export type SyncProvider = 'HUALALA' | 'HR_BUTLER';
export type SyncDataType = 'SHOP' | 'BILL' | 'DISH_SALES' | 'ORGANIZATION' | 'POSITION' | 'USER';
export type SyncMode = 'INCREMENTAL' | 'FULL';
export type SyncTriggerType = 'MANUAL' | 'SCHEDULED' | 'RETRY';
export type SyncTaskStatus = 'PENDING' | 'RUNNING' | 'SUCCESS' | 'FAILED';

export interface SyncTaskItem {
  id: number;
  provider: SyncProvider;
  dataType: SyncDataType;
  syncMode: SyncMode;
  triggerType: SyncTriggerType;
  retryOf?: number;
  rangeStart?: string;
  rangeEnd?: string;
  status: SyncTaskStatus;
  totalCount: number;
  successCount: number;
  failedCount: number;
  errorMessage?: string;
  createdBy: number;
  creatorName: string;
  startedAt?: string;
  finishedAt?: string;
  createTime: string;
}

export interface SyncTaskQuery {
  provider?: string;
  dataType?: string;
  status?: string;
  page: number;
  pageSize: number;
}

export interface SyncTaskCreatePayload {
  provider: SyncProvider;
  dataType: SyncDataType;
  syncMode: SyncMode;
  rangeStart?: string;
  rangeEnd?: string;
}

export interface SyncLogItem {
  id: number;
  logLevel: 'INFO' | 'WARN' | 'ERROR';
  stage: string;
  message: string;
  detail?: string;
  createTime: string;
}
