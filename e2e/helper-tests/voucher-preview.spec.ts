import { expect, test } from '@playwright/test';
import { validateVoucherPreview } from '../helpers/flows';

for (const fee of [150, 280]) {
  test(`shared senior preview check with ${fee} CZK tariff`, async ({ page }) => {
    await page.route('**/*', async route => {
      if (route.request().url().endsWith('/api/validate-voucher')) {
        await route.fulfill({ json: { valid: true, feeAmount: fee, discountAmount: 150, amountToPay: fee - 150 } });
      } else {
        await route.fulfill({ contentType: 'text/html', body: `
          <input id="voucherCode"><button id="btn-validate-voucher">Validate</button>
          <div id="voucher-result" hidden></div>
          <script>
            document.querySelector('button').onclick = async () => {
              const data = await (await fetch('/api/validate-voucher', {method: 'POST'})).json();
              const result = document.querySelector('#voucher-result');
              result.hidden = false;
              result.textContent = 'Voucher validated';
              result.dataset.valid = 'true';
              for (const field of ['feeAmount', 'discountAmount', 'amountToPay']) result.dataset[field] = data[field];
            };
          </script>` });
      }
    });
    await page.goto('https://registrator.example.test/form');
    const check = validateVoucherPreview(page, 'SENIOR', {
      feeAmount: 150, discountAmount: 150, amountToPay: 0,
    });
    if (fee === 150) await check;
    else await expect(check).rejects.toThrow('Voucher preview feeAmount');
  });
}
