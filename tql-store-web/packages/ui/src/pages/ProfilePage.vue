<template>
  <div class="page profile-page">
    <div class="page-heading"><h1>个人中心</h1><p>查看当前登录账号与所属组织信息</p></div>
    <section class="profile-panel">
      <div class="profile-summary">
        <a-avatar :size="72" class="profile-avatar">{{ store.profile?.displayName?.slice(0, 1) }}</a-avatar>
        <div><h2>{{ store.profile?.displayName }}</h2><p>{{ store.profile?.username }}</p><a-tag color="arcoblue">{{ clientLabel }}</a-tag></div>
      </div>
      <a-divider />
      <a-descriptions :column="2" bordered size="large">
        <a-descriptions-item label="所属组织">{{ store.profile?.tenantName }}</a-descriptions-item>
        <a-descriptions-item label="租户 ID">{{ store.profile?.tenantId || '平台级账号' }}</a-descriptions-item>
        <a-descriptions-item label="登录账号">{{ store.profile?.username }}</a-descriptions-item>
        <a-descriptions-item label="显示名称">{{ store.profile?.displayName }}</a-descriptions-item>
        <a-descriptions-item label="手机号码">{{ store.profile?.phone || '未设置' }}</a-descriptions-item>
        <a-descriptions-item label="电子邮箱">{{ store.profile?.email || '未设置' }}</a-descriptions-item>
        <a-descriptions-item label="所属主门店">{{ store.profile?.primaryStoreName || '不限定门店' }}</a-descriptions-item>
        <a-descriptions-item label="数据权限">{{ scopeLabel }}</a-descriptions-item>
      </a-descriptions>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { useAppStore } from '@tql-store/auth';
const store = useAppStore();
const clientLabel = computed(() => store.profile?.clientType === 'PLATFORM' ? 'SaaS 平台管理员' : '商家运营管理员');
const scopeMap: Record<string, string> = { ALL: '全部数据', STORE_AND_CHILD: '本门店及下级门店', STORE: '本门店', CUSTOM: '自定义门店', SELF: '仅本人数据', DEPT_AND_CHILD: '本部门及下级部门', DEPT: '本部门' };
const scopeLabel = computed(() => scopeMap[store.profile?.dataScope || 'SELF']);
</script>

<style scoped>
.page { width: 100%; padding: var(--tql-page-padding); }
.page-heading { margin: 18px 0; }
.page-heading h1 { margin: 0 0 7px; font-size: 22px; font-weight: 600; }
.page-heading p { margin: 0; color: var(--tql-text-tertiary); font-size: 13px; }
.profile-panel { padding: var(--tql-space-8); background: var(--tql-bg-card); border: 1px solid var(--tql-border); border-radius: var(--tql-radius-card); }
.profile-summary { display: flex; align-items: center; gap: 22px; }
.profile-avatar { color: var(--tql-primary); background: var(--tql-primary-soft); font-size: 27px; font-weight: 600; }
.profile-summary h2 { margin: 0 0 5px; font-size: 20px; }
.profile-summary p { margin: 0 0 10px; color: var(--tql-text-tertiary); }
:deep(.arco-descriptions-item-label) { width: 130px; color: var(--tql-text-tertiary); }
</style>
