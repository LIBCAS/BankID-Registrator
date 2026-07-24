import { test } from '@playwright/test';
import { AdminHelper } from '../../helpers/admin';
import {
  applyVoucherOnPaymentPage,
  captureWelcomeLoggedOutState,
  comgatePaymentReturnToShop,
} from '../../helpers/flows';
import { randomSuffix } from '../../helpers/random';
import {
  captureRenewalCompletion,
  captureRenewalCompletionFromPaymentCallbackSuccess,
  completeRegistrationForRenewalSeed,
  fillRenewalForm,
  startForcedRenewalJourney,
} from '../../helpers/renewal';
import { ScreenshotHelper } from '../../helpers/screenshots';
import { env } from '../../helpers/env';
import { resolveVoucherRecalculationSequence } from '../../helpers/vouchers';

test.describe('Renewal - normal customer, 100% discount, no fines', () => {
  const admin = new AdminHelper();

  test.afterAll(async () => {
    await admin.close();
  });

  test('voucher applied on renewal form, then completes without payment', async ({ page, browser }) => {
    test.setTimeout(420_000);
    const ss = new ScreenshotHelper('renewal/full-discount');
    const middleName = `${env.middleNamePrefix}${randomSuffix()}`;

    await admin.login(browser);
    const voucherCode = await admin.createVoucher(ss, {
      discountType: 'PERCENTAGE',
      discountValue: 100,
    });
    const voucherSequence = await resolveVoucherRecalculationSequence(admin, ss);

    await completeRegistrationForRenewalSeed(page, browser, admin, ss, { middleName });

    await startForcedRenewalJourney(page, ss, { middleName });
    await fillRenewalForm(page, ss, {
      voucherSequence: {
        ...voucherSequence,
        finalVoucher: voucherCode,
      },
      admin: { helper: admin, middleName },
    });
    await captureRenewalCompletion(page, ss);

    await admin.screenshotIdentityDetail(middleName, ss, 'renewal-after-completion');
    await admin.screenshotVoucherDetail(voucherCode, ss, 'voucher-detail-after-completion');
    await captureWelcomeLoggedOutState(page, ss);
  });

  test('return from Comgate, apply 100% voucher on payment page, then complete', async ({ page, browser }) => {
    test.setTimeout(420_000);
    const ss = new ScreenshotHelper('renewal/full-discount-return-apply-complete');
    const middleName = `${env.middleNamePrefix}${randomSuffix()}`;

    await admin.login(browser);
    const voucherCode = await admin.createVoucher(ss, {
      discountType: 'PERCENTAGE',
      discountValue: 100,
    });

    await completeRegistrationForRenewalSeed(page, browser, admin, ss, { middleName });

    await startForcedRenewalJourney(page, ss, { middleName });
    await fillRenewalForm(page, ss, {
      admin: { helper: admin, middleName },
    });
    await comgatePaymentReturnToShop(page, ss);
    await ss.take(page, 'renewal-payment-pending-return');
    const voucherApplyResult = await applyVoucherOnPaymentPage(page, ss, voucherCode);

    if (voucherApplyResult === 'redirected-to-success') {
      await captureRenewalCompletionFromPaymentCallbackSuccess(page, ss);
    } else {
      await captureRenewalCompletion(page, ss);
    }

    await admin.screenshotIdentityDetail(middleName, ss, 'renewal-after-completion');
    await admin.screenshotVoucherDetail(voucherCode, ss, 'voucher-detail-after-completion');
    await captureWelcomeLoggedOutState(page, ss);
  });
});
