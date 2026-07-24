import { test } from '@playwright/test';
import { env } from '../../helpers/env';
import { ScreenshotHelper } from '../../helpers/screenshots';
import { configureToolkit } from '../../helpers/toolkit';
import { randomSuffix } from '../../helpers/random';
import { AdminHelper } from '../../helpers/admin';
import { resolveVoucherRecalculationSequence } from '../../helpers/vouchers';
import {
  bankIdVerification,
  fillRegistrationForm,
  setPassword,
  comgatePaymentReturnToShop,
  applyVoucherOnPaymentPage,
  exerciseVoucherRecalculationOnPaymentPage,
  waitFinalPage,
  logout,
} from '../../helpers/flows';

test.describe('Registration - normal customer, 100% discount', () => {
  const admin = new AdminHelper();

  test.afterAll(async () => {
    await admin.close();
  });

  test('full flow until final page (no payment)', async ({ page, browser }) => {
    const ss = new ScreenshotHelper('registration/full-discount');
    test.setTimeout(180_000);

    const middleName = env.middleNamePrefix + randomSuffix();

    // Admin: create a 100% discount voucher
    await admin.login(browser);
    const voucherCode = await admin.createVoucher(ss, {
      discountType: 'PERCENTAGE',
      discountValue: 100,
    });

    await configureToolkit(page, { middleName, forceRenewal: false });
    await ss.take(page, 'toolkit-configured');

    await bankIdVerification(page, ss);
    await fillRegistrationForm(page, ss);

    await admin.screenshotIdentityDetail(middleName, ss, 'after-patron-creation');

    // Password-setting form — apply the 100% voucher (skips Comgate entirely)
    await setPassword(page, ss, { voucherCode });

    await admin.screenshotIdentityDetail(middleName, ss, 'after-password-set');

    // With 100% voucher, app redirects directly to success page (no Comgate)
    await waitFinalPage(page, ss);

    await admin.screenshotIdentityDetail(middleName, ss, 'after-completion');
    await admin.screenshotVoucherDetail(voucherCode, ss, 'voucher-detail-after-completion');
  });

  test('logout on password page, re-login and complete', async ({ page, browser }) => {
    const ss = new ScreenshotHelper('registration/full-discount-relogin-password');
    test.setTimeout(240_000);

    const middleName = env.middleNamePrefix + randomSuffix();

    await admin.login(browser);
    const voucherCode = await admin.createVoucher(ss, {
      discountType: 'PERCENTAGE',
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

    await waitFinalPage(page, ss);

    await admin.screenshotIdentityDetail(middleName, ss, 'after-completion');
    await admin.screenshotVoucherDetail(voucherCode, ss, 'voucher-detail-after-completion');
  });

  test('returns from Comgate, applies 100% voucher on payment page, then completes without paying', async ({ page, browser }) => {
    const ss = new ScreenshotHelper('registration/full-discount-return-apply-complete');
    test.setTimeout(240_000);

    const middleName = env.middleNamePrefix + randomSuffix();

    await admin.login(browser);
    const voucherCode = await admin.createVoucher(ss, {
      discountType: 'PERCENTAGE',
      discountValue: 100,
    });
    const voucherSequence = await resolveVoucherRecalculationSequence(admin, ss);

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

    await exerciseVoucherRecalculationOnPaymentPage(page, ss, voucherSequence);

    // 100% voucher shows the applied discount first, then continues through zero-payment finalization.
    await applyVoucherOnPaymentPage(page, ss, voucherCode);

    await waitFinalPage(page, ss);

    await admin.screenshotIdentityDetail(middleName, ss, 'after-completion');
    await admin.screenshotVoucherDetail(voucherCode, ss, 'voucher-detail-after-completion');
  });
});
