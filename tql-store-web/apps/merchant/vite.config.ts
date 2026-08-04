import { defineConfig } from 'vite';
import vue from '@vitejs/plugin-vue';

const manualChunks = (id: string) => {
  if (!id.includes('node_modules')) return undefined;
  if (id.includes('@arco-design')) return 'arco-design';
  if (/[\\/]node_modules[\\/](vue|vue-router|pinia)[\\/]/.test(id)) return 'vue-vendor';
  if (id.includes('axios')) return 'http-vendor';
  return 'vendor';
};

export default defineConfig({
  plugins: [vue()],
  resolve: { dedupe: ['vue'] },
  server: { port: 3101, strictPort: true },
  build: { rollupOptions: { output: { manualChunks } } },
});
