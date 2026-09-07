import { expect, test } from '@playwright/test';
import { configureToolkit } from '../helpers/toolkit';

test('unavailable welcome page reports HTTP 503 instead of a toolkit timeout', async ({ page }) => {
  await page.route('**/*', route => route.fulfill({
    status: 503,
    contentType: 'text/html',
    body: '<h1>Service Unavailable</h1>',
  }));
  await expect(configureToolkit(page, { middleName: 'Test', forceRenewal: false }))
    .rejects.toThrow('welcome page returned HTTP 503');
});
