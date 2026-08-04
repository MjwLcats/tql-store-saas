<template>
  <div class="page dashboard-page">
    <section class="welcome-panel">
      <div>
        <h1>{{ greeting }}，{{ store.profile?.displayName }}</h1>
        <p>{{ today }}，愿今天的每一家门店都运转顺畅。</p>
      </div>
      <a-button type="primary" @click="router.push('/content')">进入内容管理</a-button>
    </section>
    <section class="overview-strip">
      <div><span>当前组织</span><strong>{{ store.profile?.tenantName }}</strong></div>
      <div><span>登录端</span><strong>{{ config.clientType === 'PLATFORM' ? 'SaaS 平台端' : '商家运营端' }}</strong></div>
      <div><span>可用菜单</span><strong>{{ store.menus.length }} 项</strong></div>
    </section>
    <section class="guide-section">
      <div class="section-heading"><h2>开始工作</h2><p>常用功能入口</p></div>
      <div class="guide-list">
        <button type="button" @click="router.push('/content')"><IconFile /><span><strong>内容管理</strong><small>查询和查看运营内容</small></span><IconRight /></button>
        <button type="button" @click="router.push('/profile')"><IconUser /><span><strong>个人中心</strong><small>查看当前账号与租户信息</small></span><IconRight /></button>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, inject } from 'vue';
import { IconFile, IconRight, IconUser } from '@arco-design/web-vue/es/icon';
import { useRouter } from 'vue-router';
import { useAppStore } from '@tql-store/auth';
import { APP_CONFIG_KEY } from '../context';
const config = inject(APP_CONFIG_KEY)!;
const store = useAppStore();
const router = useRouter();
const hour = new Date().getHours();
const greeting = computed(() => hour < 12 ? '早上好' : hour < 18 ? '下午好' : '晚上好');
const today = new Intl.DateTimeFormat('zh-CN', { year: 'numeric', month: 'long', day: 'numeric', weekday: 'long' }).format(new Date());
</script>

<style scoped>
.page { width: 100%; padding: 16px 20px 20px; }
.welcome-panel { display: flex; min-height: 142px; align-items: center; justify-content: space-between; margin-top: var(--tql-space-5); padding: var(--tql-space-6) var(--tql-space-8); background: var(--tql-bg-card); border: 1px solid var(--tql-border); border-radius: var(--tql-radius-card); }
.welcome-panel h1 { margin: 0 0 10px; font-size: 24px; font-weight: 600; }
.welcome-panel p { margin: 0; color: var(--tql-text-tertiary); }
.overview-strip { display: grid; margin-top: var(--tql-space-4); background: var(--tql-bg-card); border: 1px solid var(--tql-border); border-radius: var(--tql-radius-card); grid-template-columns: repeat(3, 1fr); }
.overview-strip div { display: flex; min-height: 92px; flex-direction: column; justify-content: center; padding: 20px 28px; border-right: 1px solid var(--tql-border); }
.overview-strip div:last-child { border-right: 0; }
.overview-strip span { margin-bottom: var(--tql-space-2); color: var(--tql-text-tertiary); font-size: 13px; }
.overview-strip strong { font-size: 18px; font-weight: 500; }
.guide-section { margin-top: var(--tql-space-4); padding: var(--tql-space-6) 28px var(--tql-space-3); background: var(--tql-bg-card); border: 1px solid var(--tql-border); border-radius: var(--tql-radius-card); }
.section-heading { display: flex; align-items: baseline; gap: 12px; }
.section-heading h2 { margin: 0; font-size: 17px; }
.section-heading p { margin: 0; color: var(--tql-text-tertiary); font-size: 12px; }
.guide-list { margin-top: 14px; }
.guide-list button { display: grid; width: 100%; align-items: center; padding: var(--tql-space-4) var(--tql-space-1); color: var(--tql-text-secondary); background: transparent; border: 0; border-bottom: 1px solid var(--tql-border-light); cursor: pointer; grid-template-columns: 36px 1fr 20px; text-align: left; }
.guide-list button:hover { color: var(--tql-primary); }
.guide-list button > svg:first-child { font-size: 20px; }
.guide-list span { display: flex; flex-direction: column; gap: 5px; }
.guide-list strong { color: var(--tql-text-primary); font-size: 14px; font-weight: 500; }
.guide-list small { color: var(--tql-text-tertiary); }
@media (max-width: 720px) {
  .page { padding: var(--tql-page-padding); }
  .welcome-panel { min-height: 0; align-items: stretch; flex-direction: column; gap: var(--tql-space-4); margin-top: 0; padding: var(--tql-space-5); }
  .welcome-panel h1 { margin-bottom: var(--tql-space-2); font-size: 22px; }
  .welcome-panel p { line-height: 1.6; }
  .welcome-panel :deep(.arco-btn) { width: 100%; }
  .overview-strip { grid-template-columns: 1fr; }
  .overview-strip div { min-height: 72px; padding: var(--tql-space-4) var(--tql-space-5); border-right: 0; border-bottom: 1px solid var(--tql-border); }
  .overview-strip div:last-child { border-bottom: 0; }
  .guide-section { padding: var(--tql-space-5); }
  .section-heading { align-items: flex-start; flex-direction: column; gap: var(--tql-space-1); }
}
</style>
