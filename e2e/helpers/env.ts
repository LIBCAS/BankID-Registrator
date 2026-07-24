/**
 * Validated, required environment variables for E2E tests.
 *
 * All variables are validated eagerly at module load time.
 * If any variable is missing, the test suite fails immediately
 * with a descriptive error pointing to the .env file.
 */

function requireEnv(name: string): string {
  const value = process.env[name];
  if (!value) {
    throw new Error(
      `Required environment variable ${name} is not set. ` +
      `Define it in your e2e/.env file. See e2e/.env.example for reference.`
    );
  }
  return value;
}

function optionalEnv(name: string): string | undefined {
  const value = process.env[name]?.trim();
  return value ? value : undefined;
}

export type RenewalFinesScenarioKey =
  | 'normalNoVoucher'
  | 'normalPartialVoucher'
  | 'normalFullVoucherSubmitOnly'
  | 'normalFullVoucherSubmitAndPay'
  | 'employeeNoVoucherSubmitOnly'
  | 'employeeNoVoucherSubmitAndPay';

export type RenewalFinesScenarioConfig = {
  bankIdIdentity?: string;
  middleName?: string;
  patronBarcode?: string;
  patronAlephId?: string;
};

export const env = {
  baseUrl: requireEnv('BASE_URL'),
  contextPath: requireEnv('APP_CONTEXT_PATH'),
  adminUsername: requireEnv('ADMIN_USERNAME'),
  adminPassword: requireEnv('ADMIN_PASSWORD'),
  bankIdIdentity: requireEnv('BANKID_SANDBOX_IDENTITY'),
  middleNamePrefix: requireEnv('TEST_MIDDLE_NAME_PREFIX'),
  patronPassword: requireEnv('PATRON_PASSWORD'),
  employeeEmailDomain: requireEnv('EMPLOYEE_EMAIL_DOMAIN'),
  employeeDocPath: requireEnv('EMPLOYEE_DOC_PATH'),
  ldapSyncTimeoutMs: Number(requireEnv('LDAP_SYNC_TIMEOUT_MS')),
  screenshotDir: requireEnv('SCREENSHOT_DIR'),
  testerEmail: optionalEnv('E2E_TESTER_EMAIL'),
  invalidVoucher: optionalEnv('INVALID_VOUCHER') ?? 'INVALID-CODE',
  partialDiscountVoucher1: optionalEnv('PARTIAL_DISCOUNT_VOUCHER1'),
  partialDiscountVoucher2: optionalEnv('PARTIAL_DISCOUNT_VOUCHER2'),
  renewalFinesScenarios: {
    normalNoVoucher: {
      bankIdIdentity: optionalEnv('RENEWAL_FINES_NORMAL_NO_VOUCHER_BANKID_IDENTITY'),
      middleName: optionalEnv('RENEWAL_FINES_NORMAL_NO_VOUCHER_MIDDLE_NAME'),
      patronBarcode: optionalEnv('RENEWAL_FINES_NORMAL_NO_VOUCHER_PATRON_BARCODE'),
      patronAlephId: optionalEnv('RENEWAL_FINES_NORMAL_NO_VOUCHER_PATRON_ALEPH_ID'),
    },
    normalPartialVoucher: {
      bankIdIdentity: optionalEnv('RENEWAL_FINES_NORMAL_PARTIAL_VOUCHER_BANKID_IDENTITY'),
      middleName: optionalEnv('RENEWAL_FINES_NORMAL_PARTIAL_VOUCHER_MIDDLE_NAME'),
      patronBarcode: optionalEnv('RENEWAL_FINES_NORMAL_PARTIAL_VOUCHER_PATRON_BARCODE'),
      patronAlephId: optionalEnv('RENEWAL_FINES_NORMAL_PARTIAL_VOUCHER_PATRON_ALEPH_ID'),
    },
    normalFullVoucherSubmitOnly: {
      bankIdIdentity: optionalEnv('RENEWAL_FINES_NORMAL_FULL_VOUCHER_SUBMIT_ONLY_BANKID_IDENTITY'),
      middleName: optionalEnv('RENEWAL_FINES_NORMAL_FULL_VOUCHER_SUBMIT_ONLY_MIDDLE_NAME'),
      patronBarcode: optionalEnv('RENEWAL_FINES_NORMAL_FULL_VOUCHER_SUBMIT_ONLY_PATRON_BARCODE'),
      patronAlephId: optionalEnv('RENEWAL_FINES_NORMAL_FULL_VOUCHER_SUBMIT_ONLY_PATRON_ALEPH_ID'),
    },
    normalFullVoucherSubmitAndPay: {
      bankIdIdentity: optionalEnv('RENEWAL_FINES_NORMAL_FULL_VOUCHER_SUBMIT_AND_PAY_BANKID_IDENTITY'),
      middleName: optionalEnv('RENEWAL_FINES_NORMAL_FULL_VOUCHER_SUBMIT_AND_PAY_MIDDLE_NAME'),
      patronBarcode: optionalEnv('RENEWAL_FINES_NORMAL_FULL_VOUCHER_SUBMIT_AND_PAY_PATRON_BARCODE'),
      patronAlephId: optionalEnv('RENEWAL_FINES_NORMAL_FULL_VOUCHER_SUBMIT_AND_PAY_PATRON_ALEPH_ID'),
    },
    employeeNoVoucherSubmitOnly: {
      bankIdIdentity: optionalEnv('RENEWAL_FINES_EMPLOYEE_NO_VOUCHER_SUBMIT_ONLY_BANKID_IDENTITY'),
      middleName: optionalEnv('RENEWAL_FINES_EMPLOYEE_NO_VOUCHER_SUBMIT_ONLY_MIDDLE_NAME'),
      patronBarcode: optionalEnv('RENEWAL_FINES_EMPLOYEE_NO_VOUCHER_SUBMIT_ONLY_PATRON_BARCODE'),
      patronAlephId: optionalEnv('RENEWAL_FINES_EMPLOYEE_NO_VOUCHER_SUBMIT_ONLY_PATRON_ALEPH_ID'),
    },
    employeeNoVoucherSubmitAndPay: {
      bankIdIdentity: optionalEnv('RENEWAL_FINES_EMPLOYEE_NO_VOUCHER_SUBMIT_AND_PAY_BANKID_IDENTITY'),
      middleName: optionalEnv('RENEWAL_FINES_EMPLOYEE_NO_VOUCHER_SUBMIT_AND_PAY_MIDDLE_NAME'),
      patronBarcode: optionalEnv('RENEWAL_FINES_EMPLOYEE_NO_VOUCHER_SUBMIT_AND_PAY_PATRON_BARCODE'),
      patronAlephId: optionalEnv('RENEWAL_FINES_EMPLOYEE_NO_VOUCHER_SUBMIT_AND_PAY_PATRON_ALEPH_ID'),
    },
  } as Record<RenewalFinesScenarioKey, RenewalFinesScenarioConfig>,
};

export function isRenewalFinesScenarioConfigured(key: RenewalFinesScenarioKey): boolean {
  const scenario = env.renewalFinesScenarios[key];
  return Boolean(scenario.bankIdIdentity && scenario.middleName && scenario.patronBarcode);
}
