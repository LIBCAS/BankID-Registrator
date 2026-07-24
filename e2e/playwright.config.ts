import { defineConfig, devices } from '@playwright/test';
import dotenv from 'dotenv';
import path from 'path';

dotenv.config({ path: path.resolve(__dirname, '.env') });

const baseURL = process.env.BASE_URL;
if (!baseURL) {
  throw new Error(
    'Required environment variable BASE_URL is not set. ' +
    'Define it in your e2e/.env file. See e2e/.env.example for reference.'
  );
}

const videoMode =
  (process.env.E2E_VIDEO_MODE as 'off' | 'on' | 'retain-on-failure' | 'on-first-retry')
  || 'on-first-retry';

export default defineConfig({
  testDir: './tests',
  outputDir: './test-results',

  /* Maximum time one test can run */
  timeout: 120_000,

  /* Expect timeout for assertions */
  expect: {
    timeout: 10_000,
  },

  /* Fail the build on CI if you accidentally left test.only in the source code */
  forbidOnly: !!process.env.CI,

  /* Retry failed tests once */
  retries: process.env.CI ? 1 : 0,

  /* Run tests sequentially — scenarios depend on unique Aleph state */
  workers: 1,

  /* Reporter */
  reporter: [
    ['html', { open: 'never', outputFolder: 'playwright-report' }],
    ['list'],
  ],

  use: {
    baseURL,

    /* Collect trace on first retry */
    trace: 'on-first-retry',

    /* Screenshot on failure */
    screenshot: 'only-on-failure',

    /* Optional video recording, configurable via E2E_VIDEO_MODE */
    video: videoMode,

    /* Default navigation timeout */
    navigationTimeout: 30_000,

    /* Default action timeout */
    actionTimeout: 15_000,
  },

  projects: [
    {
      name: 'chromium',
      use: {
        ...devices['Desktop Chrome'],
        viewport: { width: 1700, height: 1400 },
      },
    },
  ],
});
