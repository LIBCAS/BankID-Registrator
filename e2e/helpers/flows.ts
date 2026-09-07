/**
 * Shared, reusable flow step helpers for registration E2E tests.
 *
 * Each function represents one discrete step of the user journey.
 * They accept a `ScreenshotHelper` so every test scenario gets
 * consistent screenshot coverage without duplicating logic.
 */
import { expect, Page, Locator } from '@playwright/test';
import { env } from './env';
import { randomEmail } from './random';
import { ScreenshotHelper } from './screenshots';

// ---------------------------------------------------------------------------
// Bank iD sandbox verification
// ---------------------------------------------------------------------------

/**
 * Complete BankID sandbox identity verification
 */
export async function bankIdVerification(
  page: Page,
  ss: ScreenshotHelper,
  options?: { identityName?: string }
): Promise<void> {
  const identityName = options?.identityName ?? env.bankIdIdentity;

  await page.getByRole('link', { name: 'Ověřit se přes Bank iD' }).click();
  await ss.take(page, 'bankid-bank-selection');

  await page.getByRole('button', { name: 'Logo společnosti Mock banka' }).click();
  await ss.take(page, 'bankid-identity-selection');

  await page.getByRole('link', { name: identityName }).click();
  await page.getByRole('button', { name: 'Přihlásit se (Login)' }).click();
  await ss.take(page, 'bankid-consent');

  await page.getByRole('button', { name: 'Potvrdit přístup' }).click();

  // BankID sandbox shows a "Vyčkejte" loading page before redirecting
  await page.waitForURL(`**${env.contextPath}/**`, { timeout: 30_000 });
  await page.waitForLoadState('networkidle');
}

// ---------------------------------------------------------------------------
// Registration form
// ---------------------------------------------------------------------------

/**
 * Fill and submit the registration form for a normal (non-employee) customer.
 * Checks all required checkboxes including the online-payment consent.
 */
export async function fillRegistrationForm(page: Page, ss: ScreenshotHelper): Promise<void> {
  await ss.take(page, 'registration-form');

  await page.locator('#email').fill(randomEmail('testing.com'));

  await page.getByText('se seznámil/a s knihovním řádem').click();
  await page.getByText('zkontroloval/a správnost a úplnost').click();
  await page.getByText('se seznámil/a se způsobem').click();
  await page.getByText('uhradit registrační poplatek přes platební').click();
  await ss.take(page, 'registration-form-filled');

  await page.getByRole('button', { name: 'Odeslat formulář' }).click();
  await page.waitForLoadState('networkidle');
}

/**
 * Fill and submit the registration form for a CAS employee.
 * Checks the employee checkbox, fills email, uploads document.
 */
export async function fillEmployeeRegistrationForm(page: Page, ss: ScreenshotHelper): Promise<void> {
  await ss.take(page, 'registration-form');

  await page.locator('#isCasEmployee').check();
  await page.locator('#email').fill(randomEmail(env.employeeEmailDomain));

  // Upload employee confirmation document via FilePond
  await page.locator('div.filepond--root input[type="file"]').setInputFiles(env.employeeDocPath);

  await page.getByText('se seznámil/a s knihovním řádem').click();
  await page.getByText('zkontroloval/a správnost a úplnost').click();
  await page.getByText('se seznámil/a se způsobem').click();
  await ss.take(page, 'registration-form-filled');

  await page.getByRole('button', { name: 'Odeslat formulář' }).click();
  await page.waitForLoadState('networkidle');
}

// ---------------------------------------------------------------------------
// Password setting
// ---------------------------------------------------------------------------

export type VoucherPreview = { feeAmount: number; discountAmount: number; amountToPay: number };

/** Validate a voucher and optionally assert the API response and displayed preview. */
export async function validateVoucherPreview(
  page: Page, voucherCode: string, expectedPreview?: VoucherPreview
): Promise<void> {
  await page.locator('#voucherCode').fill(voucherCode);
  const [response] = await Promise.all([
    page.waitForResponse(resp => resp.url().includes('/api/validate-voucher')
      && resp.request().method() === 'POST'),
    page.locator('#btn-validate-voucher').click(),
  ]);
  if (expectedPreview) {
    expect(response.ok()).toBeTruthy();
    const preview = await response.json();
    expect(preview.valid).toBe(true);
    const result = page.locator('#voucher-result');
    await expect(result).toBeVisible();
    await expect(result).toHaveAttribute('data-valid', 'true');
    for (const [field, expected] of Object.entries(expectedPreview)) {
      expect(Number(preview[field]), `Voucher preview ${field}`).toBe(expected);
      const attribute = 'data-' + field.replace(/[A-Z]/g, letter => '-' + letter.toLowerCase());
      await expect.poll(async () => Number(await result.getAttribute(attribute))).toBe(expected);
    }
  }
}

/**
 * Fill and submit the password-setting form (non-employee: "Nastavit heslo a uhradit poplatek").
 * Optionally applies a voucher code before submission.
 */
export async function setPassword(
  page: Page,
  ss: ScreenshotHelper,
  options?: {
    voucherCode?: string;
    expectedVoucherPreview?: VoucherPreview;
  }
): Promise<void> {
  await ss.take(page, 'password-form');

  await page.getByRole('textbox', { name: 'Zvolte heslo' }).fill(env.patronPassword);
  await page.getByRole('textbox', { name: 'Zopakujte zvolené heslo' }).fill(env.patronPassword);

  if (options?.voucherCode) {
    await validateVoucherPreview(page, options.voucherCode, options.expectedVoucherPreview);
    await ss.take(page, 'voucher-validated');
  }

  await ss.take(page, 'password-form-filled');

  // Non-employee button: "Nastavit heslo a uhradit poplatek"
  // Employee button: "Nastavit heslo"
  await page.getByRole('button', { name: /Nastavit heslo/ }).click();
  await page.waitForLoadState('networkidle');
}

// ---------------------------------------------------------------------------
// Comgate payment
// ---------------------------------------------------------------------------

/**
 * Complete the Comgate sandbox payment: select card → pay → confirm.
 */
export async function comgatePaymentSuccess(
  page: Page, ss: ScreenshotHelper, options?: { expectedAmountCzk: number }
): Promise<void> {
  await ss.take(page, 'comgate-payment-methods');

  await page.getByRole('button', { name: 'Card Payment Mastercard, Visa' }).click();
  await ss.take(page, 'comgate-card-selected');

  // The Comgate sandbox fills its fake card data asynchronously.
  // Clicking too early can leave the payment form in a transient state.
  await page.waitForTimeout(10_000);

  const payButton = options
    ? page.getByRole('button', { name: `Pay ${options.expectedAmountCzk} CZK`, exact: true })
    : page.getByRole('button', { name: /Pay \d+ CZK/ });
  await expect(payButton).toBeVisible();

  await confirmComgateSandboxPayment(page, ss, () => payButton.click());
}

/** Handle full-page, embedded, and popup sandbox payment verification. */
export async function confirmComgateSandboxPayment(
  page: Page, ss: ScreenshotHelper, submitPayment: () => Promise<void>
): Promise<void> {
  const verificationPages = new Set<Page>([page]);
  const onPopup = (popup: Page) => verificationPages.add(popup);
  page.on('popup', onPopup);
  let confirmButton: Locator | undefined;
  let continued = false;
  try {
    await submitPayment();
    await ss.take(page, 'comgate-pay-clicked');
    await expect.poll(async () => {
      for (const candidate of verificationPages) {
        if (candidate.isClosed()) continue;
        for (const frame of candidate.frames()) {
          const button = frame.getByRole('button', { name: 'Confirm', exact: true });
          if (await button.isVisible().catch(() => false)) {
            confirmButton = button;
            return true;
          }
        }
      }
      // Some sandbox runs require explicitly opening the verification page.
      const verificationMessage = page.getByText("Can't see the payment verification page?", { exact: false });
      const continueButton = page.getByRole('button', { name: 'Continue', exact: true });
      if (!continued && await verificationMessage.isVisible().catch(() => false)
          && await continueButton.isVisible().catch(() => false)) {
        await continueButton.click();
        continued = true;
      }
      return false;
    }, { timeout: 30_000, message: 'Waiting for Comgate sandbox Confirm control', intervals: [250, 500] }).toBe(true);

    await ss.take(page, 'comgate-provider-testing');
    await Promise.all([
      page.waitForURL(`**${env.contextPath}/**`, { timeout: 60_000, waitUntil: 'commit' }),
      confirmButton!.click(),
    ]);
  } finally {
    page.off('popup', onPopup);
  }
}

/**
 * Cancel the Comgate sandbox payment
 * Returns to the app's payment page with `cancelled` status 
 * e.g. `/bankid-registrator/payment/callback?refId=<_PATRON_BARCODE_>&id=<_COMGATE_TX_ID_>&status=cancelled`
 */
export async function comgatePaymentCancel(page: Page, ss: ScreenshotHelper): Promise<void> {
  await ss.take(page, 'comgate-payment-methods');

  await page.getByRole('button', { name: 'Cancel payment' }).click();
  await ss.take(page, 'comgate-cancel-clicked');

  await page.getByRole('textbox', { name: 'Please enter a reason for' }).click();
  await page.getByRole('textbox', { name: 'Please enter a reason for' }).fill('E2E test cancellation');
  await ss.take(page, 'comgate-cancel-reason-filled');

  await page.getByRole('button', { name: 'Cancel payment' }).click();
  await ss.take(page, 'comgate-cancel-confirmed');

  // Wait for redirect back to our app's payment page
  await page.waitForURL(`**${env.contextPath}/**`, { timeout: 30_000 });
  await page.waitForLoadState('networkidle');
}

/**
 * Return from the Comgate sandbox back to the app without cancelling the payment.
 * This produces the app callback with `status=pending`.
 */
export async function comgatePaymentReturnToShop(page: Page, ss: ScreenshotHelper): Promise<void> {
  await ss.take(page, 'comgate-payment-methods');

  await page.getByRole('button', { name: /Return to e.?shop/i }).click();
  await ss.take(page, 'comgate-return-to-shop-clicked');

  await page.waitForURL(`**${env.contextPath}/payment/callback**status=pending**`, { timeout: 30_000 });
  await page.waitForLoadState('networkidle');
}

export function getCurrentPaymentCallbackTransactionId(page: Page): string {
  const currentUrl = new URL(page.url());
  const transactionId = currentUrl.searchParams.get('id');

  if (!transactionId) {
    throw new Error(`Expected Comgate transaction id in callback URL, got: ${page.url()}`);
  }

  return transactionId;
}

export async function reopenComgateStatusPageFromTransactionLink(
  page: Page,
  ss: ScreenshotHelper,
  transactionId: string,
  options: {
    expectedLanding: 'public-cancelled' | 'welcome';
    screenshotPrefix: string;
    followLoginAndPayLink?: boolean;
  }
): Promise<void> {
  await page.goto(`https://pay1.comgate.cz/status/${transactionId}`);
  await page.waitForLoadState('domcontentloaded');
  await page.waitForTimeout(3_000);
  await ss.take(page, `${options.screenshotPrefix}-status-page-reopened`);

  const returnToShopControl = page
    .locator('a, button')
    .filter({ hasText: /Return to e.?shop/i })
    .first();

  await returnToShopControl.waitFor({ state: 'visible', timeout: 15_000 });
  await returnToShopControl.scrollIntoViewIfNeeded();
  await returnToShopControl.click();

  if (options.expectedLanding === 'public-cancelled') {
    await page.waitForURL(`**${env.contextPath}/payment/callback**status=cancelled**`, { timeout: 30_000 });
    await page.waitForLoadState('networkidle');
    await expect(page.locator('.payment-cancelled-public')).toBeVisible();
    await ss.take(page, `${options.screenshotPrefix}-returned-to-public-cancelled-page`);

    if (options.followLoginAndPayLink) {
      const loginAndPayLink = page
        .locator('a')
        .filter({ hasText: /Přihlásit se a (zaplatit|uhradit poplatek)|Log in and pay/i })
        .first();
      await loginAndPayLink.waitFor({ state: 'visible', timeout: 15_000 });
      const popupPromise = page.waitForEvent('popup', { timeout: 15_000 });
      await loginAndPayLink.click();
      const popupPage = await popupPromise;
      await popupPage.waitForLoadState('domcontentloaded');
      await popupPage.waitForTimeout(3_000);
      await ss.take(popupPage, `${options.screenshotPrefix}-login-and-pay-target-page`);
    }

    return;
  }

  await page.waitForURL(`**${env.contextPath}/welcome**`, { timeout: 30_000 });
  await page.waitForLoadState('networkidle');
  await expect(page.locator('#identity-logout')).toHaveCount(0);
  await ss.take(page, `${options.screenshotPrefix}-returned-to-welcome`);
}

export async function clearInputWithClearButton(page: Page, inputSelector: string): Promise<void> {
  const input = page.locator(inputSelector);
  await input.waitFor({ state: 'visible', timeout: 10_000 });

  const clearButton = page.locator(`.js-clear-input[data-target="${inputSelector}"]`).first();
  if (await clearButton.count() && await clearButton.isVisible()) {
    await clearButton.click();
  } else {
    await input.fill('');
  }

  await expect(input).toHaveValue('');
}

async function submitVoucherOnPaymentPage(
  page: Page
): Promise<'stayed-on-payment-page' | 'redirected-to-success'> {
  await Promise.all([
    page.waitForResponse(
      resp => resp.url().includes('/payment/apply-voucher') && resp.request().method() === 'POST',
      { timeout: 30_000 }
    ),
    page.locator('form[action$="/payment/apply-voucher"] button[type="submit"]').click(),
  ]);

  const landingState = await Promise.race([
    page
      .waitForURL(`**${env.contextPath}/payment/callback**status=success**`, {
        timeout: 30_000,
        waitUntil: 'commit',
      })
      .then(() => 'redirected-to-success' as const),
    page
      .waitForURL(new RegExp(`${env.contextPath.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')}/payment(?:$|[?#])`), {
        timeout: 30_000,
        waitUntil: 'commit',
      })
      .then(() => 'stayed-on-payment-page' as const),
  ]);

  await page.waitForLoadState('domcontentloaded');
  return landingState;
}

export async function exerciseVoucherRecalculationOnPaymentPage(
  page: Page,
  ss: ScreenshotHelper,
  sequence: {
    invalidVoucher: string;
    partialVoucher1: string;
    partialVoucher2: string;
  }
): Promise<void> {
  await ss.take(page, 'payment-page-before-voucher-recalculation-sequence');

  const attempts = [
    { code: sequence.invalidVoucher, screenshot: 'payment-page-invalid-voucher-result' },
    { code: sequence.partialVoucher1, screenshot: 'payment-page-partial-voucher-1-result' },
    { code: sequence.partialVoucher2, screenshot: 'payment-page-partial-voucher-2-result' },
  ];

  for (let index = 0; index < attempts.length; index += 1) {
    const attempt = attempts[index];

    if (index > 0) {
      await clearInputWithClearButton(page, '#voucherCodeInput');
    }

    await page.locator('#voucherCodeInput').fill(attempt.code);
    const landingState = await submitVoucherOnPaymentPage(page);

    if (landingState !== 'stayed-on-payment-page') {
      throw new Error(`Expected payment page voucher attempt to stay on /payment for code ${attempt.code}.`);
    }

    await ss.take(page, attempt.screenshot);
  }
}

/**
 * Apply a voucher on the payment page.
 * For partial discounts, the page stays on /payment.
 * For 100% discounts, the page first shows the applied voucher and a Continue button.
 * The helper clicks Continue to re-enter LIBCAS Payments Gateway API zero-payment finalization.
 */
export async function applyVoucherOnPaymentPage(
  page: Page,
  ss: ScreenshotHelper,
  voucherCode: string
): Promise<'stayed-on-payment-page' | 'redirected-to-success'> {
  await ss.take(page, 'payment-page-before-voucher-apply');

  await page.locator('#voucherCodeInput').fill(voucherCode);
  await ss.take(page, 'payment-page-voucher-filled');

  const landingState = await submitVoucherOnPaymentPage(page);

  if (landingState === 'stayed-on-payment-page') {
    await ss.take(page, 'payment-page-voucher-applied');

    const continueButton = page.locator('#payment-initiate-button', {
      hasText: /Pokračovat|Continue|Pokračovať/,
    });

    if (await continueButton.count() && await continueButton.isVisible()) {
      await ss.take(page, 'payment-page-before-continue');
      await continueButton.click();
      await page.waitForURL(`**${env.contextPath}/payment/callback**status=success**`, {
        timeout: 60_000,
        waitUntil: 'commit',
      });
      await page.waitForLoadState('domcontentloaded');
      await ss.take(page, 'payment-page-after-continue-success-callback');
      return 'redirected-to-success';
    }
  }

  return landingState;
}

/**
 * On the app's payment page, click "Pay again" to re-enter Comgate.
 */
export async function retryPayment(page: Page, ss: ScreenshotHelper): Promise<void> {
  await ss.take(page, 'payment-cancelled-or-returned');
  await page.locator('#payment-initiate-button').click();
  await page.waitForLoadState('networkidle');
  // The auto-submitting redirect page sends us to Comgate
}

// ---------------------------------------------------------------------------
// Final page + LDAP sync wait
// ---------------------------------------------------------------------------

/**
 * Wait on the final success page with LDAP sync progress loader.
 * Takes screenshots at start, midpoint, and end.
 */
export async function waitFinalPage(page: Page, ss: ScreenshotHelper): Promise<void> {
  await ss.take(page, 'final-page-start');

  await page.waitForTimeout(env.ldapSyncTimeoutMs / 2);
  await ss.take(page, 'final-page-mid');

  await page.waitForTimeout(env.ldapSyncTimeoutMs / 2 + 10_000);
  await ss.take(page, 'final-page-end');
}

// ---------------------------------------------------------------------------
// Logout
// ---------------------------------------------------------------------------

/**
 * Click the identity logout button in the header.
 * Waits for redirect to the welcome page.
 */
export async function logout(page: Page, ss: ScreenshotHelper): Promise<void> {
  await ss.take(page, 'before-logout');
  await page.locator('#identity-logout').click();
  await page.waitForURL(`**${env.contextPath}/welcome**`, { timeout: 15_000 });
  await page.waitForLoadState('networkidle');
  await ss.take(page, 'after-logout');
}

/**
 * Navigate to the welcome page after a scenario completes and capture whether
 * the UI is in a logged-out state.
 */
export async function captureWelcomeLoggedOutState(page: Page, ss: ScreenshotHelper): Promise<void> {
  await page.goto(`${env.contextPath}/welcome`);
  await page.waitForLoadState('networkidle');

  await expect(page.locator('#identity-logout')).toHaveCount(0);
  await ss.take(page, 'welcome-logged-out-state');
}
