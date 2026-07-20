import { defineConfig } from 'vite';
import vue from '@vitejs/plugin-vue';
export default defineConfig({ plugins: [vue()], resolve: { dedupe: ['vue'] }, server: { port: 3101, strictPort: true } });
