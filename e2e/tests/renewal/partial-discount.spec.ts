import { test } from '@playwright/test';
import { AdminHelper } from '../../helpers/admin';
import {
  applyVoucherOnPaymentPage,
  comgatePaymentReturnToShop,
  comgatePaymentSuccess,
  logout,
  retryPayment,
} from '../../helpers/flows';
import { randomSuffix } from '../../helpers/random';
import {
  captureRenewalCompletion,
  completeRegistrationForRenewalSeed,
  fillRenewalForm,
  startForcedRenewalJourney,
} from '../../helpers/renewal';
import { ScreenshotHelper } from '../../helpers/screenshots';
import { env } from '../../helpers/env';

test.describe('Renewal - normal customer, partial discount, no fines', () => {
  const admin = new AdminHelper();

  test.afterAll(async () => {
    await admin.close();
  });

  test('voucher applied on renewal form, then paid successfully', async ({ page, browser }) => {
    test.setTimeout(420_000);
    const ss = new ScreenshotHelper('renewal/partial-discount');
    const middleName = `${env.middleNamePrefix}${randomSuffix()}`;

    await admin.login(browser);
    const voucherCode = await admin.createVoucher(ss, {
      discountType: 'FIXED_CZK',
      discountValue: 100,
    });

    await completeRegistrationForRenewalSeed(page, browser, admin, ss, { middleName });

    await startForcedRenewalJourney(page, ss, { middleName });
    await fillRenewalForm(page, ss, {
      voucherCode,
      admin: { helper: admin, middleName },
    });
    await comgatePaymentSuccess(page, ss);
    await captureRenewalCompletion(page, ss);

    await admin.screenshotIdentityDetail(middleName, ss, 'renewal-after-payment');
    await admin.screenshotVoucherDetail(voucherCode, ss, 'voucher-detail-after-payment');
  });

  test('return from Comgate, apply voucher on payment page, then pay', async ({ page, browser }) => {
    test.setTimeout(420_000);
    const ss = new ScreenshotHelper('renewal/partial-discount-return-apply-pay');
    const middleName = `${env.middleNamePrefix}${randomSuffix()}`;

    await admin.login(browser);
    const voucherCode = await admin.createVoucher(ss, {
      discountType: 'FIXED_CZK',
      discountValue: 100,
    });

    await completeRegistrationForRenewalSeed(page, browser, admin, ss, { middleName });

    await startForcedRenewalJourney(page, ss, { middleName });
    await fillRenewalForm(page, ss, {
      admin: { helper: admin, middleName },
    });
    await comgatePaymentReturnToShop(page, ss);
    await ss.take(page, 'renewal-payment-pending-return');
    await applyVoucherOnPaymentPage(page, ss, voucherCode);

    await admin.screenshotIdentityDetail(middleName, ss, 'renewal-after-voucher-applied');

    await retryPayment(page, ss);
    await comgatePaymentSuccess(page, ss);
    await captureRenewalCompletion(page, ss);

    await admin.screenshotIdentityDetail(middleName, ss, 'renewal-after-payment');
    await admin.screenshotVoucherDetail(voucherCode, ss, 'voucher-detail-after-payment');
  });

  test('apply voucher on payment page, logout, re-verify, then pay', async ({ page, browser }) => {
    test.setTimeout(480_000);
    const ss = new ScreenshotHelper('renewal/partial-discount-return-apply-logout-relogin-pay');
    const middleName = `${env.middleNamePrefix}${randomSuffix()}`;

    await admin.login(browser);
    const voucherCode = await admin.createVoucher(ss, {
      discountType: 'FIXED_CZK',
      discountValue: 100,
    });

    await completeRegistrationForRenewalSeed(page, browser, admin, ss, { middleName });

    await startForcedRenewalJourney(page, ss, { middleName });
    await fillRenewalForm(page, ss, {
      admin: { helper: admin, middleName },
    });
    await comgatePaymentReturnToShop(page, ss);
    await ss.take(page, 'renewal-payment-pending-return');
    await applyVoucherOnPaymentPage(page, ss, voucherCode);

    await admin.screenshotIdentityDetail(middleName, ss, 'renewal-after-voucher-applied');

    await logout(page, ss);

    await startForcedRenewalJourney(page, ss, { middleName });
    await ss.take(page, 'renewal-payment-page-after-relogin-with-voucher');

    await retryPayment(page, ss);
    await comgatePaymentSuccess(page, ss);
    await captureRenewalCompletion(page, ss);

    await admin.screenshotIdentityDetail(middleName, ss, 'renewal-after-payment');
    await admin.screenshotVoucherDetail(voucherCode, ss, 'voucher-detail-after-payment');
  });
});
