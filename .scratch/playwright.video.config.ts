import { defineConfig } from '@playwright/test';
import base from '../playwright.config';

export default defineConfig({
  ...base,
  testDir: '../e2e',
  webServer: { ...base.webServer, cwd: '..' },
  use: {
    ...base.use,
    video: { mode: 'on', scale: 1, size: { width: 1440, height: 900 } },
  },
});
