import { Browser, Locator, Page } from '@playwright/test';
import { AdminHelper } from './admin';
import { env, isRenewalFinesScenarioConfigured, RenewalFinesScenarioConfig, RenewalFinesScenarioKey } from './env';
import {
  bankIdVerification,
  validateVoucherPreview,
  VoucherPreview,
  comgatePaymentSuccess,
  fillEmployeeRegistrationForm,
  fillRegistrationForm,
  setPassword,
  waitFinalPage,
} from './flows';
import { randomEmail } from './random';
import { ScreenshotHelper } from './screenshots';
import { configureToolkit } from './toolkit';
import { clearInputWithClearButton } from './flows';

export type RegisteredPatronInfo = {
  middleName: string;
  bankIdIdentity: string;
  alephId: string;
  alephBarcode: string;
  isEmployee: boolean;
};

type MainCssNetworkEvent = {
  event: 'request' | 'response' | 'requestfailed';
  url: string;
  pageUrl: string;
  method: string;
  resourceType: string;
  status?: number;
  failureText?: string;
  redirectedFrom?: string;
  timestamp: number;
};

const MAIN_CSS_PATH = '/assets/dist/css/main.css';
const mainCssNetworkTraces = new WeakMap<Page, MainCssNetworkEvent[]>();
const mainCssTraceAttachedPages = new WeakSet<Page>();

export async function completeRegistrationForRenewalSeed(
  page: Page,
  browser: Browser,
  admin: AdminHelper,
  ss: ScreenshotHelper,
  options: {
    middleName: string;
    bankIdIdentity?: string;
    isEmployee?: boolean;
    expectedRegistrationFeeCzk?: number;
  }
): Promise<RegisteredPatronInfo> {
  const bankIdIdentity = options.bankIdIdentity ?? env.bankIdIdentity;
  const isEmployee = options.isEmployee ?? false;

  await configureToolkit(page, { middleName: options.middleName, forceRenewal: false });
  await ss.take(page, 'registration-toolkit-configured');

  await bankIdVerification(page, ss, { identityName: bankIdIdentity });

  if (isEmployee) {
    await fillEmployeeRegistrationForm(page, ss);
  } else {
    await fillRegistrationForm(page, ss);
  }

  await admin.login(browser);
  await admin.screenshotIdentityDetail(options.middleName, ss, 'registration-after-patron-creation');

  await setPassword(page, ss);
  await admin.screenshotIdentityDetail(options.middleName, ss, 'registration-after-password-set');

  if (isEmployee) {
    await waitFinalPage(page, ss);
    await admin.screenshotIdentityDetail(options.middleName, ss, 'registration-after-completion');
  } else {
    await comgatePaymentSuccess(page, ss, options.expectedRegistrationFeeCzk === undefined
      ? undefined : { expectedAmountCzk: options.expectedRegistrationFeeCzk });
    await waitFinalPage(page, ss);
    await admin.screenshotIdentityDetail(options.middleName, ss, 'registration-after-payment');
  }

  const identifiers = await admin.getIdentityIdentifiers(options.middleName);

  return {
    middleName: options.middleName,
    bankIdIdentity,
    alephId: identifiers.alephId,
    alephBarcode: identifiers.alephBarcode,
    isEmployee,
  };
}

export async function startForcedRenewalJourney(
  page: Page,
  ss: ScreenshotHelper,
  options: {
    middleName: string;
    bankIdIdentity?: string;
  }
): Promise<void> {
  await configureToolkit(page, { middleName: options.middleName, forceRenewal: true });
  await ss.take(page, 'renewal-toolkit-configured');

  await bankIdVerification(page, ss, {
    identityName: options.bankIdIdentity ?? env.bankIdIdentity,
  });
}

function ensureCheckboxState(locator: Locator, checked: boolean): Promise<void> {
  return locator.evaluate((element, desired) => {
    const input = element as HTMLInputElement;
    if (input.checked !== desired) {
      input.click();
    }
  }, checked);
}

export async function fillRenewalForm(
  page: Page,
  ss: ScreenshotHelper,
  options?: {
    isEmployee?: boolean;
    voucherCode?: string;
    expectedVoucherPreview?: VoucherPreview;
    voucherSequence?: {
      invalidVoucher: string;
      partialVoucher1: string;
      partialVoucher2: string;
      finalVoucher: string;
    };
    submitAction?: 'submit' | 'submitAndPay';
    admin?: {
      helper: AdminHelper;
      middleName: string;
      screenshotDescription?: string;
    };
  }
): Promise<void> {
  resetMainCssNetworkTrace(page);
  ensureMainCssNetworkTrace(page);

  const isEmployee = options?.isEmployee ?? false;

  await waitForRenewalAlephPrefill(page);

  await ss.take(page, 'renewal-form');

  await page.locator('#email').fill(
    isEmployee ? randomEmail(env.employeeEmailDomain) : randomEmail('testing.com')
  );

  if (isEmployee) {
    await page.locator('#isCasEmployee').check();
    await page.locator('div.filepond--root input[type="file"]').setInputFiles(env.employeeDocPath);
    await page.locator('.filepond--item').first().waitFor({ state: 'visible', timeout: 10_000 });
  }

  await ensureCheckboxState(page.locator('#declaration1'), true);
  await ensureCheckboxState(page.locator('#declaration2'), true);
  await ensureCheckboxState(page.locator('#declaration3'), true);

  if (!isEmployee && await page.locator('#declaration4').count()) {
    await ensureCheckboxState(page.locator('#declaration4'), true);
  }

  if (options?.voucherSequence) {
    await exerciseRenewalVoucherSequence(page, ss, options.voucherSequence);
  } else if (options?.voucherCode) {
    await validateVoucherPreview(page, options.voucherCode, options.expectedVoucherPreview);
    await ss.take(page, 'renewal-form-voucher-validated');
  }

  await ss.take(page, 'renewal-form-filled');

  const submitButton = page.locator('#renewal-submit-button');
  const submitAndPayButton = page.locator('#renewal-submit-and-pay-button');
  let targetButton = submitButton;

  if (options?.submitAction === 'submitAndPay') {
    targetButton = submitAndPayButton;
  } else if (options?.submitAction === 'submit') {
    targetButton = submitButton;
  } else if (await submitAndPayButton.isVisible()) {
    targetButton = submitAndPayButton;
  }

  await Promise.all([
    page.waitForResponse(
      resp => resp.url().includes('/membership-renewal') && resp.request().method() === 'POST'
    ),
    targetButton.click(),
  ]);
  await page.waitForLoadState('load');

  if (options?.admin) {
    await options.admin.helper.screenshotIdentityDetail(
      options.admin.middleName,
      ss,
      options.admin.screenshotDescription ?? 'renewal-after-form-submission'
    );
  }
}

async function exerciseRenewalVoucherSequence(
  page: Page,
  ss: ScreenshotHelper,
  sequence: {
    invalidVoucher: string;
    partialVoucher1: string;
    partialVoucher2: string;
    finalVoucher: string;
  }
): Promise<void> {
  const attempts = [
    { code: sequence.invalidVoucher, screenshot: 'renewal-form-invalid-voucher-result' },
    { code: sequence.partialVoucher1, screenshot: 'renewal-form-partial-voucher-1-result' },
    { code: sequence.partialVoucher2, screenshot: 'renewal-form-partial-voucher-2-result' },
    { code: sequence.finalVoucher, screenshot: 'renewal-form-full-voucher-result' },
  ];

  for (let index = 0; index < attempts.length; index += 1) {
    const attempt = attempts[index];

    if (index > 0) {
      await clearInputWithClearButton(page, '#voucherCode');
    }

    await page.locator('#voucherCode').fill(attempt.code);
    await page.locator('#btn-validate-voucher').click();
    await page.waitForResponse(resp => resp.url().includes('/api/validate-voucher'));
    await ss.take(page, attempt.screenshot);
  }
}

async function waitForRenewalAlephPrefill(page: Page): Promise<void> {
  const alephPrefillButtons = page.locator('.js-renewal-prefill-aleph');
  const prefillCount = await alephPrefillButtons.count();

  if (prefillCount === 0) {
    return;
  }

  for (let i = 0; i < prefillCount; i += 1) {
    const targetSelector = await alephPrefillButtons.nth(i).getAttribute('data-input-target');
    if (!targetSelector) {
      continue;
    }

    await page.locator(targetSelector).waitFor({ state: 'attached', timeout: 10_000 });
    await page.waitForFunction(
      (selector: string) => {
        const element = document.querySelector(selector) as HTMLInputElement | null;
        return !!element && element.value.trim().length > 0;
      },
      targetSelector,
      { timeout: 10_000 }
    );
  }
}

export async function captureRenewalCompletion(page: Page, ss: ScreenshotHelper): Promise<void> {
  await Promise.race([
    page.locator('body.page-membership-renewal-success').waitFor({ state: 'visible', timeout: 30_000 }),
    page.locator('.payment-success-renewal').waitFor({ state: 'visible', timeout: 30_000 }),
  ]);
  await waitForMainCssReady(page);
  await ss.take(page, 'renewal-completion');
}

export async function captureRenewalCompletionFromPaymentCallbackSuccess(
  page: Page,
  ss: ScreenshotHelper
): Promise<void> {
  await page.waitForURL(`**${env.contextPath}/payment/callback**status=success**`, {
    timeout: 30_000,
    waitUntil: 'commit',
  });
  await page.locator('.payment-success-renewal').waitFor({ state: 'visible', timeout: 30_000 });
  await waitForMainCssReady(page);
  await ss.takeRaw(page, 'raw-renewal-completion');
}

async function waitForMainCssReady(page: Page): Promise<void> {
  try {
    await page.waitForFunction(() => {
      const mainCssPath = '/assets/dist/css/main.css';
      const hasMainCssLink = Array.from(document.querySelectorAll<HTMLLinkElement>('link[rel="stylesheet"]'))
        .some((link) => (link.href || '').includes(mainCssPath));

      if (!hasMainCssLink) {
        return false;
      }

      const mainCssSheet = Array.from(document.styleSheets)
        .find((sheet) => (sheet.href || '').includes(mainCssPath));

      if (!mainCssSheet) {
        return false;
      }

      let isMainCssReadable = false;
      try {
        void mainCssSheet.cssRules.length;
        isMainCssReadable = true;
      } catch {
        return false;
      }

      const resourceEntries = performance.getEntriesByType('resource')
        .filter((entry) => entry.name.includes(mainCssPath));
      const latestResourceEntry = resourceEntries.length > 0
        ? resourceEntries[resourceEntries.length - 1] as (PerformanceResourceTiming & { responseStatus?: number })
        : undefined;
      const responseStatus = latestResourceEntry?.responseStatus;

      const logo = document.querySelector<HTMLElement>('header nav img');
      const main = document.querySelector<HTMLElement>('body > main');
      const footer = document.querySelector<HTMLElement>('body > footer');

      if (!logo || !main || !footer) {
        return false;
      }

      const logoHeight = Math.round(parseFloat(getComputedStyle(logo).height));
      const mainMarginTop = parseFloat(getComputedStyle(main).marginTop);
      const bodyDisplay = getComputedStyle(document.body).display;
      const footerFlexShrink = getComputedStyle(footer).flexShrink;
      const hasSuccessfulMainCssResponse = responseStatus === undefined || responseStatus === 200;

      return isMainCssReadable
        && hasSuccessfulMainCssResponse
        && logoHeight === 42
        && mainMarginTop >= 80
        && bodyDisplay === 'flex'
        && footerFlexShrink === '0';
    }, undefined, { timeout: 30_000 });
  } catch {
    const networkTrace = getMainCssNetworkTrace(page);
    const diagnostics = await page.evaluate(() => {
      const mainCssPath = '/assets/dist/css/main.css';
      const links = Array.from(document.querySelectorAll<HTMLLinkElement>('link[rel="stylesheet"]'));
      const mainCssLinks = links
        .map((link) => link.href || '')
        .filter((href) => href.includes(mainCssPath));

      const styleSheets = Array.from(document.styleSheets);
      const mainCssSheet = styleSheets.find((sheet) => (sheet.href || '').includes(mainCssPath));

      let isMainCssReadable = false;
      try {
        if (mainCssSheet) {
          void mainCssSheet.cssRules.length;
          isMainCssReadable = true;
        }
      } catch {
        isMainCssReadable = false;
      }

      const resourceEntries = performance.getEntriesByType('resource')
        .filter((entry) => entry.name.includes(mainCssPath))
        .map((entry) => {
          const resourceEntry = entry as PerformanceResourceTiming & { responseStatus?: number };
          return {
            name: resourceEntry.name,
            initiatorType: resourceEntry.initiatorType,
            responseStatus: resourceEntry.responseStatus ?? null,
            transferSize: resourceEntry.transferSize,
            duration: resourceEntry.duration,
          };
        });

      const logo = document.querySelector<HTMLElement>('header nav img');
      const main = document.querySelector<HTMLElement>('body > main');
      const footer = document.querySelector<HTMLElement>('body > footer');

      return {
        pageUrl: window.location.href,
        mainCssLinks,
        hasMainCssSheet: Boolean(mainCssSheet),
        isMainCssReadable,
        resourceEntries,
        logoHeight: logo ? getComputedStyle(logo).height : null,
        mainMarginTop: main ? getComputedStyle(main).marginTop : null,
        bodyDisplay: getComputedStyle(document.body).display,
        footerFlexShrink: footer ? getComputedStyle(footer).flexShrink : null,
      };
    });

    throw new Error(`main.css was not ready on renewal success page: ${JSON.stringify({
      ...diagnostics,
      networkTrace,
    })}`);
  }
}

function ensureMainCssNetworkTrace(page: Page): void {
  if (mainCssTraceAttachedPages.has(page)) {
    return;
  }

  mainCssTraceAttachedPages.add(page);
  mainCssNetworkTraces.set(page, []);

  page.on('request', (request) => {
    if (!isMainCssRequest(request.url())) {
      return;
    }

    recordMainCssNetworkEvent(page, {
      event: 'request',
      url: request.url(),
      pageUrl: page.url(),
      method: request.method(),
      resourceType: request.resourceType(),
      redirectedFrom: request.redirectedFrom()?.url(),
      timestamp: Date.now(),
    });
  });

  page.on('response', (response) => {
    if (!isMainCssRequest(response.url())) {
      return;
    }

    const request = response.request();
    recordMainCssNetworkEvent(page, {
      event: 'response',
      url: response.url(),
      pageUrl: page.url(),
      method: request.method(),
      resourceType: request.resourceType(),
      status: response.status(),
      redirectedFrom: request.redirectedFrom()?.url(),
      timestamp: Date.now(),
    });
  });

  page.on('requestfailed', (request) => {
    if (!isMainCssRequest(request.url())) {
      return;
    }

    recordMainCssNetworkEvent(page, {
      event: 'requestfailed',
      url: request.url(),
      pageUrl: page.url(),
      method: request.method(),
      resourceType: request.resourceType(),
      failureText: request.failure()?.errorText,
      redirectedFrom: request.redirectedFrom()?.url(),
      timestamp: Date.now(),
    });
  });
}

function resetMainCssNetworkTrace(page: Page): void {
  mainCssNetworkTraces.set(page, []);
}

function recordMainCssNetworkEvent(page: Page, event: MainCssNetworkEvent): void {
  const existingTrace = mainCssNetworkTraces.get(page) ?? [];
  existingTrace.push(event);
  mainCssNetworkTraces.set(page, existingTrace);
}

function getMainCssNetworkTrace(page: Page): MainCssNetworkEvent[] {
  const trace = mainCssNetworkTraces.get(page) ?? [];
  return trace.slice(-20);
}

function isMainCssRequest(url: string): boolean {
  return url.includes(MAIN_CSS_PATH);
}

export function getConfiguredRenewalFinesScenario(
  key: RenewalFinesScenarioKey
): RenewalFinesScenarioConfig | null {
  return isRenewalFinesScenarioConfigured(key) ? env.renewalFinesScenarios[key] : null;
}
