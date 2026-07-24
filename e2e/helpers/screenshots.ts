import { Page } from '@playwright/test';
import * as path from 'path';
import { env } from './env';

type ScreenshotOptions = {
  enableHeaderAbsoluteHack?: boolean;
  fullPage?: boolean;
};

/**
 * Manages sequential, full-page screenshots in a timestamped directory.
 *
 * Each scenario gets its own subdirectory under the configured screenshot dir:
 *   screenshots/registration/normal-no-discount/202604151639/
 *
 * Screenshots are numbered sequentially across all pages (user + admin),
 * giving a clear chronological narrative when sorted alphabetically.
 *
 * Usage:
 *   const ss = new ScreenshotHelper('registration/normal-no-discount');
 *   await ss.take(page, 'welcome-page');                     // → 01-welcome-page.png
 *   await ss.take(adminPage, 'identity-detail', 'admin');    // → 02-admin-identity-detail.png
 */
export class ScreenshotHelper {
  private counter = 0;
  readonly dir: string;

  constructor(scenarioPath: string) {
    const now = new Date();
    const ts = [
      now.getFullYear(),
      String(now.getMonth() + 1).padStart(2, '0'),
      String(now.getDate()).padStart(2, '0'),
      String(now.getHours()).padStart(2, '0'),
      String(now.getMinutes()).padStart(2, '0'),
    ].join('');
    this.dir = path.join(env.screenshotDir, scenarioPath, ts);
  }

  async take(
    page: Page,
    description: string,
    prefix?: string,
    options?: ScreenshotOptions
  ): Promise<void> {
    this.counter++;
    const num = String(this.counter).padStart(2, '0');
    const filename = prefix
      ? `${num}-${prefix}-${description}.png`
      : `${num}-${description}.png`;

    // Convert known fixed/sticky elements to absolute so they render correctly in fullPage screenshots
    const isThisApp = page.url().includes(env.contextPath);
    const enableHeaderAbsoluteHack = options?.enableHeaderAbsoluteHack ?? true;

    if (isThisApp) {
      await page.evaluate((shouldApplyHeaderHack) => {
        const fixMap: Record<string, string> = {
          '#session-timer-bar': 'relative',
        };
        if (shouldApplyHeaderHack) {
          fixMap['header'] = 'absolute';
        }
        for (const [sel, newPos] of Object.entries(fixMap)) {
          const el = document.querySelector<HTMLElement>(sel);
          if (el) {
            el.dataset.pwOrigPos = getComputedStyle(el).position;
            el.style.setProperty('position', newPos, 'important');
          }
        }
      }, enableHeaderAbsoluteHack);
    }

    await page.screenshot({
      path: path.join(this.dir, filename),
      fullPage: options?.fullPage ?? true,
    });

    if (isThisApp) {
      try {
        await page.evaluate(() => {
          document.querySelectorAll<HTMLElement>('[data-pw-orig-pos]').forEach((el) => {
            el.style.setProperty('position', el.dataset.pwOrigPos!);
            delete el.dataset.pwOrigPos;
          });
        });
      } catch {
        // Page navigated away — no cleanup needed.
      }
    }
  }

  async takeRaw(
    page: Page,
    description: string,
    prefix?: string,
    options?: ScreenshotOptions
  ): Promise<void> {
    this.counter++;
    const num = String(this.counter).padStart(2, '0');
    const filename = prefix
      ? `${num}-${prefix}-${description}.png`
      : `${num}-${description}.png`;

    await page.screenshot({
      path: path.join(this.dir, filename),
      fullPage: options?.fullPage ?? true,
    });
  }
}
