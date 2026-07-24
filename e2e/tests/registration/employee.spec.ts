import { test } from '@playwright/test';
import { env } from '../../helpers/env';
import { ScreenshotHelper } from '../../helpers/screenshots';
import { configureToolkit } from '../../helpers/toolkit';
import { randomSuffix } from '../../helpers/random';
import { AdminHelper } from '../../helpers/admin';
import {
  bankIdVerification,
  captureWelcomeLoggedOutState,
  fillEmployeeRegistrationForm,
  setPassword,
  waitFinalPage,
  logout,
} from '../../helpers/flows';

test.describe('Registration - CAS employee', () => {
  const admin = new AdminHelper();

  test.afterAll(async () => {
    await admin.close();
  });

  test('full flow until final page (no payment)', async ({ page, browser }) => {
    const ss = new ScreenshotHelper('registration/employee');
    test.setTimeout(180_000);

    const middleName = env.middleNamePrefix + randomSuffix();

    await configureToolkit(page, { middleName, forceRenewal: false });
    await ss.take(page, 'toolkit-configured');

    await bankIdVerification(page, ss);

    // Employee registration form — checks employee box, fills email, uploads doc
    await fillEmployeeRegistrationForm(page, ss);

    // Admin: identity + Aleph patron were created (employee, no fee)
    await admin.login(browser);
    await admin.screenshotIdentityDetail(middleName, ss, 'after-patron-creation');

    // Password-setting form — employee sees "Nastavit heslo" (no payment mention)
    await setPassword(page, ss);

    // Admin: password was set
    await admin.screenshotIdentityDetail(middleName, ss, 'after-password-set');

    // Employee goes directly to final page (no Comgate)
    await waitFinalPage(page, ss);
    await captureWelcomeLoggedOutState(page, ss);

    // Admin: registration complete
    await admin.screenshotIdentityDetail(middleName, ss, 'after-completion');
  });

  test('logout on password page, re-login and complete', async ({ page, browser }) => {
    const ss = new ScreenshotHelper('registration/employee-relogin-password');
    test.setTimeout(240_000);

    const middleName = env.middleNamePrefix + randomSuffix();

    await configureToolkit(page, { middleName, forceRenewal: false });
    await ss.take(page, 'toolkit-configured');

    await bankIdVerification(page, ss);
    await fillEmployeeRegistrationForm(page, ss);

    await admin.login(browser);
    await admin.screenshotIdentityDetail(middleName, ss, 'after-patron-creation');

    // On password-setting page — log out before completing
    await ss.take(page, 'password-form-before-logout');
    await logout(page, ss);

    // Re-authenticate via BankID — should land back on password-setting page
    await configureToolkit(page, { middleName, forceRenewal: false });
    await bankIdVerification(page, ss);

    // Should be on password form again (passwordSet == false)
    await setPassword(page, ss);

    await admin.screenshotIdentityDetail(middleName, ss, 'after-password-set');

    await waitFinalPage(page, ss);
    await captureWelcomeLoggedOutState(page, ss);

    await admin.screenshotIdentityDetail(middleName, ss, 'after-completion');
  });
});
