import { Page } from '@playwright/test';
import { env } from './env';

/**
 * Open the Tester's Toolkit panel on the welcome page, configure settings, and save.
 *
 * Waits for the toolkit's initial GET /api/test-settings call to complete
 * before filling inputs (otherwise the async response overwrites our values).
 */
export async function configureToolkit(
  page: Page,
  options: { middleName: string; forceRenewal: boolean; testerEmail?: string }
): Promise<void> {
  // Listen before navigation, but surface an unavailable app immediately.
  // Promise.all also handles the pending response wait if navigation fails.
  const [settingsResponse] = await Promise.all([
    page.waitForResponse(
      resp => resp.url().includes('/api/test-settings') && resp.request().method() === 'GET'
    ),
    page.goto(`${env.contextPath}/welcome`).then(response => {
      if (!response || !response.ok()) {
        throw new Error(`Cannot configure Tester's Toolkit: welcome page returned HTTP ${response?.status() ?? 'unknown'} at ${page.url()}`);
      }
    }),
  ]);
  if (!settingsResponse.ok()) {
    throw new Error(`Cannot configure Tester's Toolkit: settings API returned HTTP ${settingsResponse.status()}`);
  }

  // Open the toolkit panel
  await page.getByRole('button', { name: 'Nástroje pro testování' }).click();
  await page.locator('#test-toolkit-middle-name').waitFor({ state: 'visible' });

  // Toggle force renewal if current state differs from desired
  const forceRenewalCheckbox = page.locator('#test-toolkit-force-renewal');
  const isChecked = await forceRenewalCheckbox.evaluate((el: HTMLInputElement) => el.checked);
  if (isChecked !== options.forceRenewal) {
    await Promise.all([
      page.waitForResponse(
        resp =>
          resp.url().includes('/api/test-settings') &&
          resp.request().method() === 'PUT' &&
          resp.url().includes(`forceRenewal=${encodeURIComponent(String(options.forceRenewal))}`)
      ),
      page.locator('.test-toolkit__switch-slider').click(),
    ]);
  }

  // Set middle name
  await page.locator('#test-toolkit-middle-name').fill(options.middleName);
  const testerEmail = options.testerEmail ?? env.testerEmail ?? '';
  await page.locator('#test-toolkit-email').fill(testerEmail);

  // Save middle name and wait until the PUT request is persisted.
  await Promise.all([
    page.waitForResponse(
      resp =>
        resp.url().includes('/api/test-settings') &&
        resp.request().method() === 'PUT' &&
        resp.url().includes(`middleName=${encodeURIComponent(options.middleName)}`) &&
        resp.url().includes(`testerEmail=${encodeURIComponent(testerEmail)}`)
    ),
    page.locator('#test-toolkit-save-mname').click(),
  ]);
}
