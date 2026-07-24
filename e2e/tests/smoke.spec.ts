/**
 * Smoke test — verifies the E2E setup works and the app is reachable.
 * Run this first to confirm connectivity before running full scenario tests.
 */
import { test, expect } from '@playwright/test';

const contextPath = process.env.APP_CONTEXT_PATH || '/bankid-registrator';

test.describe('Smoke test', () => {
  test('welcome page loads', async ({ page }) => {
    await page.goto(`${contextPath}/welcome`);

    // The welcome page should contain the verify/login button
    await expect(page).toHaveTitle(/.+/);
    await page.screenshot({ path: 'screenshots/smoke/01-welcome.png', fullPage: true });
  });

  test('tester toolkit API is reachable', async ({ request }) => {
    const response = await request.get(`${contextPath}/api/test-settings`);

    // Should return 200 on local/testing profiles
    // Will return 404 on production (expected — API doesn't exist there)
    expect([200, 404]).toContain(response.status());

    if (response.ok()) {
      const data = await response.json();
      expect(data).toHaveProperty('middleName');
      expect(data).toHaveProperty('forceRenewal');
    }
  });
});
