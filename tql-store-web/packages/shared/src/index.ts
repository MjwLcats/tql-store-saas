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
  administrator?: boolean;
}

export interface MenuItem {
  id: number;
  parentId: number;
  name: string;
  type: 'DIRECTORY' | 'MENU' | 'BUTTON';
  routeName?: string;
  path: string;
  componentKey: string;
  icon?: string;
  iconId?: number;
  iconSvg?: string;
  permission?: string;
  order: number;
  visible: number;
  status: number;
  children?: MenuItem[];
}

export interface MerchantOption {
  id: number;
  code: string;
  name: string;
}

export interface MerchantMenuItem {
  id: number;
  tenantId: number;
  parentId: number;
  name: string;
  type: 'DIRECTORY' | 'MENU' | 'BUTTON';
  routeName?: string;
  path?: string;
  componentKey?: string;
  icon?: string;
  iconId?: number;
  iconSvg?: string;
  permission?: string;
  order: number;
  visible: number;
  status: number;
  systemBuiltin: boolean;
  children?: MerchantMenuItem[];
}

export interface MerchantMenuSavePayload {
  tenantId: number;
  parentId: number;
  name: string;
  type: 'DIRECTORY' | 'MENU' | 'BUTTON';
  routeName?: string;
  path?: string;
  componentKey?: string;
  icon?: string;
  iconId?: number;
  permission?: string;
  order: number;
  visible: number;
  status: number;
}

export interface IconItem {
  id: number;
  name: string;
  code: string;
  category: string;
  sourceType: 'SYSTEM' | 'CUSTOM';
  svgContent?: string;
  status: number;
  order: number;
  usageCount: number;
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

export type ContentActivityStatus = 'DRAFT' | 'ACTIVE' | 'TERMINATED';
export type ContentPlanStatus = 'DRAFT' | 'ACTIVE' | 'ENDED' | 'TERMINATED';
export type ContentCreationMode = 'STANDARD_TEMPLATE' | 'AI_ASSISTED' | 'SELF_CREATED';
export type ContentTrainingPolicy = 'NONE' | 'REQUIRED' | 'DYNAMIC';

export interface ContentActivityItem {
  id: number;
  name: string;
  objective?: string;
  startTime: string;
  endTime: string;
  status: ContentActivityStatus;
  ownerId: number;
  ownerName: string;
  planCount: number;
  employeeCount: number;
  completedCount: number;
  createdTime: string;
}

export interface ContentPlanItem {
  id: number;
  activityId: number;
  name: string;
  taskInstruction: string;
  creationMode: ContentCreationMode;
  storyboardCount: number;
  trainingPolicy: ContentTrainingPolicy;
  deadline: string;
  status: ContentPlanStatus;
  currentVersionNo: number;
  employeeCount: number;
}

export interface CreateContentActivityPayload {
  name: string;
  objective?: string;
  startTime: string;
  endTime: string;
  ownerId?: number;
}

export interface UpdateContentPlanPayload {
  name: string;
  objective?: string;
  startTime: string;
  endTime: string;
  taskInstruction: string;
  creationMode: string;
  storyboardCount: number;
  trainingPolicy: string;
  employeeIds: number[];
}

export interface CreateContentPlanPayload {
  activityId: number;
  name: string;
  taskInstruction: string;
  creationMode: ContentCreationMode;
  storyboardCount?: number;
  trainingPolicy: ContentTrainingPolicy;
  deadline: string;
}

export interface ContentPrecheckResult {
  requestedCount: number;
  eligibleCount: number;
  duplicateCount: number;
  unavailableCount: number;
  failures: Array<{ employeeId: number; code: string; message: string }>;
}

export interface ContentPublishResult {
  planId: number;
  planVersionNo: number;
  result: 'SUCCESS' | 'PARTIAL_SUCCESS';
  createdCount: number;
  failedCount: number;
  failures: Array<{ employeeId: number; code: string; message: string }>;
}

export interface ContentDeliveryItem {
  taskId: number;
  employeeId: number;
  employeeNumber?: string;
  employeeName: string;
  organizationName?: string;
  storeName?: string;
  stage: string;
  createdTime: string;
  deadline: string;
  completionTime?: string;
}

export interface ContentAccountItem {
  id: number;
  platform: string;
  accountName: string;
  platformAccountId: string;
  accountType: string;
  organizationId?: number;
  organizationName?: string;
  employeeId: number;
  employeeName: string;
  employeeNumber?: string;
  status: 'ACTIVE' | 'PENDING' | 'FAILED' | 'AUTH_EXPIRED' | 'DISABLED';
  updateTime: string;
}

export interface ContentAccountPayload {
  platform: string;
  accountName: string;
  platformAccountId: string;
  accountType: string;
  organizationId?: number;
  employeeId: number;
}

export interface ContentVideoPerformanceItem {
  id: number;
  taskId?: number;
  accountId: number;
  accountName: string;
  platform: string;
  platformVideoId: string;
  videoTitle: string;
  videoUrl?: string;
  publishTime: string;
  viewCount: number;
  likeCount: number;
  commentCount: number;
  shareCount: number;
  favoriteCount: number;
  followerGain: number;
  conversionCount: number;
  transactionAmount: number;
  syncStatus: string;
  lastSyncTime?: string;
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
  organizationId?: number;
  organizationName?: string;
  email?: string;
  phone?: string;
  loginEnabled: boolean;
  sourceType: 'LOCAL' | 'HR_BUTLER';
  status: number;
  dataScope: DataScope;
  primaryStoreId?: number;
  primaryStoreName?: string;
  department?: string;
  position?: string;
  roleNames: string[];
}

export interface PersonnelImportResult {
  rowNumber: number;
  inputName: string;
  inputPhone: string;
  userId?: number;
  organizationStore: string;
  name: string;
  phone: string;
  department: string;
  position: string;
  status: 'VALID' | 'INVALID' | 'DUPLICATE' | 'DUPLICATE_USER' | 'MISMATCH' | 'NOT_FOUND';
  message: string;
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
  loginEnabled?: boolean;
  storeId?: number;
  organizationId?: number;
  page: number;
  pageSize: number;
}

export type SyncProvider = 'HUALALA' | 'HR_BUTLER' | 'KINGDEE';
export type SyncDataType = 'SHOP' | 'BILL' | 'DISH_SALES' | 'ORGANIZATION' | 'POSITION' | 'USER' | 'OUTBOUND';
export type SyncMode = 'INCREMENTAL' | 'FULL';
export type SyncTriggerType = 'MANUAL' | 'SCHEDULED' | 'RETRY';
export type SyncTaskStatus = 'PENDING' | 'RUNNING' | 'SUCCESS' | 'PARTIAL_SUCCESS' | 'FAILED';

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
  durationMs?: number;
  createTime: string;
}

export interface SyncTaskQuery {
  provider?: string;
  dataType?: string;
  status?: string;
  createdStart?: string;
  createdEnd?: string;
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
