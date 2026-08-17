import { defineConfig } from '@playwright/test';

export default defineConfig({
  testDir: './e2e',
  fullyParallel: false,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 1 : 0,
  workers: 1,
  reporter: 'list',
  use: {
    baseURL: 'http://localhost:8080',
    trace: 'on-first-retry',
    video: process.env.CI ? 'on' : 'off',
  },
  webServer: {
    command: './gradlew bootRun --args="--spring.profiles.active=e2e"',
    url: 'http://localhost:8080/classifications',
    reuseExistingServer: !process.env.CI,
    timeout: 120000,
  },
});
