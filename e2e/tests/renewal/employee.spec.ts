import { test } from '@playwright/test';
import { AdminHelper } from '../../helpers/admin';
import { logout } from '../../helpers/flows';
import { randomSuffix } from '../../helpers/random';
import {
  captureRenewalCompletion,
  completeRegistrationForRenewalSeed,
  fillRenewalForm,
  startForcedRenewalJourney,
} from '../../helpers/renewal';
import { ScreenshotHelper } from '../../helpers/screenshots';
import { env } from '../../helpers/env';

test.describe('Renewal - employee, no voucher, no fines', () => {
  const admin = new AdminHelper();

  test.afterAll(async () => {
    await admin.close();
  });

  test('full flow until success page without payment', async ({ page, browser }) => {
    test.setTimeout(360_000);
    const ss = new ScreenshotHelper('renewal/employee');
    const middleName = `${env.middleNamePrefix}${randomSuffix()}`;

    await completeRegistrationForRenewalSeed(page, browser, admin, ss, {
      middleName,
      isEmployee: true,
    });

    await startForcedRenewalJourney(page, ss, { middleName });
    await fillRenewalForm(page, ss, {
      isEmployee: true,
      admin: { helper: admin, middleName },
    });
    await captureRenewalCompletion(page, ss);

    await admin.screenshotIdentityDetail(middleName, ss, 'renewal-after-completion');
  });

  test('logout on renewal form, re-verify and complete', async ({ page, browser }) => {
    test.setTimeout(420_000);
    const ss = new ScreenshotHelper('renewal/employee-relogin-renewal-form');
    const middleName = `${env.middleNamePrefix}${randomSuffix()}`;

    await completeRegistrationForRenewalSeed(page, browser, admin, ss, {
      middleName,
      isEmployee: true,
    });

    await startForcedRenewalJourney(page, ss, { middleName });
    await ss.take(page, 'renewal-form-before-logout');
    await logout(page, ss);

    await startForcedRenewalJourney(page, ss, { middleName });
    await fillRenewalForm(page, ss, {
      isEmployee: true,
      admin: { helper: admin, middleName },
    });
    await captureRenewalCompletion(page, ss);

    await admin.screenshotIdentityDetail(middleName, ss, 'renewal-after-completion');
  });
});
