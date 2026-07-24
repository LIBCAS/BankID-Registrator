import { AdminHelper } from './admin';
import { env } from './env';
import { ScreenshotHelper } from './screenshots';

export type VoucherRecalculationSequence = {
  invalidVoucher: string;
  partialVoucher1: string;
  partialVoucher2: string;
};

export async function resolveVoucherRecalculationSequence(
  admin: AdminHelper,
  ss: ScreenshotHelper
): Promise<VoucherRecalculationSequence> {
  const partialVoucher1 = env.partialDiscountVoucher1 || await admin.createVoucher(ss, {
    code: `E2E-PART1-${Date.now()}`,
    discountType: 'FIXED_CZK',
    discountValue: 80,
  });

  const partialVoucher2 = env.partialDiscountVoucher2 || await admin.createVoucher(ss, {
    code: `E2E-PART2-${Date.now()}`,
    discountType: 'FIXED_CZK',
    discountValue: 140,
  });

  return {
    invalidVoucher: env.invalidVoucher,
    partialVoucher1,
    partialVoucher2,
  };
}
