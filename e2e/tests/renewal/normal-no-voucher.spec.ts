import { test } from '@playwright/test';
import { AdminHelper } from '../../helpers/admin';
import {
  comgatePaymentCancel,
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

test.describe('Renewal - normal customer, no voucher, no fines', () => {
  const admin = new AdminHelper();

  test.afterAll(async () => {
    await admin.close();
  });

  test('full flow with successful payment', async ({ page, browser }) => {
    test.setTimeout(360_000);
    const ss = new ScreenshotHelper('renewal/normal-no-voucher');
    const middleName = `${env.middleNamePrefix}${randomSuffix()}`;

    await completeRegistrationForRenewalSeed(page, browser, admin, ss, { middleName });

    await startForcedRenewalJourney(page, ss, { middleName });
    await fillRenewalForm(page, ss, {
      admin: { helper: admin, middleName },
    });
    await comgatePaymentSuccess(page, ss);
    await captureRenewalCompletion(page, ss);

    await admin.screenshotIdentityDetail(middleName, ss, 'renewal-after-payment');
  });

  test('payment cancelled, then retried successfully', async ({ page, browser }) => {
    test.setTimeout(420_000);
    const ss = new ScreenshotHelper('renewal/normal-no-voucher-cancel-retry');
    const middleName = `${env.middleNamePrefix}${randomSuffix()}`;

    await completeRegistrationForRenewalSeed(page, browser, admin, ss, { middleName });

    await startForcedRenewalJourney(page, ss, { middleName });
    await fillRenewalForm(page, ss, {
      admin: { helper: admin, middleName },
    });
    await comgatePaymentCancel(page, ss);
    await ss.take(page, 'renewal-payment-cancelled');

    await admin.screenshotIdentityDetail(middleName, ss, 'renewal-after-payment-cancelled');

    await retryPayment(page, ss);
    await comgatePaymentSuccess(page, ss);
    await captureRenewalCompletion(page, ss);

    await admin.screenshotIdentityDetail(middleName, ss, 'renewal-after-payment');
  });

  test('returns from Comgate, then retries successfully', async ({ page, browser }) => {
    test.setTimeout(420_000);
    const ss = new ScreenshotHelper('renewal/normal-no-voucher-return-retry');
    const middleName = `${env.middleNamePrefix}${randomSuffix()}`;

    await completeRegistrationForRenewalSeed(page, browser, admin, ss, { middleName });

    await startForcedRenewalJourney(page, ss, { middleName });
    await fillRenewalForm(page, ss, {
      admin: { helper: admin, middleName },
    });
    await comgatePaymentReturnToShop(page, ss);
    await ss.take(page, 'renewal-payment-pending-return');

    await retryPayment(page, ss);
    await comgatePaymentSuccess(page, ss);
    await captureRenewalCompletion(page, ss);

    await admin.screenshotIdentityDetail(middleName, ss, 'renewal-after-payment');
  });

  test('logout on payment page, re-verification, then pay', async ({ page, browser }) => {
    test.setTimeout(420_000);
    const ss = new ScreenshotHelper('renewal/normal-no-voucher-relogin-payment');
    const middleName = `${env.middleNamePrefix}${randomSuffix()}`;

    await completeRegistrationForRenewalSeed(page, browser, admin, ss, { middleName });

    await startForcedRenewalJourney(page, ss, { middleName });
    await fillRenewalForm(page, ss, {
      admin: { helper: admin, middleName },
    });
    await comgatePaymentReturnToShop(page, ss);
    await ss.take(page, 'renewal-payment-pending-return');

    await logout(page, ss);

    await startForcedRenewalJourney(page, ss, { middleName });
    await ss.take(page, 'renewal-payment-page-after-relogin');

    await retryPayment(page, ss);
    await comgatePaymentSuccess(page, ss);
    await captureRenewalCompletion(page, ss);

    await admin.screenshotIdentityDetail(middleName, ss, 'renewal-after-payment');
  });
});
