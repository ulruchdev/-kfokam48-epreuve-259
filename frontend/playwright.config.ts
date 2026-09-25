import { defineConfig, devices } from '@playwright/test'

/**
 * The app is started by `docker compose up --build` (frontend on :5173,
 * proxying /api to the backend), not by this config: there is no webServer
 * entry here on purpose.
 */
export default defineConfig({
  testDir: './e2e',
  // The full flow drives three browser contexts against a backend that may have just
  // restarted (cold JVM / Hibernate): 30 s is too tight on a modest machine, 60 s is not.
  timeout: 60_000,
  fullyParallel: false,
  workers: 1,
  retries: 0,
  reporter: [['html', { outputFolder: 'playwright-report', open: 'never' }]],
  use: {
    baseURL: 'http://localhost:5173',
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
  },
  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] },
    },
    {
      // npm run e2e:headed — a slower, visible run for watching the flow.
      name: 'chromium-headed',
      // slowMo 400 ms on three browser contexts: the default 30 s is not enough to watch the flow
      timeout: 180_000,
      use: {
        ...devices['Desktop Chrome'],
        headless: false,
        launchOptions: { slowMo: 400 },
      },
    },
  ],
})
