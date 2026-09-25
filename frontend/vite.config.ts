/// <reference types="vitest/config" />
import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
  test: {
    environment: 'jsdom',
    globals: true,
    setupFiles: ['./src/test/setup.ts'],
    // e2e/ holds Playwright specs (its own test() implementation, run via
    // `npm run e2e`), not Vitest ones — exclude it from discovery.
    exclude: ['**/node_modules/**', '**/e2e/**'],
    // CSS Modules are processed regardless; plain CSS (with the Google Fonts
    // @import) is not needed in jsdom and would add a network dependency.
    css: false,
    // This machine has little RAM: cap Vitest to one worker, no isolation
    // overhead, or forked/threaded workers time out.
    pool: 'threads',
    maxWorkers: 1,
    isolate: false,
    testTimeout: 10000,
    coverage: {
      provider: 'v8',
      reporter: ['text', 'html'],
    },
  },
})
