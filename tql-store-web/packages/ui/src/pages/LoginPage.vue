<template>
  <main class="login-page">
    <section class="login-brand-panel">
      <div class="brand-top"><BrandMark :title="config.shortTitle" /></div>
      <div class="brand-copy">
        <h1>让每一家门店<br />都高效运转</h1>
        <p>{{ config.loginDescription }}</p>
        <div class="brand-points">
          <span><IconCheckCircleFill /> 统一门店运营标准</span>
          <span><IconCheckCircleFill /> 数据驱动经营决策</span>
          <span><IconCheckCircleFill /> 全链路任务协同</span>
        </div>
      </div>
      <p class="brand-footer">TQL Store SaaS · 安全、稳定、可扩展</p>
    </section>

    <section class="login-form-panel">
      <div class="login-card">
        <div class="mobile-brand"><BrandMark :title="config.shortTitle" /></div>
        <h2>{{ config.loginTitle }}</h2>
        <p class="login-subtitle">请输入账号信息进入{{ config.clientType === 'PLATFORM' ? '平台工作台' : '商家工作台' }}</p>
        <a-form ref="formRef" :model="form" layout="vertical" autocomplete="on" @submit-success="handleLogin">
          <a-form-item
            v-if="config.clientType === 'MERCHANT'"
            field="merchantNo"
            label="商户号"
            :rules="[{ required: true, message: '请输入商户号' }]"
          >
            <a-input v-model="form.merchantNo" name="merchantNo" autocomplete="off" size="large" placeholder="请输入商户号" allow-clear>
              <template #prefix><IconIdcard /></template>
            </a-input>
          </a-form-item>
          <a-form-item field="username" label="账号" :rules="[{ required: true, message: '请输入账号' }]">
            <a-input v-model="form.username" name="username" autocomplete="username" size="large" placeholder="请输入账号" allow-clear>
              <template #prefix><IconUser /></template>
            </a-input>
          </a-form-item>
          <a-form-item field="password" label="密码" :rules="[{ required: true, message: '请输入密码' }]">
            <a-input-password v-model="form.password" name="password" autocomplete="current-password" size="large" placeholder="请输入密码" allow-clear>
              <template #prefix><IconLock /></template>
            </a-input-password>
          </a-form-item>
          <div class="login-options"><a-checkbox v-model="remember">记住登录信息</a-checkbox></div>
          <a-button class="login-button" type="primary" size="large" html-type="submit" long :loading="loading">
            登录
          </a-button>
        </a-form>
      </div>
      <p class="copyright">© 2026 同庆楼门店运营 SaaS</p>
    </section>
  </main>
</template>

<script setup lang="ts">
import { inject, onMounted, reactive, ref, watch } from 'vue';
import { Message } from '@arco-design/web-vue';
import { IconCheckCircleFill, IconIdcard, IconLock, IconUser } from '@arco-design/web-vue/es/icon';
import { useRouter } from 'vue-router';
import { login } from '@tql-store/api';
import { setToken, useAppStore } from '@tql-store/auth';
import BrandMark from '../components/BrandMark.vue';
import { APP_CONFIG_KEY } from '../context';

const config = inject(APP_CONFIG_KEY)!;

const router = useRouter();
const store = useAppStore();
const loading = ref(false);
const remember = ref(true);
const rememberedLoginKey = `tql-store:${config.clientType.toLowerCase()}:remembered-login`;
const form = reactive({
  merchantNo: config.defaultMerchantNo || '',
  username: config.defaultUsername || '',
  password: config.defaultPassword || ''
});

onMounted(() => {
  try {
    const saved = localStorage.getItem(rememberedLoginKey);
    if (!saved) return;
    const value = JSON.parse(saved) as { merchantNo?: string; username?: string };
    form.merchantNo = typeof value.merchantNo === 'string' ? value.merchantNo : form.merchantNo;
    form.username = typeof value.username === 'string' ? value.username : form.username;
  } catch {
    localStorage.removeItem(rememberedLoginKey);
  }
});

watch(remember, (checked) => {
  if (!checked) localStorage.removeItem(rememberedLoginKey);
});

function saveRememberedLogin() {
  if (!remember.value) {
    localStorage.removeItem(rememberedLoginKey);
    return;
  }
  localStorage.setItem(rememberedLoginKey, JSON.stringify({
    merchantNo: config.clientType === 'MERCHANT' ? form.merchantNo.trim() : undefined,
    username: form.username.trim()
  }));
}

async function handleLogin() {
  loading.value = true;
  try {
    const result = await login(form.username, form.password, config.clientType, form.merchantNo);
    saveRememberedLogin();
    setToken(config.clientType, result.token);
    await store.loadContext(true);
    Message.success(`欢迎回来，${result.user.displayName}`);
    await router.replace('/dashboard');
  } catch (error) {
    Message.error(error instanceof Error ? error.message : '登录失败');
  } finally {
    loading.value = false;
  }
}
</script>

<style scoped>
.login-page { display: grid; width: 100%; height: 100%; min-height: 660px; grid-template-columns: minmax(420px, 42%) 1fr; background: #fff; }
.login-brand-panel { position: relative; display: flex; overflow: hidden; flex-direction: column; padding: 34px 48px; color: #fff; background: #172033; }
.login-brand-panel::before { position: absolute; top: -190px; right: -210px; width: 520px; height: 520px; content: ''; border: 1px solid rgba(64,128,255,.2); border-radius: 50%; box-shadow: 0 0 0 70px rgba(64,128,255,.035), 0 0 0 140px rgba(64,128,255,.025); }
.login-brand-panel::after { position: absolute; right: 36px; bottom: 60px; width: 230px; height: 190px; content: ''; opacity: .65; background-image: linear-gradient(rgba(64,128,255,.16) 1px, transparent 1px), linear-gradient(90deg, rgba(64,128,255,.16) 1px, transparent 1px); background-size: 28px 28px; mask-image: linear-gradient(135deg, transparent, #000); }
.brand-top { position: relative; z-index: 1; }
.brand-copy { position: relative; z-index: 1; max-width: 430px; margin: auto 0; }
.brand-copy h1 { margin: 0 0 24px; font-size: clamp(36px, 3.5vw, 54px); font-weight: 600; line-height: 1.25; letter-spacing: 1px; }
.brand-copy p { max-width: 370px; margin: 0 0 34px; color: #c9cdd4; font-size: 16px; line-height: 1.8; }
.brand-points { display: flex; flex-direction: column; gap: 15px; color: #e5e6eb; font-size: 14px; }
.brand-points span { display: flex; align-items: center; gap: 9px; }
.brand-points svg { color: #4080ff; font-size: 16px; }
.brand-footer { position: relative; z-index: 1; margin: 0; color: #6b778d; font-size: 12px; }
.login-form-panel { position: relative; display: flex; align-items: center; justify-content: center; padding: 56px; }
.login-card { width: 100%; max-width: 410px; }
.mobile-brand { display: none; }
.login-card h2 { margin: 0 0 10px; color: #1d2129; font-size: 28px; font-weight: 600; }
.login-subtitle { margin: 0 0 34px; color: #86909c; font-size: 14px; }
.login-options { display: flex; align-items: center; justify-content: space-between; margin: -2px 0 22px; color: #86909c; font-size: 12px; }
.login-button { height: 44px; font-weight: 500; }
.copyright { position: absolute; bottom: 24px; color: #c9cdd4; font-size: 12px; }
:deep(.arco-form-item-label-col) { font-weight: 500; }
:deep(.arco-input-wrapper) { height: 44px; background: #f7f8fa; border-color: transparent; }
:deep(.arco-input-wrapper:hover), :deep(.arco-input-focus) { background: #fff; border-color: #4080ff; }
@media (max-width: 1100px) { .login-page { grid-template-columns: 400px 1fr; } .login-brand-panel { padding: 32px; } }
</style>
