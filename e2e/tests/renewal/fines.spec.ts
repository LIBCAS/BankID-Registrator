import { test } from '@playwright/test';
import { AdminHelper } from '../../helpers/admin';
import { comgatePaymentReturnToShop, comgatePaymentSuccess, retryPayment } from '../../helpers/flows';
import { captureRenewalCompletion, fillRenewalForm, getConfiguredRenewalFinesScenario, startForcedRenewalJourney } from '../../helpers/renewal';
import { ScreenshotHelper } from '../../helpers/screenshots';
import { resolveVoucherRecalculationSequence } from '../../helpers/vouchers';

test.describe('Renewal - env-gated patrons with outstanding fines', () => {
  const admin = new AdminHelper();

  test.afterAll(async () => {
    await admin.close();
  });

  test('normal customer, no voucher, fines > 0', async ({ page, browser }) => {
    test.setTimeout(180_000);
    const ss = new ScreenshotHelper('renewal/fines-normal-no-voucher');
    const scenario = getConfiguredRenewalFinesScenario('normalNoVoucher');
    test.skip(!scenario, 'Missing fined patron env block for normal no-voucher renewal scenario.');

    await admin.login(browser);
    await startForcedRenewalJourney(page, ss, {
      middleName: scenario!.middleName!,
      bankIdIdentity: scenario!.bankIdIdentity!,
    });

    await fillRenewalForm(page, ss, {
      admin: { helper: admin, middleName: scenario!.middleName! },
    });
    await comgatePaymentReturnToShop(page, ss);
    await retryPayment(page, ss);
    await comgatePaymentSuccess(page, ss);
    await captureRenewalCompletion(page, ss);

    await admin.screenshotIdentityDetail(scenario!.middleName!, ss, 'renewal-after-payment');
  });

  test('normal customer, partial voucher, fines > 0', async ({ page, browser }) => {
    test.setTimeout(240_000);
    const ss = new ScreenshotHelper('renewal/fines-normal-partial-voucher');
    const scenario = getConfiguredRenewalFinesScenario('normalPartialVoucher');
    test.skip(!scenario, 'Missing fined patron env block for normal partial-voucher renewal scenario.');

    await admin.login(browser);
    const voucherCode = await admin.createVoucher(ss, {
      discountType: 'FIXED_CZK',
      discountValue: 100,
    });

    await startForcedRenewalJourney(page, ss, {
      middleName: scenario!.middleName!,
      bankIdIdentity: scenario!.bankIdIdentity!,
    });

    await fillRenewalForm(page, ss, {
      voucherCode,
      admin: { helper: admin, middleName: scenario!.middleName! },
    });
    await comgatePaymentReturnToShop(page, ss);
    await retryPayment(page, ss);
    await comgatePaymentSuccess(page, ss);
    await captureRenewalCompletion(page, ss);

    await admin.screenshotIdentityDetail(scenario!.middleName!, ss, 'renewal-after-payment');
    await admin.screenshotVoucherDetail(voucherCode, ss, 'voucher-detail-after-payment');
  });

  test('normal customer, 100% voucher, fines > 0, submit only', async ({ page, browser }) => {
    test.setTimeout(240_000);
    const ss = new ScreenshotHelper('renewal/fines-normal-full-voucher-submit');
    const scenario = getConfiguredRenewalFinesScenario('normalFullVoucherSubmitOnly');
    test.skip(!scenario, 'Missing fined patron env block for normal full-voucher submit-only renewal scenario.');

    await admin.login(browser);
    const voucherCode = await admin.createVoucher(ss, {
      discountType: 'PERCENTAGE',
      discountValue: 100,
    });
    const voucherSequence = await resolveVoucherRecalculationSequence(admin, ss);

    await startForcedRenewalJourney(page, ss, {
      middleName: scenario!.middleName!,
      bankIdIdentity: scenario!.bankIdIdentity!,
    });

    await fillRenewalForm(page, ss, {
      voucherSequence: {
        ...voucherSequence,
        finalVoucher: voucherCode,
      },
      submitAction: 'submit',
      admin: { helper: admin, middleName: scenario!.middleName! },
    });
    await captureRenewalCompletion(page, ss);

    await admin.screenshotIdentityDetail(scenario!.middleName!, ss, 'renewal-after-completion');
    await admin.screenshotVoucherDetail(voucherCode, ss, 'voucher-detail-after-completion');
  });

  test('normal customer, 100% voucher, fines > 0, submit and pay', async ({ page, browser }) => {
    test.setTimeout(300_000);
    const ss = new ScreenshotHelper('renewal/fines-normal-full-voucher-submit-and-pay');
    const scenario = getConfiguredRenewalFinesScenario('normalFullVoucherSubmitAndPay');
    test.skip(!scenario, 'Missing fined patron env block for normal full-voucher submit-and-pay renewal scenario.');

    await admin.login(browser);
    const voucherCode = await admin.createVoucher(ss, {
      discountType: 'PERCENTAGE',
      discountValue: 100,
    });

    await startForcedRenewalJourney(page, ss, {
      middleName: scenario!.middleName!,
      bankIdIdentity: scenario!.bankIdIdentity!,
    });

    await fillRenewalForm(page, ss, {
      voucherCode,
      submitAction: 'submitAndPay',
      admin: { helper: admin, middleName: scenario!.middleName! },
    });
    await comgatePaymentReturnToShop(page, ss);
    await retryPayment(page, ss);
    await comgatePaymentSuccess(page, ss);
    await captureRenewalCompletion(page, ss);

    await admin.screenshotIdentityDetail(scenario!.middleName!, ss, 'renewal-after-payment');
    await admin.screenshotVoucherDetail(voucherCode, ss, 'voucher-detail-after-payment');
  });

  test('employee, no voucher, fines > 0, submit only', async ({ page, browser }) => {
    test.setTimeout(180_000);
    const ss = new ScreenshotHelper('renewal/fines-employee-no-voucher-submit');
    const scenario = getConfiguredRenewalFinesScenario('employeeNoVoucherSubmitOnly');
    test.skip(!scenario, 'Missing fined patron env block for employee no-voucher submit-only renewal scenario.');

    await admin.login(browser);
    await startForcedRenewalJourney(page, ss, {
      middleName: scenario!.middleName!,
      bankIdIdentity: scenario!.bankIdIdentity!,
    });

    await fillRenewalForm(page, ss, {
      isEmployee: true,
      submitAction: 'submit',
      admin: { helper: admin, middleName: scenario!.middleName! },
    });
    await captureRenewalCompletion(page, ss);

    await admin.screenshotIdentityDetail(scenario!.middleName!, ss, 'renewal-after-completion');
  });

  test('employee, no voucher, fines > 0, submit and pay', async ({ page, browser }) => {
    test.setTimeout(240_000);
    const ss = new ScreenshotHelper('renewal/fines-employee-no-voucher-submit-and-pay');
    const scenario = getConfiguredRenewalFinesScenario('employeeNoVoucherSubmitAndPay');
    test.skip(!scenario, 'Missing fined patron env block for employee no-voucher submit-and-pay renewal scenario.');

    await admin.login(browser);
    await startForcedRenewalJourney(page, ss, {
      middleName: scenario!.middleName!,
      bankIdIdentity: scenario!.bankIdIdentity!,
    });

    await fillRenewalForm(page, ss, {
      isEmployee: true,
      submitAction: 'submitAndPay',
      admin: { helper: admin, middleName: scenario!.middleName! },
    });
    await comgatePaymentReturnToShop(page, ss);
    await retryPayment(page, ss);
    await comgatePaymentSuccess(page, ss);
    await captureRenewalCompletion(page, ss);

    await admin.screenshotIdentityDetail(scenario!.middleName!, ss, 'renewal-after-payment');
  });
});
