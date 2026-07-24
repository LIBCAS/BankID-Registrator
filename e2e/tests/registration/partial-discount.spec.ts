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
  applyVoucherOnPaymentPage,
  retryPayment,
  waitFinalPage,
  logout,
} from '../../helpers/flows';

test.describe('Registration - normal customer, partial discount', () => {
  const admin = new AdminHelper();

  test.afterAll(async () => {
    await admin.close();
  });

  test('full flow with successful payment', async ({ page, browser }) => {
    const ss = new ScreenshotHelper('registration/partial-discount');
    test.setTimeout(180_000);

    const middleName = env.middleNamePrefix + randomSuffix();

    // Admin: create a partial discount voucher before the user flow
    await admin.login(browser);
    const voucherCode = await admin.createVoucher(ss, {
      discountType: 'PERCENTAGE',
      discountValue: 70,
    });

    await configureToolkit(page, { middleName, forceRenewal: false });
    await ss.take(page, 'toolkit-configured');

    await bankIdVerification(page, ss);
    await fillRegistrationForm(page, ss);

    await admin.screenshotIdentityDetail(middleName, ss, 'after-patron-creation');

    // Password-setting form — apply the partial voucher
    await setPassword(page, ss, { voucherCode });

    await admin.screenshotIdentityDetail(middleName, ss, 'after-password-set');

    // Comgate payment — discounted amount
    await comgatePaymentSuccess(page, ss);

    await waitFinalPage(page, ss);

    await admin.screenshotIdentityDetail(middleName, ss, 'after-payment');
    await admin.screenshotVoucherDetail(voucherCode, ss, 'voucher-detail-after-payment');
  });

  test('payment cancelled, then logs out', async ({ page, browser }) => {
    const ss = new ScreenshotHelper('registration/partial-discount-cancel-logout');
    test.setTimeout(180_000);

    const middleName = env.middleNamePrefix + randomSuffix();

    await admin.login(browser);
    const voucherCode = await admin.createVoucher(ss, {
      discountType: 'FIXED_CZK',
      discountValue: 100,
    });

    await configureToolkit(page, { middleName, forceRenewal: false });
    await ss.take(page, 'toolkit-configured');

    await bankIdVerification(page, ss);
    await fillRegistrationForm(page, ss);

    await admin.screenshotIdentityDetail(middleName, ss, 'after-patron-creation');

    await setPassword(page, ss, { voucherCode });

    await admin.screenshotIdentityDetail(middleName, ss, 'after-password-set');

    // Cancel payment in Comgate
    await comgatePaymentCancel(page, ss);
    await ss.take(page, 'payment-cancelled');

    await admin.screenshotIdentityDetail(middleName, ss, 'after-payment-cancelled');

    // Log out
    await logout(page, ss);
    await admin.screenshotVoucherDetail(voucherCode, ss, 'voucher-detail-after-cancelled-logout');
  });

  test('first payment cancelled, second successful', async ({ page, browser }) => {
    const ss = new ScreenshotHelper('registration/partial-discount-retry');
    test.setTimeout(240_000);

    const middleName = env.middleNamePrefix + randomSuffix();

    await admin.login(browser);
    const voucherCode = await admin.createVoucher(ss, {
      discountType: 'FIXED_CZK',
      discountValue: 100,
    });

    await configureToolkit(page, { middleName, forceRenewal: false });
    await ss.take(page, 'toolkit-configured');

    await bankIdVerification(page, ss);
    await fillRegistrationForm(page, ss);

    await admin.screenshotIdentityDetail(middleName, ss, 'after-patron-creation');

    await setPassword(page, ss, { voucherCode });

    await admin.screenshotIdentityDetail(middleName, ss, 'after-password-set');

    // First attempt: cancel payment
    await comgatePaymentCancel(page, ss);
    await ss.take(page, 'payment-cancelled');

    await admin.screenshotIdentityDetail(middleName, ss, 'after-payment-cancelled');

    // Retry: pay again from the app's payment page
    await retryPayment(page, ss);
    await comgatePaymentSuccess(page, ss);

    await waitFinalPage(page, ss);

    await admin.screenshotIdentityDetail(middleName, ss, 'after-payment');
    await admin.screenshotVoucherDetail(voucherCode, ss, 'voucher-detail-after-payment');
  });

  test('logout on password page, re-login and complete', async ({ page, browser }) => {
    const ss = new ScreenshotHelper('registration/partial-discount-relogin-password');
    test.setTimeout(240_000);

    const middleName = env.middleNamePrefix + randomSuffix();

    await admin.login(browser);
    const voucherCode = await admin.createVoucher(ss, {
      discountType: 'FIXED_CZK',
      discountValue: 100,
    });

    await configureToolkit(page, { middleName, forceRenewal: false });
    await ss.take(page, 'toolkit-configured');

    await bankIdVerification(page, ss);
    await fillRegistrationForm(page, ss);

    await admin.screenshotIdentityDetail(middleName, ss, 'after-patron-creation');

    // On password-setting page — log out
    await ss.take(page, 'password-form-before-logout');
    await logout(page, ss);

    // Re-authenticate — should land back on password page
    await configureToolkit(page, { middleName, forceRenewal: false });
    await bankIdVerification(page, ss);

    await setPassword(page, ss, { voucherCode });

    await admin.screenshotIdentityDetail(middleName, ss, 'after-password-set');

    await comgatePaymentSuccess(page, ss);

    await waitFinalPage(page, ss);

    await admin.screenshotIdentityDetail(middleName, ss, 'after-payment');
    await admin.screenshotVoucherDetail(voucherCode, ss, 'voucher-detail-after-payment');
  });

  test('logout on payment page after failed payment, re-login and pay', async ({ page, browser }) => {
    const ss = new ScreenshotHelper('registration/partial-discount-relogin-payment');
    test.setTimeout(240_000);

    const middleName = env.middleNamePrefix + randomSuffix();

    await admin.login(browser);
    const voucherCode = await admin.createVoucher(ss, {
      discountType: 'FIXED_CZK',
      discountValue: 100,
    });

    await configureToolkit(page, { middleName, forceRenewal: false });
    await ss.take(page, 'toolkit-configured');

    await bankIdVerification(page, ss);
    await fillRegistrationForm(page, ss);

    await admin.screenshotIdentityDetail(middleName, ss, 'after-patron-creation');

    await setPassword(page, ss, { voucherCode });

    await admin.screenshotIdentityDetail(middleName, ss, 'after-password-set');

    // Cancel payment
    await comgatePaymentCancel(page, ss);
    await ss.take(page, 'payment-cancelled');

    // Log out from the failed payment page
    await logout(page, ss);

    // Re-authenticate — should land on payment page (passwordSet == true)
    await configureToolkit(page, { middleName, forceRenewal: false });
    await bankIdVerification(page, ss);

    // Should be on payment page now — pay successfully
    await ss.take(page, 'payment-page-after-relogin');
    await page.getByRole('button', { name: 'Uhradit registrační poplatek' }).click();
    await page.waitForLoadState('networkidle');
    await comgatePaymentSuccess(page, ss);

    await waitFinalPage(page, ss);

    await admin.screenshotIdentityDetail(middleName, ss, 'after-payment');
    await admin.screenshotVoucherDetail(voucherCode, ss, 'voucher-detail-after-payment');
  });

  test('returns from Comgate, applies partial voucher on payment page, then pays', async ({ page, browser }) => {
    const ss = new ScreenshotHelper('registration/partial-discount-return-apply-pay');
    test.setTimeout(240_000);

    const middleName = env.middleNamePrefix + randomSuffix();

    await admin.login(browser);
    const voucherCode = await admin.createVoucher(ss, {
      discountType: 'FIXED_CZK',
      discountValue: 100,
    });

    await configureToolkit(page, { middleName, forceRenewal: false });
    await ss.take(page, 'toolkit-configured');

    await bankIdVerification(page, ss);
    await fillRegistrationForm(page, ss);

    await admin.screenshotIdentityDetail(middleName, ss, 'after-patron-creation');

    // Do not apply the voucher on the password page.
    await setPassword(page, ss);

    await admin.screenshotIdentityDetail(middleName, ss, 'after-password-set');

    // Return from Comgate with pending status, then apply the voucher on the payment page.
    await comgatePaymentReturnToShop(page, ss);
    await ss.take(page, 'payment-page-pending-return');
    await applyVoucherOnPaymentPage(page, ss, voucherCode);

    await admin.screenshotIdentityDetail(middleName, ss, 'after-voucher-applied');

    await retryPayment(page, ss);
    await comgatePaymentSuccess(page, ss);

    await waitFinalPage(page, ss);

    await admin.screenshotIdentityDetail(middleName, ss, 'after-payment');
    await admin.screenshotVoucherDetail(voucherCode, ss, 'voucher-detail-after-payment');
  });

  test('returns from Comgate, applies partial voucher, logs out, re-logins and pays', async ({ page, browser }) => {
    const ss = new ScreenshotHelper('registration/partial-discount-return-apply-logout-relogin-pay');
    test.setTimeout(300_000);

    const middleName = env.middleNamePrefix + randomSuffix();

    await admin.login(browser);
    const voucherCode = await admin.createVoucher(ss, {
      discountType: 'FIXED_CZK',
      discountValue: 100,
    });

    await configureToolkit(page, { middleName, forceRenewal: false });
    await ss.take(page, 'toolkit-configured');

    await bankIdVerification(page, ss);
    await fillRegistrationForm(page, ss);

    await admin.screenshotIdentityDetail(middleName, ss, 'after-patron-creation');

    // Do not apply the voucher on the password page.
    await setPassword(page, ss);

    await admin.screenshotIdentityDetail(middleName, ss, 'after-password-set');

    await comgatePaymentReturnToShop(page, ss);
    await ss.take(page, 'payment-page-pending-return');
    await applyVoucherOnPaymentPage(page, ss, voucherCode);

    await admin.screenshotIdentityDetail(middleName, ss, 'after-voucher-applied');

    await logout(page, ss);

    // Re-authenticate — should land on the payment page with the voucher already applied.
    await configureToolkit(page, { middleName, forceRenewal: false });
    await bankIdVerification(page, ss);

    await ss.take(page, 'payment-page-after-relogin-with-voucher');
    await retryPayment(page, ss);
    await comgatePaymentSuccess(page, ss);

    await waitFinalPage(page, ss);

    await admin.screenshotIdentityDetail(middleName, ss, 'after-payment');
    await admin.screenshotVoucherDetail(voucherCode, ss, 'voucher-detail-after-payment');
  });
});
