import { test, expect, type Page } from '@playwright/test';
import { injectCursor, goto, click, glide, pause } from './helpers/pacing';

async function animateTextareaScroll(page: Page, frac: number, duration = 950): Promise<void> {
  await page.evaluate(({ frac, duration }) => {
    const el = document.getElementById('erModalPrompt') as HTMLTextAreaElement;
    const max = el.scrollHeight - el.clientHeight;
    const start = el.scrollTop;
    const target = max * frac;
    const t0 = performance.now();
    const ease = (t: number) => (t < 0.5 ? 2 * t * t : 1 - Math.pow(-2 * t + 2, 2) / 2);
    return new Promise<void>((resolve) => {
      const step = (now: number) => {
        const p = Math.min(1, (now - t0) / duration);
        el.scrollTop = start + (target - start) * ease(p);
        if (p < 1) requestAnimationFrame(step);
        else resolve();
      };
      requestAnimationFrame(step);
    });
  }, { frac, duration });
}

test.describe('Full prompt modal — demo walkthrough', () => {

  test.use({ viewport: { width: 1440, height: 900 } });

  test('walks the full-prompt functionality end to end', async ({ page }) => {
    await goto(page, '/extraction-rules', 700);
    const openBtn = page.locator('[data-er-modal-open]');
    await expect(openBtn).toBeVisible();
    await expect(openBtn).toHaveText(/View Full prompt/);
    await injectCursor(page);
    await pause(page, 700);

    await glide(page, page.locator('.er-rail-item').nth(0));
    await pause(page, 450);
    await glide(page, page.locator('.er-rail-item').nth(1));
    await pause(page, 400);

    await click(page, openBtn);
    const modal = page.locator('#erPromptModal');
    await expect(modal).toHaveClass(/open/);
    await expect(modal).toHaveAttribute('aria-hidden', 'false');
    await expect(page.locator('#erModalTitle')).toHaveText(/Full prompt — all fields/);
    await pause(page, 800);

    const field = page.locator('#erModalPrompt');
    await expect(field).toBeVisible();
    await expect(field).toHaveAttribute('readonly', '');
    await expect(page.locator('#erPromptModal textarea')).toHaveCount(1);
    for (const label of ['Classification', 'Image description', 'Lesson date', 'Scheduled events', 'Tags']) {
      await expect(field).toContainText(`=== ${label} ===`);
    }
    await expect(field).toContainText('school communication classifier');
    await pause(page, 500);

    await glide(page, field);
    await pause(page, 400);

    await animateTextareaScroll(page, 0.6);
    await pause(page, 350);
    await animateTextareaScroll(page, 0);
    await pause(page, 400);

    await click(page, page.locator('[data-er-copy-all]'));
    const copyBtn = page.locator('[data-er-copy-all]');
    await expect(copyBtn).toHaveText('Copied');
    await pause(page, 1000);

    await glide(page, page.locator('.er-modal-close'));
    await pause(page, 300);
    await page.keyboard.press('Escape');
    await expect(modal).not.toHaveClass(/open/);
    await expect(modal).toHaveAttribute('aria-hidden', 'true');
    await pause(page, 600);
  });
});
