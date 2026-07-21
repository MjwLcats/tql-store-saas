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
      <a-divider />
      <div class="security-setting">
        <div>
          <h3>登录密码</h3>
          <p>定期更新登录密码有助于保护账号安全，修改成功后需要重新登录。</p>
        </div>
        <a-button type="outline" @click="openPasswordDialog">
          <template #icon><IconLock /></template>
          修改密码
        </a-button>
      </div>
    </section>

    <a-modal
      v-model:visible="passwordDialogVisible"
      title="修改登录密码"
      ok-text="确认修改"
      cancel-text="取消"
      :ok-loading="passwordLoading"
      :on-before-ok="handleChangePassword"
      unmount-on-close
      @cancel="resetPasswordForm"
    >
      <a-alert class="password-tip">密码长度应为 8–64 位，修改成功后当前会话将自动退出。</a-alert>
      <a-form ref="passwordFormRef" :model="passwordForm" layout="vertical">
        <a-form-item field="currentPassword" label="当前密码" :rules="[{ required: true, message: '请输入当前密码' }]">
          <a-input-password v-model="passwordForm.currentPassword" autocomplete="current-password" placeholder="请输入当前密码" allow-clear />
        </a-form-item>
        <a-form-item
          field="newPassword"
          label="新密码"
          :rules="[
            { required: true, message: '请输入新密码' },
            { minLength: 8, message: '新密码至少 8 位' },
            { maxLength: 64, message: '新密码不能超过 64 位' }
          ]"
        >
          <a-input-password v-model="passwordForm.newPassword" autocomplete="new-password" placeholder="请输入 8–64 位新密码" allow-clear />
        </a-form-item>
        <a-form-item field="confirmPassword" label="确认新密码" :rules="[{ required: true, message: '请再次输入新密码' }]">
          <a-input-password v-model="passwordForm.confirmPassword" autocomplete="new-password" placeholder="请再次输入新密码" allow-clear />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { computed, inject, reactive, ref } from 'vue';
import type { FormInstance } from '@arco-design/web-vue';
import { Message } from '@arco-design/web-vue';
import { IconLock } from '@arco-design/web-vue/es/icon';
import { useRouter } from 'vue-router';
import { changePassword } from '@tql-store/api';
import { clearToken, useAppStore } from '@tql-store/auth';
import { APP_CONFIG_KEY } from '../context';

const config = inject(APP_CONFIG_KEY)!;
const store = useAppStore();
const router = useRouter();
const passwordDialogVisible = ref(false);
const passwordLoading = ref(false);
const passwordFormRef = ref<FormInstance>();
const passwordForm = reactive({ currentPassword: '', newPassword: '', confirmPassword: '' });
const clientLabel = computed(() => store.profile?.clientType === 'PLATFORM' ? 'SaaS 平台管理员' : '商家运营管理员');
const scopeMap: Record<string, string> = { ALL: '全部数据', STORE_AND_CHILD: '本门店及下级门店', STORE: '本门店', CUSTOM: '自定义门店', SELF: '仅本人数据', DEPT_AND_CHILD: '本部门及下级部门', DEPT: '本部门' };
const scopeLabel = computed(() => scopeMap[store.profile?.dataScope || 'SELF']);

function resetPasswordForm() {
  Object.assign(passwordForm, { currentPassword: '', newPassword: '', confirmPassword: '' });
  passwordFormRef.value?.clearValidate();
}

function openPasswordDialog() {
  resetPasswordForm();
  passwordDialogVisible.value = true;
}

async function handleChangePassword() {
  const errors = await passwordFormRef.value?.validate();
  if (errors) return false;
  if (passwordForm.newPassword === passwordForm.currentPassword) {
    Message.warning('新密码不能与当前密码相同');
    return false;
  }
  if (passwordForm.newPassword !== passwordForm.confirmPassword) {
    Message.warning('两次输入的新密码不一致');
    return false;
  }

  passwordLoading.value = true;
  try {
    await changePassword({
      currentPassword: passwordForm.currentPassword,
      newPassword: passwordForm.newPassword
    });
    passwordDialogVisible.value = false;
    clearToken(config.clientType);
    store.reset();
    await router.replace('/login');
    Message.success('密码修改成功，请使用新密码重新登录');
    return true;
  } catch (error) {
    Message.error(error instanceof Error ? error.message : '密码修改失败');
    return false;
  } finally {
    passwordLoading.value = false;
  }
}
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
.security-setting { display: flex; align-items: center; justify-content: space-between; gap: 24px; }
.security-setting h3 { margin: 0 0 7px; color: var(--tql-text-primary); font-size: 16px; font-weight: 600; }
.security-setting p { margin: 0; color: var(--tql-text-tertiary); font-size: 13px; line-height: 1.6; }
.password-tip { margin-bottom: 20px; }
</style>
