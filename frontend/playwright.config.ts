import { defineConfig, devices } from '@playwright/test';

/**
 * E2E contra el stack completo levantado con docker compose (perfil demo).
 * E2E_BASE_URL: URL del frontend (por defecto http://localhost:8081).
 * PW_CHANNEL: navegador instalado a usar (p. ej. "msedge" en Windows); en CI se usa Chromium de Playwright.
 */
export default defineConfig({
  testDir: './e2e',
  testMatch: /.*\.e2e\.ts/,
  fullyParallel: false,
  workers: 1,
  retries: process.env['CI'] ? 1 : 0,
  timeout: 60_000,
  reporter: process.env['CI'] ? [['list'], ['html', { open: 'never' }]] : 'list',
  use: {
    baseURL: process.env['E2E_BASE_URL'] ?? 'http://localhost:8081',
    locale: 'es-EC',
    timezoneId: 'America/Guayaquil',
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
  },
  projects: [
    {
      name: 'desktop',
      use: {
        ...devices['Desktop Chrome'],
        viewport: { width: 1440, height: 900 },
        channel: process.env['PW_CHANNEL'] || undefined,
      },
    },
  ],
});
