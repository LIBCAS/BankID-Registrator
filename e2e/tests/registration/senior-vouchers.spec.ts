import { expect, test } from '@playwright/test';
import { env } from '../../helpers/env';
import { ScreenshotHelper } from '../../helpers/screenshots';
import { configureToolkit } from '../../helpers/toolkit';
import { randomSuffix } from '../../helpers/random';
import { AdminHelper } from '../../helpers/admin';
import {
  bankIdVerification, fillRegistrationForm, setPassword,
  comgatePaymentSuccess, waitFinalPage,
} from '../../helpers/flows';

import { seniorVoucherScenarios } from '../../helpers/senior-vouchers';

test.describe('Registration - senior customer, voucher discounts', () => {
  for (const scenario of seniorVoucherScenarios) {
    test(scenario.name, async ({ page, browser }) => {
      test.skip(!env.bankIdIdentitySenior,
        'Set BANKID_SANDBOX_IDENTITY_SENIOR=LeosV in e2e/.env to run senior scenarios.');
      test.setTimeout(180_000 + env.ldapSyncTimeoutMs);
      const ss = new ScreenshotHelper(`registration/senior-${scenario.name}`);
      const admin = new AdminHelper();
      const middleName = env.middleNamePrefix + randomSuffix();
      try {
        await admin.login(browser);
        const voucherCode = await admin.createVoucher(ss, {
          recipientType: 'SENIOR',
          discountType: scenario.discountType,
          discountValue: scenario.value,
        });
        await configureToolkit(page, { middleName, forceRenewal: false });
        await bankIdVerification(page, ss, { identityName: env.bankIdIdentitySenior });
        await fillRegistrationForm(page, ss);
        await admin.screenshotIdentityDetail(middleName, ss, 'after-patron-creation');

        // Full coverage must bypass both payment initiation and Comgate.
        const paymentNavigations: string[] = [];
        page.on('request', request => {
          if (!request.isNavigationRequest()) return;
          const url = new URL(request.url());
          if (url.hostname.endsWith('.comgate.cz') || url.hostname === 'comgate.cz'
              || url.pathname.endsWith('/payment/initiate')) {
            paymentNavigations.push(request.url());
          }
        });
        await setPassword(page, ss, {
          voucherCode,
          expectedVoucherPreview: {
            feeAmount: 150, discountAmount: scenario.discount, amountToPay: scenario.payable,
          },
        });

        if (scenario.payable > 0) {
          expect(await admin.getIdentityPaymentStatus(middleName)).toBe('Nezaplaceno!');
          await comgatePaymentSuccess(page, ss, { expectedAmountCzk: scenario.payable });
        } else {
          expect(paymentNavigations, 'Fully waived registration must not enter payment gateway').toEqual([]);
        }

        await expect(page).toHaveURL(/\/payment\/callback\?.*status=success/);
        await expect(page.locator('.payment-success-registration')).toBeVisible();
        // Independently catches the original false-success/unpaid-Aleph-charge bug.
        await expect.poll(() => admin.getIdentityPaymentStatus(middleName), {
          timeout: 30_000, intervals: [1_000, 3_000, 5_000],
        }).toBe(scenario.payable > 0 ? 'Zaplaceno ✔' : 'Bezplatné / neznámo');
        await waitFinalPage(page, ss);
        await admin.screenshotIdentityDetail(middleName, ss, 'after-completion');
        await admin.screenshotVoucherDetail(voucherCode, ss, 'voucher-after-completion');
      } finally {
        await admin.close();
      }
    });
  }
});
