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
  waitFinalPage,
} from '../../helpers/flows';

// ---------------------------------------------------------------------------
// Test
// ---------------------------------------------------------------------------
test.describe('Registration - normal customer, no discount', () => {
  const ss = new ScreenshotHelper('registration/normal-no-discount');
  const admin = new AdminHelper();

  test.afterAll(async () => {
    await admin.close();
  });

  test('full flow with successful payment', async ({ page, browser }) => {
    test.setTimeout(180_000);

    const middleName = env.middleNamePrefix + randomSuffix();

    // Welcome page — configure Tester's Toolkit
    await configureToolkit(page, { middleName, forceRenewal: false });
    await ss.take(page, 'toolkit-configured');

    // Bank iD sandbox — identity verification
    await bankIdVerification(page, ss);

    // Registration form
    await fillRegistrationForm(page, ss);

    // Admin: identity + Aleph patron were just created
    await admin.login(browser);
    await admin.screenshotIdentityDetail(middleName, ss, 'after-patron-creation');

    // Password-setting form
    await setPassword(page, ss);

    // Admin: patron password was just set
    await admin.screenshotIdentityDetail(middleName, ss, 'after-password-set');

    // Comgate payment
    await comgatePaymentSuccess(page, ss);

    // Final page + LDAP sync wait
    await waitFinalPage(page, ss);

    // Admin: registration complete
    await admin.screenshotIdentityDetail(middleName, ss, 'after-payment');
  });
});