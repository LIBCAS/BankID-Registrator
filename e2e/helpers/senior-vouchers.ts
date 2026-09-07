// Independent expectations: the testing server and Aleph must use a 150 CZK senior tariff.
export const seniorVoucherScenarios = [
  { name: 'fixed-75', discountType: 'FIXED_CZK', value: 75, discount: 75, payable: 75 },
  { name: 'fixed-150', discountType: 'FIXED_CZK', value: 150, discount: 150, payable: 0 },
  { name: 'fixed-280-capped', discountType: 'FIXED_CZK', value: 280, discount: 150, payable: 0 },
  { name: 'percentage-50', discountType: 'PERCENTAGE', value: 50, discount: 75, payable: 75 },
  { name: 'percentage-100', discountType: 'PERCENTAGE', value: 100, discount: 150, payable: 0 },
] as const;

