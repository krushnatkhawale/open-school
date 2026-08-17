import { test, expect } from '@playwright/test';
import { goto, click, press, injectCursor } from './helpers/pacing';

test.describe('Extraction rules — full prompt modal', () => {

  test.use({ viewport: { width: 1440, height: 900 } });

  test.beforeEach(async ({ page }) => {
    await goto(page, '/extraction-rules');
    await injectCursor(page);
  });

  test('rail button opens modal with a single full-prompt textarea', async ({ page }) => {
    const modal = page.locator('#erPromptModal');

    await expect(modal).toHaveAttribute('aria-hidden', 'true');

    await click(page, page.locator('[data-er-modal-open]'));

    await expect(modal).toHaveClass(/open/);
    await expect(modal).toHaveAttribute('aria-hidden', 'false');
    await expect(page.locator('#erModalTitle')).toHaveText(/Full prompt — all fields/);

    const field = page.locator('#erModalPrompt');
    await expect(field).toBeVisible();
    await expect(field).toHaveAttribute('readonly', '');
    await expect(page.locator('#erPromptModal textarea')).toHaveCount(1);
    for (const label of ['Classification', 'Image description', 'Lesson date', 'Scheduled events', 'Tags']) {
      await expect(field).toContainText(`=== ${label} ===`);
    }
    await expect(field).toContainText('school communication classifier');
  });

  test('modal closes with escape', async ({ page }) => {
    const modal = page.locator('#erPromptModal');
    await click(page, page.locator('[data-er-modal-open]'));
    await expect(modal).toHaveClass(/open/);

    await press(page, 'Escape');

    await expect(modal).not.toHaveClass(/open/);
    await expect(modal).toHaveAttribute('aria-hidden', 'true');
  });

  test('copy button confirms a copy', async ({ page }) => {
    await click(page, page.locator('[data-er-modal-open]'));

    const copyBtn = page.locator('[data-er-copy-all]');
    await click(page, copyBtn);

    await expect(copyBtn).toHaveClass(/copied/);
    await expect(copyBtn).toHaveText('Copied');
  });
});
