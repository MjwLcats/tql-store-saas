<template>
  <div class="brand-mark" :class="[{ compact }, tone]" aria-label="同庆楼门店运营">
    <span class="brand-symbol"><span></span><span></span><span></span></span>
    <span v-if="!compact" class="brand-name">{{ title }}</span>
  </div>
</template>

<script setup lang="ts">
withDefaults(defineProps<{ title: string; compact?: boolean; tone?: 'light' | 'dark' }>(), {
  compact: false,
  tone: 'dark'
});
</script>

<style scoped>
.brand-mark { display: flex; align-items: center; gap: 11px; min-width: 0; }
.brand-symbol { position: relative; width: 30px; height: 28px; flex: 0 0 30px; }
.brand-symbol span { position: absolute; width: 14px; height: 14px; border-radius: 4px 9px 4px 9px; transform: rotate(45deg); }
.brand-symbol span:nth-child(1) { left: 1px; top: 7px; background: var(--tql-brand-cyan); }
.brand-symbol span:nth-child(2) { left: 9px; top: 1px; background: var(--tql-primary-hover); }
.brand-symbol span:nth-child(3) { left: 16px; top: 9px; background: var(--tql-primary); }
.brand-name { overflow: hidden; color: var(--tql-color-white); font-size: 17px; font-weight: 600; letter-spacing: .2px; white-space: nowrap; }
.light .brand-name { color: var(--tql-text-primary); }
.compact { justify-content: center; }
</style>
