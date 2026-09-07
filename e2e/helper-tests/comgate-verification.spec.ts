import { expect, test } from '@playwright/test';
import { confirmComgateSandboxPayment } from '../helpers/flows';
import { ScreenshotHelper } from '../helpers/screenshots';
import { env } from '../helpers/env';

for (const mode of ['redirect', 'iframe', 'continue-popup', 'continue-redirect']) {
  test(`Comgate verification: ${mode}`, async ({ page }) => {
    let confirmations = 0;
    await page.context().route('**/*', async route => {
      const url = new URL(route.request().url());
      const callback = `${env.contextPath}/payment/callback?status=success`;
      if (url.pathname.includes('/payment/callback')) {
        confirmations++;
        await route.fulfill({ contentType: 'text/html', body: '<h1>Registration success</h1>' });
      } else if (url.pathname === '/provider/testing/display/test') {
        const destination = mode === 'continue-popup' ? 'window.opener.location' : 'window.top.location';
        await route.fulfill({ contentType: 'text/html', body:
          `<button onclick="${destination}.href='${callback}'">Confirm</button>` });
      } else {
        const verification = '/provider/testing/display/test';
        const start = mode === 'redirect' ? `location.href='${verification}'`
          : mode === 'iframe' ? `document.querySelector('iframe').src='${verification}'`
          : `document.querySelector('section').hidden=false`;
        const proceed = mode === 'continue-popup' ? `window.open('${verification}')`
          : `location.href='${verification}'`;
        await route.fulfill({ contentType: 'text/html', body: `
          <button onclick="${start}">Pay 75 CZK</button>
          <iframe></iframe>
          <section hidden><p>Can't see the payment verification page? Click below.</p>
          <button onclick="${proceed}">Continue</button></section>` });
      }
    });
    await page.goto('https://gateway.example.test/start');
    const screenshots = { take: async () => {} } as unknown as ScreenshotHelper;
    await confirmComgateSandboxPayment(page, screenshots,
      () => page.getByRole('button', { name: 'Pay 75 CZK' }).click());
    await expect(page.getByRole('heading', { name: 'Registration success' })).toBeVisible();
    expect(confirmations).toBe(1);
  });
}
