import { test } from '@playwright/test';
import { env } from '../../helpers/env';
import { AdminHelper } from '../../helpers/admin';
import { randomSuffix } from '../../helpers/random';
import { completeRegistrationForRenewalSeed } from '../../helpers/renewal';
import { ScreenshotHelper } from '../../helpers/screenshots';

function buildSnippet(prefix: string, patron: {
  bankIdIdentity: string;
  middleName: string;
  alephBarcode: string;
  alephId: string;
}): string {
  return [
    `${prefix}_BANKID_IDENTITY=${patron.bankIdIdentity}`,
    `${prefix}_MIDDLE_NAME=${patron.middleName}`,
    `${prefix}_PATRON_BARCODE=${patron.alephBarcode}`,
    `${prefix}_PATRON_ALEPH_ID=${patron.alephId}`,
  ].join('\n');
}

async function createPatronForFinesScenario(
  browser: Parameters<typeof test>[0]['browser'],
  page: Parameters<typeof test>[0]['page'],
  admin: AdminHelper,
  screenshotPath: string,
  middleName: string,
  bankIdIdentity: string,
  isEmployee: boolean
) {
  const ss = new ScreenshotHelper(screenshotPath);
  return completeRegistrationForRenewalSeed(page, browser, admin, ss, {
    middleName,
    bankIdIdentity,
    isEmployee,
  });
}

test.describe('Renewal bootstrap - fined patron candidates', () => {
  const admin = new AdminHelper();

  test.afterAll(async () => {
    await admin.close();
  });

  test('create patron for normal no-voucher fines scenario', async ({ page, browser }, testInfo) => {
    test.setTimeout(240_000);

    const bankIdIdentity = env.renewalFinesScenarios.normalNoVoucher.bankIdIdentity;
    test.skip(!bankIdIdentity, 'Missing RENEWAL_FINES_NORMAL_NO_VOUCHER_BANKID_IDENTITY in e2e/.env');

    const patron = await createPatronForFinesScenario(
      browser,
      page,
      admin,
      'renewal/bootstrap-fines-normal-no-voucher',
      `${env.middleNamePrefix}${randomSuffix()}`,
      bankIdIdentity!,
      false
    );

    const snippet = buildSnippet('RENEWAL_FINES_NORMAL_NO_VOUCHER', patron);
    console.log(`\n${snippet}\n`);
    await testInfo.attach('renewal-fines-normal-no-voucher-env', {
      body: snippet,
      contentType: 'text/plain',
    });
  });

  test('create patron for normal partial-voucher fines scenario', async ({ page, browser }, testInfo) => {
    test.setTimeout(240_000);

    const bankIdIdentity = env.renewalFinesScenarios.normalPartialVoucher.bankIdIdentity;
    test.skip(!bankIdIdentity, 'Missing RENEWAL_FINES_NORMAL_PARTIAL_VOUCHER_BANKID_IDENTITY in e2e/.env');

    const patron = await createPatronForFinesScenario(
      browser,
      page,
      admin,
      'renewal/bootstrap-fines-normal-partial-voucher',
      `${env.middleNamePrefix}${randomSuffix()}`,
      bankIdIdentity!,
      false
    );

    const snippet = buildSnippet('RENEWAL_FINES_NORMAL_PARTIAL_VOUCHER', patron);
    console.log(`\n${snippet}\n`);
    await testInfo.attach('renewal-fines-normal-partial-voucher-env', {
      body: snippet,
      contentType: 'text/plain',
    });
  });

  test('create patron for normal full-voucher submit-only fines scenario', async ({ page, browser }, testInfo) => {
    test.setTimeout(240_000);

    const bankIdIdentity = env.renewalFinesScenarios.normalFullVoucherSubmitOnly.bankIdIdentity;
    test.skip(!bankIdIdentity, 'Missing RENEWAL_FINES_NORMAL_FULL_VOUCHER_SUBMIT_ONLY_BANKID_IDENTITY in e2e/.env');

    const patron = await createPatronForFinesScenario(
      browser,
      page,
      admin,
      'renewal/bootstrap-fines-normal-full-voucher',
      `${env.middleNamePrefix}${randomSuffix()}`,
      bankIdIdentity!,
      false
    );

    const snippet = buildSnippet('RENEWAL_FINES_NORMAL_FULL_VOUCHER_SUBMIT_ONLY', patron);
    console.log(`\n${snippet}\n`);
    await testInfo.attach('renewal-fines-normal-full-voucher-submit-only-env', {
      body: snippet,
      contentType: 'text/plain',
    });
  });

  test('create patron for normal full-voucher submit-and-pay fines scenario', async ({ page, browser }, testInfo) => {
    test.setTimeout(240_000);

    const bankIdIdentity = env.renewalFinesScenarios.normalFullVoucherSubmitAndPay.bankIdIdentity;
    test.skip(!bankIdIdentity, 'Missing RENEWAL_FINES_NORMAL_FULL_VOUCHER_SUBMIT_AND_PAY_BANKID_IDENTITY in e2e/.env');

    const patron = await createPatronForFinesScenario(
      browser,
      page,
      admin,
      'renewal/bootstrap-fines-normal-full-voucher-submit-and-pay',
      `${env.middleNamePrefix}${randomSuffix()}`,
      bankIdIdentity!,
      false
    );

    const snippet = buildSnippet('RENEWAL_FINES_NORMAL_FULL_VOUCHER_SUBMIT_AND_PAY', patron);
    console.log(`\n${snippet}\n`);
    await testInfo.attach('renewal-fines-normal-full-voucher-submit-and-pay-env', {
      body: snippet,
      contentType: 'text/plain',
    });
  });

  test('create patron for employee no-voucher submit-only fines scenario', async ({ page, browser }, testInfo) => {
    test.setTimeout(240_000);

    const bankIdIdentity = env.renewalFinesScenarios.employeeNoVoucherSubmitOnly.bankIdIdentity;
    test.skip(!bankIdIdentity, 'Missing RENEWAL_FINES_EMPLOYEE_NO_VOUCHER_SUBMIT_ONLY_BANKID_IDENTITY in e2e/.env');

    const patron = await createPatronForFinesScenario(
      browser,
      page,
      admin,
      'renewal/bootstrap-fines-employee-no-voucher-submit-only',
      `${env.middleNamePrefix}${randomSuffix()}`,
      bankIdIdentity!,
      true
    );

    const snippet = buildSnippet('RENEWAL_FINES_EMPLOYEE_NO_VOUCHER_SUBMIT_ONLY', patron);
    console.log(`\n${snippet}\n`);
    await testInfo.attach('renewal-fines-employee-no-voucher-submit-only-env', {
      body: snippet,
      contentType: 'text/plain',
    });
  });

  test('create patron for employee no-voucher submit-and-pay fines scenario', async ({ page, browser }, testInfo) => {
    test.setTimeout(240_000);

    const bankIdIdentity = env.renewalFinesScenarios.employeeNoVoucherSubmitAndPay.bankIdIdentity;
    test.skip(!bankIdIdentity, 'Missing RENEWAL_FINES_EMPLOYEE_NO_VOUCHER_SUBMIT_AND_PAY_BANKID_IDENTITY in e2e/.env');

    const patron = await createPatronForFinesScenario(
      browser,
      page,
      admin,
      'renewal/bootstrap-fines-employee-no-voucher-submit-and-pay',
      `${env.middleNamePrefix}${randomSuffix()}`,
      bankIdIdentity!,
      true
    );

    const snippet = buildSnippet('RENEWAL_FINES_EMPLOYEE_NO_VOUCHER_SUBMIT_AND_PAY', patron);
    console.log(`\n${snippet}\n`);
    await testInfo.attach('renewal-fines-employee-no-voucher-submit-and-pay-env', {
      body: snippet,
      contentType: 'text/plain',
    });
  });
});
