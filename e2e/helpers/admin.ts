/**
 * Admin dashboard helper using a separate browser context.
 *
 * A separate context means a separate JSESSIONID cookie, so the admin session
 * is fully independent of the customer's BankID session (no shared-session issues).
 */
import { Browser, BrowserContext, Page } from '@playwright/test';
import { env } from './env';
import { ScreenshotHelper } from './screenshots';

export class AdminHelper {
  private context: BrowserContext | null = null;
  private page: Page | null = null;
  private identityDetailUrl: string | null = null;

  private async openIdentityDetail(searchMiddleName: string): Promise<void> {
    if (!this.page) throw new Error('AdminHelper: not logged in. Call login() first.');

    if (!this.identityDetailUrl) {
      await this.page.goto(
        `${env.contextPath}/dashboard?searchFullname=${encodeURIComponent(searchMiddleName)}`
      );
      await this.page.waitForLoadState('networkidle');

      const detailLink = this.page.locator('a[href*="/dashboard/identity/"]').first();
      await detailLink.click();
      await this.page.waitForLoadState('networkidle');

      this.identityDetailUrl = this.page.url();
    } else {
      await this.page.goto(this.identityDetailUrl);
      await this.page.waitForLoadState('networkidle');
    }
  }

  /**
   * Log in as admin in a fresh browser context.
   */
  async login(browser: Browser): Promise<void> {
    // Close any previous context and reset cached state
    await this.context?.close();
    this.identityDetailUrl = null;

    this.context = await browser.newContext();
    this.page = await this.context.newPage();

    await this.page.goto(`${env.contextPath}/user/login`);
    await this.page.fill('input[name="username"]', env.adminUsername);
    await this.page.fill('input[name="password"]', env.adminPassword);
    await this.page.click('button[type="submit"]');
    await this.page.waitForURL(`**${env.contextPath}/dashboard**`);
  }

  /**
   * Navigate to the identity detail page and take a screenshot.
   *
   * On the first call, searches the dashboard by the unique middle name
   * (passed as part of the patron's fullname) to locate the identity,
   * then caches the detail URL. Subsequent calls navigate directly.
   */
  async screenshotIdentityDetail(
    searchMiddleName: string,
    ss: ScreenshotHelper,
    description: string
  ): Promise<void> {
    await this.openIdentityDetail(searchMiddleName);

    await ss.take(this.page!, description, 'admin');
  }

  /**
   * Read Aleph identifiers from the current identity detail page.
   */
  async getIdentityIdentifiers(
    searchMiddleName: string
  ): Promise<{ alephId: string; alephBarcode: string }> {
    await this.openIdentityDetail(searchMiddleName);

    const libraryRows = this.page!.locator('table').nth(1).locator('tbody').first().locator('tr');
    const alephId = (await libraryRows.nth(0).locator('td').textContent())?.trim();
    const alephBarcode = (await libraryRows.nth(1).locator('td').textContent())?.trim();

    if (!alephId || !alephBarcode) {
      throw new Error('AdminHelper: failed to read Aleph identifiers from identity detail page.');
    }

    return { alephId, alephBarcode };
  }

  /**
   * Create a voucher in the admin dashboard and return the generated code.
   */
  async createVoucher(
    ss: ScreenshotHelper,
    options: {
      discountType: 'FIXED_CZK' | 'PERCENTAGE';
      discountValue: number;
      code?: string;
    }
  ): Promise<string> {
    if (!this.page) throw new Error('AdminHelper: not logged in. Call login() first.');

    await this.page.goto(`${env.contextPath}/dashboard/vouchers/new`);
    await this.page.waitForLoadState('networkidle');

    const code = options.code || `E2E-${Date.now()}`;
    await this.page.locator('#create-code').fill(code);
    await this.page.locator('#create-discountType').selectOption(options.discountType);
    await this.page.locator('#create-discountValue').fill(String(options.discountValue));
    await this.page.locator('#create-maxUses').fill('1');
    await ss.take(this.page, 'voucher-creation-form', 'admin');

    await this.page.getByRole('button', { name: 'Vytvořit voucher' }).click();
    await this.page.waitForURL(`**${env.contextPath}/dashboard/vouchers/**`, {
      timeout: 30_000,
      waitUntil: 'domcontentloaded',
    });
    await ss.take(this.page, 'voucher-created', 'admin');
    await this.screenshotVoucherDetail(code, ss, 'voucher-detail');

    return code;
  }

  /**
   * Navigate to a voucher detail page by code and take a screenshot.
   */
  async screenshotVoucherDetail(
    code: string,
    ss: ScreenshotHelper,
    description: string
  ): Promise<void> {
    if (!this.page) throw new Error('AdminHelper: not logged in. Call login() first.');

    await this.page.goto(
      `${env.contextPath}/dashboard/vouchers?search=${encodeURIComponent(code)}`
    );
    await this.page.waitForLoadState('networkidle');

    const detailLink = this.page.locator(`a[href*="/dashboard/vouchers/"]:has-text("${code}")`).first();
    await detailLink.click();
    await this.page.waitForLoadState('networkidle');

    await ss.take(this.page, description, 'admin');
  }

  /**
   * Close the admin browser context and release resources.
   */
  async close(): Promise<void> {
    await this.context?.close();
    this.context = null;
    this.page = null;
    this.identityDetailUrl = null;
  }
}
