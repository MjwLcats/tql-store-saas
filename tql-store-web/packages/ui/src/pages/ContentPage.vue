<template>
  <div class="container content-page">
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
                <a-form-item field="keyword" label="关键词">
                  <a-input v-model="query.keyword" placeholder="请输入内容标题" allow-clear />
                </a-form-item>
              </div>
              <div class="tql-search-item">
                <a-form-item field="category" label="内容分类">
                  <a-select v-model="query.category" placeholder="全部分类" allow-clear>
                    <a-option value="运营通知">运营通知</a-option>
                    <a-option value="培训资料">培训资料</a-option>
                    <a-option value="经营分析">经营分析</a-option>
                    <a-option value="制度规范">制度规范</a-option>
                  </a-select>
                </a-form-item>
              </div>
              <div class="tql-search-item">
                <a-form-item field="status" label="状态">
                  <a-select v-model="query.status" placeholder="全部状态" allow-clear>
                    <a-option value="PUBLISHED">已发布</a-option>
                    <a-option value="DRAFT">草稿</a-option>
                    <a-option value="OFFLINE">已下线</a-option>
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
            <a-button type="primary"><template #icon><IconPlus /></template>新建内容</a-button>
            <span class="record-total">共 {{ total }} 条</span>
          </a-space>
        </a-col>
        <a-col :span="12" class="table-actions">
          <a-button type="text" @click="load"><template #icon><IconRefresh /></template>刷新</a-button>
        </a-col>
      </a-row>
      <a-table
        row-key="id"
        :columns="columns"
        :data="records"
        :loading="loading"
        :pagination="false"
        :bordered="false"
        :scroll="{ x: 900 }"
      >
        <template #title="{ record }">
          <button class="title-link" type="button">{{ record.title }}</button>
        </template>
        <template #status="{ record }">
          <a-tag :color="statusInfo(record.status).color">{{ statusInfo(record.status).label }}</a-tag>
        </template>
        <template #publishTime="{ record }">{{ formatTime(record.publishTime) }}</template>
        <template #actions>
          <a-space><a-link>查看</a-link><a-link>编辑</a-link></a-space>
        </template>
      </a-table>
      <div class="pagination-row">
        <a-pagination
          v-model:current="query.page"
          v-model:page-size="query.pageSize"
          :total="total"
          :show-total="true"
          :show-page-size="true"
          @change="load"
          @page-size-change="handlePageSizeChange"
        />
      </div>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue';
import { Message, type TableColumnData } from '@arco-design/web-vue';
import { IconPlus, IconRefresh, IconSearch } from '@arco-design/web-vue/es/icon';
import { fetchContents } from '@tql-store/api';
import type { ContentItem, ContentStatus } from '@tql-store/shared';
const loading = ref(false);
const records = ref<ContentItem[]>([]);
const total = ref(0);
const query = reactive({ keyword: '', category: '', status: '', page: 1, pageSize: 10 });
const columns: TableColumnData[] = [
  { title: '标题', dataIndex: 'title', slotName: 'title', width: 300 },
  { title: '分类', dataIndex: 'category', width: 130 },
  { title: '所属门店', dataIndex: 'storeName', width: 180 },
  { title: '状态', dataIndex: 'status', slotName: 'status', width: 110 },
  { title: '负责人', dataIndex: 'owner', width: 120 },
  { title: '发布时间', dataIndex: 'publishTime', slotName: 'publishTime', width: 180 },
  { title: '操作', slotName: 'actions', width: 130, fixed: 'right' }
];
const statusMap: Record<ContentStatus, { label: string; color: string }> = {
  PUBLISHED: { label: '已发布', color: 'green' },
  DRAFT: { label: '草稿', color: 'blue' },
  OFFLINE: { label: '已下线', color: 'gray' }
};
const statusInfo = (status: ContentStatus) => statusMap[status] || { label: status, color: 'gray' };

onMounted(load);

async function load() {
  loading.value = true;
  try {
    const result = await fetchContents(query);
    records.value = result.records;
    total.value = result.total;
  } catch (error) {
    Message.error(error instanceof Error ? error.message : '内容列表加载失败');
  } finally {
    loading.value = false;
  }
}

function search() { query.page = 1; load(); }
function reset() {
  query.keyword = '';
  query.category = '';
  query.status = '';
  query.page = 1;
  load();
}
function handlePageSizeChange(size: number) { query.pageSize = size; query.page = 1; load(); }
function formatTime(value?: string) {
  if (!value) return '—';
  return new Intl.DateTimeFormat('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit', hour12: false }).format(new Date(value));
}
</script>

<style scoped>
.container { width: 100%; padding: 16px 20px 20px; }
.search-card { border-color: var(--tql-border); border-radius: var(--tql-radius-card); box-shadow: none; }
.general-card { border-color: var(--tql-border); border-radius: var(--tql-radius-card); box-shadow: none; }
.table-toolbar { margin-bottom: 16px; }
.table-actions { display: flex; align-items: center; justify-content: flex-end; }
.record-total { color: var(--tql-text-tertiary); font-size: 12px; }
.title-link { padding: 0; color: var(--tql-text-primary); background: none; border: 0; cursor: pointer; text-align: left; }
.title-link:hover { color: var(--tql-primary); }
.pagination-row { display: flex; justify-content: flex-end; padding-top: 16px; }
:deep(.arco-table-th) { background: var(--tql-bg-subtle); }
:deep(.arco-table-td), :deep(.arco-table-th) { height: 48px; }
</style>
