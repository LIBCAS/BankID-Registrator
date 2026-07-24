import { test } from '@playwright/test';
import { env } from '../../helpers/env';
import { ScreenshotHelper } from '../../helpers/screenshots';
import { configureToolkit } from '../../helpers/toolkit';
import { randomSuffix } from '../../helpers/random';
import { AdminHelper } from '../../helpers/admin';
import {
  bankIdVerification,
  fillRegistrationForm,
  setPassword,
  comgatePaymentSuccess,
  comgatePaymentCancel,
  comgatePaymentReturnToShop,
  getCurrentPaymentCallbackTransactionId,
  reopenComgateStatusPageFromTransactionLink,
  retryPayment,
  waitFinalPage,
  logout,
} from '../../helpers/flows';

test.describe('Registration - normal customer, no discount - payment failures & re-login', () => {
  const admin = new AdminHelper();

  test.afterAll(async () => {
    await admin.close();
  });

  test('payment cancelled, then logs out', async ({ page, browser }) => {
    const ss = new ScreenshotHelper('registration/no-discount-cancel-logout');
    test.setTimeout(180_000);

    const middleName = env.middleNamePrefix + randomSuffix();

    await configureToolkit(page, { middleName, forceRenewal: false });
    await ss.take(page, 'toolkit-configured');

    await bankIdVerification(page, ss);
    await fillRegistrationForm(page, ss);

    await admin.login(browser);
    await admin.screenshotIdentityDetail(middleName, ss, 'after-patron-creation');

    await setPassword(page, ss);

    await admin.screenshotIdentityDetail(middleName, ss, 'after-password-set');

    // Cancel payment in Comgate
    await comgatePaymentCancel(page, ss);
    await ss.take(page, 'payment-cancelled');
    const cancelledTransactionId = getCurrentPaymentCallbackTransactionId(page);

    await admin.screenshotIdentityDetail(middleName, ss, 'after-payment-cancelled');

    // Log out
    await logout(page, ss);

    await reopenComgateStatusPageFromTransactionLink(page, ss, cancelledTransactionId, {
      expectedLanding: 'public-cancelled',
      screenshotPrefix: 'cancelled-transaction-link-after-logout',
      followLoginAndPayLink: true,
    });
  });

  test('payment returned to shop, then logs out', async ({ page, browser }) => {
    const ss = new ScreenshotHelper('registration/no-discount-return-logout');
    test.setTimeout(180_000);

    const middleName = env.middleNamePrefix + randomSuffix();

    await configureToolkit(page, { middleName, forceRenewal: false });
    await ss.take(page, 'toolkit-configured');

    await bankIdVerification(page, ss);
    await fillRegistrationForm(page, ss);

    await admin.login(browser);
    await admin.screenshotIdentityDetail(middleName, ss, 'after-patron-creation');

    await setPassword(page, ss);

    await admin.screenshotIdentityDetail(middleName, ss, 'after-password-set');

    await comgatePaymentReturnToShop(page, ss);
    await ss.take(page, 'payment-returned-to-shop');
    const pendingTransactionId = getCurrentPaymentCallbackTransactionId(page);

    await logout(page, ss);

    await reopenComgateStatusPageFromTransactionLink(page, ss, pendingTransactionId, {
      expectedLanding: 'welcome',
      screenshotPrefix: 'pending-transaction-link-after-logout',
    });
  });

  test('first payment cancelled, second successful', async ({ page, browser }) => {
    const ss = new ScreenshotHelper('registration/no-discount-retry');
    test.setTimeout(240_000);

    const middleName = env.middleNamePrefix + randomSuffix();

    await configureToolkit(page, { middleName, forceRenewal: false });
    await ss.take(page, 'toolkit-configured');

    await bankIdVerification(page, ss);
    await fillRegistrationForm(page, ss);

    await admin.login(browser);
    await admin.screenshotIdentityDetail(middleName, ss, 'after-patron-creation');

    await setPassword(page, ss);

    await admin.screenshotIdentityDetail(middleName, ss, 'after-password-set');

    // First attempt: cancel payment
    await comgatePaymentCancel(page, ss);
    await ss.take(page, 'payment-cancelled');

    await admin.screenshotIdentityDetail(middleName, ss, 'after-payment-cancelled');

    // Retry: pay again
    await retryPayment(page, ss);
    await comgatePaymentSuccess(page, ss);

    await waitFinalPage(page, ss);

    await admin.screenshotIdentityDetail(middleName, ss, 'after-payment');
  });

  test('logout on password page, re-login and complete', async ({ page, browser }) => {
    const ss = new ScreenshotHelper('registration/no-discount-relogin-password');
    test.setTimeout(240_000);

    const middleName = env.middleNamePrefix + randomSuffix();

    await configureToolkit(page, { middleName, forceRenewal: false });
    await ss.take(page, 'toolkit-configured');

    await bankIdVerification(page, ss);
    await fillRegistrationForm(page, ss);

    await admin.login(browser);
    await admin.screenshotIdentityDetail(middleName, ss, 'after-patron-creation');

    // On password-setting page — log out
    await ss.take(page, 'password-form-before-logout');
    await logout(page, ss);

    // Re-authenticate — should land back on password page
    await configureToolkit(page, { middleName, forceRenewal: false });
    await bankIdVerification(page, ss);

    // Password page (passwordSet == false)
    await setPassword(page, ss);

    await admin.screenshotIdentityDetail(middleName, ss, 'after-password-set');

    await comgatePaymentSuccess(page, ss);

    await waitFinalPage(page, ss);

    await admin.screenshotIdentityDetail(middleName, ss, 'after-payment');
  });

  test('logout on payment page after failed payment, re-login and pay', async ({ page, browser }) => {
    const ss = new ScreenshotHelper('registration/no-discount-relogin-payment');
    test.setTimeout(240_000);

    const middleName = env.middleNamePrefix + randomSuffix();

    await configureToolkit(page, { middleName, forceRenewal: false });
    await ss.take(page, 'toolkit-configured');

    await bankIdVerification(page, ss);
    await fillRegistrationForm(page, ss);

    await admin.login(browser);
    await admin.screenshotIdentityDetail(middleName, ss, 'after-patron-creation');

    await setPassword(page, ss);

    await admin.screenshotIdentityDetail(middleName, ss, 'after-password-set');

    // Cancel payment
    await comgatePaymentCancel(page, ss);
    await ss.take(page, 'payment-cancelled');

    // Log out from the failed payment page
    await logout(page, ss);

    // Re-authenticate — should land on payment page (passwordSet == true)
    await configureToolkit(page, { middleName, forceRenewal: false });
    await bankIdVerification(page, ss);

    // Should be on payment page — pay successfully
    await ss.take(page, 'payment-page-after-relogin');
    await page.getByRole('button', { name: 'Uhradit registrační poplatek' }).click();
    await page.waitForLoadState('networkidle');
    await comgatePaymentSuccess(page, ss);

    await waitFinalPage(page, ss);

    await admin.screenshotIdentityDetail(middleName, ss, 'after-payment');
  });
});
