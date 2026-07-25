<template>
  <div class="container integration-sync-page">
    <a-card class="search-card tql-search-card">
      <div class="tql-search-layout">
        <div class="tql-search-form-area">
          <a-form
            class="search-form tql-search-form"
            :model="query"
            :label-col-props="{ flex: '64px' }"
            :wrapper-col-props="{ flex: '1' }"
            label-align="left"
          >
            <div class="tql-search-fields">
              <div class="tql-search-item">
                <a-form-item field="provider" label="数据来源">
                  <a-select v-model="query.provider" placeholder="全部来源" allow-clear>
                    <a-option value="HUALALA">哗啦啦</a-option>
                    <a-option value="HR_BUTLER">人力管家</a-option>
                  </a-select>
                </a-form-item>
              </div>
              <div class="tql-search-item">
                <a-form-item field="dataType" label="数据类型">
                  <a-select v-model="query.dataType" placeholder="全部类型" allow-clear>
                    <a-option value="SHOP">门店数据</a-option>
                    <a-option value="BILL">账单数据</a-option>
                    <a-option value="DISH_SALES">菜品销售</a-option>
                    <a-option value="ORGANIZATION">部门组织</a-option>
                    <a-option value="POSITION">岗位职位</a-option>
                    <a-option value="USER">员工用户</a-option>
                  </a-select>
                </a-form-item>
              </div>
              <div class="tql-search-item">
                <a-form-item field="status" label="同步状态">
                  <a-select v-model="query.status" placeholder="全部状态" allow-clear>
                    <a-option value="PENDING">等待执行</a-option>
                    <a-option value="RUNNING">执行中</a-option>
                    <a-option value="SUCCESS">成功</a-option>
                    <a-option value="FAILED">失败</a-option>
                  </a-select>
                </a-form-item>
              </div>
            </div>
          </a-form>
        </div>
        <a-divider class="search-divider tql-search-divider" direction="vertical" />
        <div class="search-actions tql-search-actions">
          <a-space>
            <a-button type="primary" @click="search"><template #icon><IconSearch /></template>查询</a-button>
            <a-button @click="reset"><template #icon><IconRefresh /></template>重置</a-button>
          </a-space>
        </div>
      </div>
    </a-card>

    <a-card class="general-card tql-list-card">
      <a-row class="table-toolbar">
        <a-col :span="12">
          <a-space>
            <a-button type="primary" @click="openManualModal">
              <template #icon><IconSync /></template>手动同步
            </a-button>
            <span class="record-total">共 {{ total }} 条</span>
          </a-space>
        </a-col>
        <a-col :span="12" class="table-actions">
          <a-button type="text" @click="load()"><template #icon><IconRefresh /></template>刷新</a-button>
        </a-col>
      </a-row>

      <a-table
        row-key="id"
        :columns="columns"
        :data="records"
        :loading="loading"
        :pagination="false"
        :bordered="false"
        :scroll="{ x: 1540 }"
      >
        <template #id="{ record }">#{{ record.id }}</template>
        <template #provider="{ record }">{{ providerLabel(record.provider) }}</template>
        <template #dataType="{ record }">{{ dataTypeLabel(record.dataType) }}</template>
        <template #syncRange="{ record }">{{ syncRange(record) }}</template>
        <template #syncMode="{ record }">{{ syncModeLabel(record.syncMode) }}</template>
        <template #triggerType="{ record }">
          <span>{{ triggerLabel(record.triggerType) }}</span>
          <a-link v-if="record.retryOf" class="retry-source">#{{ record.retryOf }}</a-link>
        </template>
        <template #status="{ record }">
          <a-tag :color="statusInfo(record.status).color">{{ statusInfo(record.status).label }}</a-tag>
        </template>
        <template #result="{ record }">
          <span v-if="record.totalCount > 0">
            {{ record.successCount }}/{{ record.totalCount }}
            <span v-if="record.failedCount" class="failed-count">失败 {{ record.failedCount }}</span>
          </span>
          <span v-else>—</span>
        </template>
        <template #createTime="{ record }">{{ formatTime(record.createTime) }}</template>
        <template #errorMessage="{ record }">
          <a-tooltip v-if="record.errorMessage" :content="record.errorMessage">
            <span class="error-message">{{ record.errorMessage }}</span>
          </a-tooltip>
          <span v-else>—</span>
        </template>
        <template #actions="{ record }">
          <a-space>
            <a-link @click="showLogs(record)">日志</a-link>
            <a-popconfirm
              v-if="record.status === 'FAILED'"
              content="确认重新执行该同步任务吗？"
              type="warning"
              @ok="retry(record)"
            >
              <a-link>重试</a-link>
            </a-popconfirm>
          </a-space>
        </template>
      </a-table>

      <div class="pagination-row">
        <a-pagination
          v-model:current="query.page"
          v-model:page-size="query.pageSize"
          :total="total"
          :show-total="true"
          :show-page-size="true"
          @change="load()"
          @page-size-change="handlePageSizeChange"
        />
      </div>
    </a-card>

    <a-modal
      v-model:visible="manualVisible"
      title="手动同步第三方数据"
      :mask-closable="false"
      :esc-to-close="!manualSubmitting"
      :closable="!manualSubmitting"
      width="560px"
    >
      <a-alert class="manual-tip" type="info" show-icon>
        任务将在后台异步执行，创建后可在同步列表中查看进度和详细日志。
      </a-alert>
      <a-form :model="manualForm" layout="vertical">
        <a-form-item label="数据来源" required>
          <a-select v-model="manualForm.provider" @change="handleProviderChange">
            <a-option value="HUALALA">哗啦啦</a-option>
            <a-option value="HR_BUTLER">人力管家</a-option>
          </a-select>
        </a-form-item>
        <a-form-item label="数据类型" required>
          <a-select v-model="manualForm.dataType">
            <a-option v-for="option in manualDataTypes" :key="option.value" :value="option.value">
              {{ option.label }}
            </a-option>
          </a-select>
        </a-form-item>
        <a-form-item label="同步方式" required>
          <a-radio-group v-model="manualForm.syncMode" type="button">
            <a-radio value="INCREMENTAL">增量同步</a-radio>
            <a-radio value="FULL">全量同步</a-radio>
          </a-radio-group>
        </a-form-item>
        <a-form-item v-if="requiresDateRange" label="营业日期" required>
          <a-range-picker
            v-model="manualForm.dateRange"
            value-format="YYYY-MM-DD"
            style="width: 100%"
            :allow-clear="false"
          />
          <template #extra>单次最多同步31天，退款和反结账数据将在重叠窗口内重新覆盖。</template>
        </a-form-item>
      </a-form>
      <template #footer>
        <a-button :disabled="manualSubmitting" @click="manualVisible = false">取消</a-button>
        <a-button type="primary" :loading="manualSubmitting" @click="submitManual">创建任务</a-button>
      </template>
    </a-modal>

    <a-drawer v-model:visible="logVisible" :width="560" :footer="false">
      <template #title>任务 #{{ activeTask?.id }} 执行日志</template>
      <a-spin :loading="logLoading" class="log-spin">
        <a-empty v-if="!logLoading && logs.length === 0" description="暂无执行日志" />
        <a-timeline v-else>
          <a-timeline-item
            v-for="log in logs"
            :key="log.id"
            :label="formatTime(log.createTime)"
            :dot-color="logColor(log.logLevel)"
          >
            <div class="log-entry">
              <div class="log-title">
                <a-tag :color="logColor(log.logLevel)" size="small">{{ log.stage }}</a-tag>
                <span>{{ log.message }}</span>
              </div>
              <div v-if="log.detail" class="log-detail">{{ log.detail }}</div>
            </div>
          </a-timeline-item>
        </a-timeline>
      </a-spin>
    </a-drawer>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue';
import { Message, type TableColumnData } from '@arco-design/web-vue';
import { IconRefresh, IconSearch, IconSync } from '@arco-design/web-vue/es/icon';
import { createSyncTask, fetchSyncLogs, fetchSyncTasks, retrySyncTask } from '@tql-store/api';
import type {
  SyncDataType,
  SyncLogItem,
  SyncMode,
  SyncProvider,
  SyncTaskItem,
  SyncTaskStatus
} from '@tql-store/shared';

const loading = ref(false);
const records = ref<SyncTaskItem[]>([]);
const total = ref(0);
const query = reactive({ provider: '', dataType: '', status: '', page: 1, pageSize: 10 });
const manualVisible = ref(false);
const manualSubmitting = ref(false);
const logVisible = ref(false);
const logLoading = ref(false);
const logs = ref<SyncLogItem[]>([]);
const activeTask = ref<SyncTaskItem>();
let poller: ReturnType<typeof setInterval> | undefined;

const today = localDateString(new Date());
const manualForm = reactive<{
  provider: SyncProvider;
  dataType: SyncDataType;
  syncMode: SyncMode;
  dateRange: string[];
}>({ provider: 'HUALALA', dataType: 'SHOP', syncMode: 'FULL', dateRange: [today, today] });

const manualDataTypes = computed(() => manualForm.provider === 'HUALALA'
  ? [
      { value: 'SHOP' as const, label: '门店数据' },
      { value: 'BILL' as const, label: '账单数据' },
      { value: 'DISH_SALES' as const, label: '菜品销售' }
    ]
  : [
      { value: 'ORGANIZATION' as const, label: '部门组织' },
      { value: 'POSITION' as const, label: '岗位职位' },
      { value: 'USER' as const, label: '员工用户' }
    ]);
const requiresDateRange = computed(() =>
  manualForm.provider === 'HUALALA' && manualForm.dataType !== 'SHOP');

const columns: TableColumnData[] = [
  { title: '任务编号', dataIndex: 'id', slotName: 'id', width: 100 },
  { title: '数据来源', dataIndex: 'provider', slotName: 'provider', width: 120 },
  { title: '数据类型', dataIndex: 'dataType', slotName: 'dataType', width: 120 },
  { title: '同步范围', slotName: 'syncRange', width: 190 },
  { title: '同步方式', dataIndex: 'syncMode', slotName: 'syncMode', width: 110 },
  { title: '触发方式', dataIndex: 'triggerType', slotName: 'triggerType', width: 120 },
  { title: '状态', dataIndex: 'status', slotName: 'status', width: 110 },
  { title: '同步结果', slotName: 'result', width: 140 },
  { title: '操作人', dataIndex: 'creatorName', width: 120 },
  { title: '创建时间', dataIndex: 'createTime', slotName: 'createTime', width: 180 },
  { title: '失败原因', dataIndex: 'errorMessage', slotName: 'errorMessage', width: 260 },
  { title: '操作', slotName: 'actions', width: 130, fixed: 'right' }
];

const statusMap: Record<SyncTaskStatus, { label: string; color: string }> = {
  PENDING: { label: '等待执行', color: 'gray' },
  RUNNING: { label: '执行中', color: 'blue' },
  SUCCESS: { label: '成功', color: 'green' },
  FAILED: { label: '失败', color: 'red' }
};

onMounted(() => {
  load();
  poller = setInterval(() => {
    if (records.value.some((item) => item.status === 'PENDING' || item.status === 'RUNNING')) {
      load(true);
    }
  }, 3000);
});

onBeforeUnmount(() => {
  if (poller) clearInterval(poller);
});

async function load(silent = false) {
  if (!silent) loading.value = true;
  try {
    const result = await fetchSyncTasks(query);
    records.value = result.records;
    total.value = result.total;
  } catch (error) {
    if (!silent) Message.error(error instanceof Error ? error.message : '同步任务加载失败');
  } finally {
    if (!silent) loading.value = false;
  }
}

function search() {
  query.page = 1;
  load();
}

function reset() {
  query.provider = '';
  query.dataType = '';
  query.status = '';
  query.page = 1;
  load();
}

function handlePageSizeChange(size: number) {
  query.pageSize = size;
  query.page = 1;
  load();
}

function openManualModal() {
  manualForm.provider = 'HUALALA';
  manualForm.dataType = 'SHOP';
  manualForm.syncMode = 'FULL';
  manualForm.dateRange = [today, today];
  manualVisible.value = true;
}

function handleProviderChange(value: unknown) {
  manualForm.dataType = value === 'HR_BUTLER' ? 'ORGANIZATION' : 'SHOP';
  manualForm.syncMode = 'FULL';
}

async function submitManual() {
  if (requiresDateRange.value && manualForm.dateRange.length !== 2) {
    Message.warning('请选择营业日期范围');
    return;
  }
  manualSubmitting.value = true;
  try {
    const taskId = await createSyncTask({
      provider: manualForm.provider,
      dataType: manualForm.dataType,
      syncMode: manualForm.syncMode,
      rangeStart: requiresDateRange.value ? manualForm.dateRange[0] : undefined,
      rangeEnd: requiresDateRange.value ? manualForm.dateRange[1] : undefined
    });
    manualVisible.value = false;
    Message.success(`同步任务 #${taskId} 已创建`);
    query.page = 1;
    await load();
  } catch (error) {
    Message.error(error instanceof Error ? error.message : '同步任务创建失败');
  } finally {
    manualSubmitting.value = false;
  }
}

async function retry(task: SyncTaskItem) {
  try {
    const taskId = await retrySyncTask(task.id);
    Message.success(`重试任务 #${taskId} 已创建`);
    query.page = 1;
    await load();
  } catch (error) {
    Message.error(error instanceof Error ? error.message : '任务重试失败');
  }
}

async function showLogs(task: SyncTaskItem) {
  activeTask.value = task;
  logs.value = [];
  logVisible.value = true;
  logLoading.value = true;
  try {
    logs.value = await fetchSyncLogs(task.id);
  } catch (error) {
    Message.error(error instanceof Error ? error.message : '同步日志加载失败');
  } finally {
    logLoading.value = false;
  }
}

function providerLabel(value: SyncProvider) {
  return value === 'HUALALA' ? '哗啦啦' : '人力管家';
}

function dataTypeLabel(value: SyncDataType) {
  return {
    SHOP: '门店数据',
    BILL: '账单数据',
    DISH_SALES: '菜品销售',
    ORGANIZATION: '部门组织',
    POSITION: '岗位职位',
    USER: '员工用户'
  }[value];
}

function syncModeLabel(value: SyncMode) {
  return value === 'FULL' ? '全量同步' : '增量同步';
}

function triggerLabel(value: SyncTaskItem['triggerType']) {
  return { MANUAL: '手动', SCHEDULED: '定时', RETRY: '失败重试' }[value];
}

function statusInfo(status: SyncTaskStatus) {
  return statusMap[status] || { label: status, color: 'gray' };
}

function syncRange(task: SyncTaskItem) {
  if (!task.rangeStart || !task.rangeEnd) {
    return {
      SHOP: '全部门店数据',
      BILL: '全部账单数据',
      DISH_SALES: '全部菜品销售数据',
      ORGANIZATION: '全部部门组织',
      POSITION: '全部岗位职位',
      USER: '全部员工用户'
    }[task.dataType];
  }
  return task.rangeStart === task.rangeEnd ? task.rangeStart : `${task.rangeStart} 至 ${task.rangeEnd}`;
}

function logColor(level: SyncLogItem['logLevel']) {
  return level === 'ERROR' ? 'red' : level === 'WARN' ? 'orange' : 'blue';
}

function formatTime(value?: string) {
  if (!value) return '—';
  return new Intl.DateTimeFormat('zh-CN', {
    year: 'numeric', month: '2-digit', day: '2-digit',
    hour: '2-digit', minute: '2-digit', second: '2-digit', hour12: false
  }).format(new Date(value));
}

function localDateString(date: Date) {
  const offset = date.getTimezoneOffset() * 60_000;
  return new Date(date.getTime() - offset).toISOString().slice(0, 10);
}
</script>

<style scoped>
.container { width: 100%; padding: 16px 20px 20px; }
.search-card { border-color: var(--tql-border); border-radius: var(--tql-radius-card); box-shadow: none; }
.general-card { border-color: var(--tql-border); border-radius: var(--tql-radius-card); box-shadow: none; }
.table-toolbar { margin-bottom: 16px; }
.table-actions { display: flex; align-items: center; justify-content: flex-end; }
.record-total { color: var(--tql-text-tertiary); font-size: 12px; }
.retry-source { margin-left: 4px; font-size: 12px; }
.failed-count { margin-left: 6px; color: var(--tql-danger); font-size: 12px; }
.error-message { display: inline-block; max-width: 220px; overflow: hidden; color: var(--tql-danger); text-overflow: ellipsis; white-space: nowrap; vertical-align: middle; }
.pagination-row { display: flex; justify-content: flex-end; padding-top: 16px; }
.manual-tip { margin-bottom: 20px; }
.log-spin { display: block; min-height: 180px; }
.log-entry { padding-bottom: 8px; }
.log-title { display: flex; align-items: center; gap: 8px; color: var(--tql-text-primary); }
.log-detail { margin-top: 6px; padding: 8px 10px; color: var(--tql-text-tertiary); background: var(--tql-bg-subtle); border-radius: var(--tql-radius-control); font-size: 12px; word-break: break-all; }
:deep(.arco-table-th) { background: var(--tql-bg-subtle); }
:deep(.arco-table-td), :deep(.arco-table-th) { height: 48px; }
</style>
