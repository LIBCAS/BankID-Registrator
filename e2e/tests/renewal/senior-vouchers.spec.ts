import { expect, test } from '@playwright/test';
import { AdminHelper } from '../../helpers/admin';
import { env } from '../../helpers/env';
import { captureWelcomeLoggedOutState, comgatePaymentSuccess } from '../../helpers/flows';
import { randomSuffix } from '../../helpers/random';
import {
  captureRenewalCompletion,
  completeRegistrationForRenewalSeed,
  fillRenewalForm,
  startForcedRenewalJourney,
} from '../../helpers/renewal';
import { ScreenshotHelper } from '../../helpers/screenshots';
import { seniorVoucherScenarios } from '../../helpers/senior-vouchers';

test.describe('Renewal - senior customer, voucher discounts, no fines', () => {
  for (const scenario of seniorVoucherScenarios) {
    test(scenario.name, async ({ page, browser }) => {
      test.skip(!env.bankIdIdentitySenior,
        'Set BANKID_SANDBOX_IDENTITY_SENIOR=LeosV in e2e/.env to run senior scenarios.');
      test.setTimeout(420_000 + env.ldapSyncTimeoutMs);
      const ss = new ScreenshotHelper(`renewal/senior-${scenario.name}`);
      const admin = new AdminHelper();
      const middleName = env.middleNamePrefix + randomSuffix();
      try {
        const patron = await completeRegistrationForRenewalSeed(page, browser, admin, ss, {
          middleName,
          bankIdIdentity: env.bankIdIdentitySenior,
          expectedRegistrationFeeCzk: 150,
        });
        await expect.poll(() => admin.getIdentityPaymentStatus(middleName), {
          timeout: 30_000,
        }).toBe('Zaplaceno ✔');
        const previousExpiry = await admin.getMembershipExpiry(middleName);
        const expectedExpiry = new Date(`${previousExpiry}T00:00:00Z`);
        expectedExpiry.setUTCDate(expectedExpiry.getUTCDate() + 365);

        const voucherCode = await admin.createVoucher(ss, {
          recipientType: 'SENIOR',
          discountType: scenario.discountType, discountValue: scenario.value,
        });
        await startForcedRenewalJourney(page, ss, {
          middleName, bankIdIdentity: patron.bankIdIdentity,
        });
        await expect(page.locator('[data-standard-renewal-fee]'))
          .toHaveAttribute('data-standard-renewal-fee', /^150(?:\.0+)?$/);
        await expect(page.locator('[data-outstanding-fines]'))
          .toHaveAttribute('data-outstanding-fines', /^0(?:\.0+)?$/);

        // Track renewal only; the seed registration legitimately used Comgate.
        const paymentNavigations: string[] = [];
        page.on('request', request => {
          if (!request.isNavigationRequest()) return;
          const url = new URL(request.url());
          if (url.hostname.endsWith('.comgate.cz') || url.hostname === 'comgate.cz'
              || url.pathname.endsWith('/payment/initiate')) {
            paymentNavigations.push(request.url());
          }
        });
        await fillRenewalForm(page, ss, {
          voucherCode,
          expectedVoucherPreview: {
            feeAmount: 150, discountAmount: scenario.discount, amountToPay: scenario.payable,
          },
          admin: { helper: admin, middleName },
        });
        if (scenario.payable > 0) {
          expect(await admin.getIdentityPaymentStatus(middleName)).toBe('Nezaplaceno!');
          await comgatePaymentSuccess(page, ss, { expectedAmountCzk: scenario.payable });
          await expect(page.locator('.payment-success-renewal')).toBeVisible();
        } else {
          await expect(page.locator('body.page-membership-renewal-success')).toBeVisible();
          expect(paymentNavigations, 'Waived renewal must not enter the payment gateway').toEqual([]);
        }
        await captureRenewalCompletion(page, ss);
        // Paid seed registration remains paid for a waived renewal; no new debt may remain.
        await expect.poll(() => admin.getIdentityPaymentStatus(middleName), {
          timeout: 30_000, intervals: [1_000, 3_000, 5_000],
        }).toBe('Zaplaceno ✔');
        expect(await admin.getIdentityIdentifiers(middleName)).toEqual({
          alephId: patron.alephId, alephBarcode: patron.alephBarcode,
        });
        expect(await admin.getMembershipExpiry(middleName)).toBe(expectedExpiry.toISOString().slice(0, 10));
        await admin.screenshotIdentityDetail(middleName, ss, 'renewal-after-completion');
        await admin.screenshotVoucherDetail(voucherCode, ss, 'renewal-voucher-after-completion');
        await captureWelcomeLoggedOutState(page, ss);
      } finally {
        await admin.close();
      }
    });
  }
});
