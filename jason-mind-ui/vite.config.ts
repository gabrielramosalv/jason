import { fileURLToPath, URL } from 'node:url'

import { defineConfig } from 'vitest/config'
import vue from '@vitejs/plugin-vue'
import tailwindcss from '@tailwindcss/vite'

// Em desenvolvimento, o Vite encaminha /api ao Mind API (D-154). Em produção a UI e a API
// compartilham a origem, servidas pelo Peel Web.
const mindApi = process.env.MIND_API_URL ?? 'http://127.0.0.1:3272'

export default defineConfig({
  server: {
    port: 5173,
    proxy: {
      '/api': { target: mindApi, changeOrigin: false },
    },
  },
  plugins: [vue(), tailwindcss()],
  test: {
    environment: 'happy-dom',
    include: ['src/**/*.test.ts'],
  },
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
})
