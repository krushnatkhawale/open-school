import { test, expect } from '@playwright/test';
import { goto, click, fill, press } from './helpers/pacing';
import { resetExtractionRules } from './helpers/seed';

const CUSTOM_RULE = 'Custom classification rule written by the e2e suite.';

test.describe('Extraction rules — editing and enablement', () => {

  test.use({ viewport: { width: 1440, height: 900 } });

  test.beforeEach(async ({ request }) => {
    await resetExtractionRules(request);
  });

  test.afterEach(async ({ request }) => {
    await resetExtractionRules(request);
  });

  test('the rail lists all five fields and scroll-spies to the selected field', async ({ page }) => {
    await goto(page, '/extraction-rules');

    await expect(page.locator('.er-rail-item')).toHaveCount(5);
    await expect(page.locator('.er-rail-label')).toHaveText([
      'Classification',
      'Image description',
      'Lesson date',
      'Scheduled events',
      'Tags',
    ]);

    const tagsItem = page.locator('.er-rail-item[data-spykey="tags"]');
    await click(page, tagsItem);
    await expect(tagsItem).toHaveClass(/active/);
    await expect(page.locator('#tags')).toBeInViewport();
  });

  test('a saved rule shows confirmation, updates the full prompt and survives reload', async ({ page }) => {
    await goto(page, '/extraction-rules');

    const section = page.locator('#classification');
    await expect(section.locator('.badge-default')).toBeVisible();
    await expect(section.locator('.er-status')).toHaveText('enabled');

    await fill(page, section.locator('#rules-classification'), CUSTOM_RULE);
    await click(page, section.locator('#er-save-classification button[type="submit"]'));

    await expect(page.locator('.flash-success')).toContainText('Saved Classification.');
    await expect(section.locator('.badge-default')).toHaveCount(0);
    await expect(section.locator('.er-status')).toHaveText('enabled');
    await expect(section.locator('#rules-classification')).toHaveValue(CUSTOM_RULE);

    await click(page, page.locator('[data-er-modal-open]'));
    await expect(page.locator('#erModalPrompt')).toContainText(CUSTOM_RULE);
    await press(page, 'Escape');

    await goto(page, '/extraction-rules');
    await expect(page.locator('#rules-classification')).toHaveValue(CUSTOM_RULE);
  });

  test('disabling a field falls back to the default prompt and the choice persists', async ({ page }) => {
    await goto(page, '/extraction-rules');

    const section = page.locator('#classification');
    const toggle = section.locator('input[name="enabled"]');
    const switchTrack = section.locator('.er-switch');
    const defaultRules = (await section.locator('#rules-classification').inputValue()).trimEnd();
    await fill(page, section.locator('#rules-classification'), CUSTOM_RULE);
    await click(page, switchTrack);
    await expect(toggle).not.toBeChecked();
    await click(page, section.locator('#er-save-classification button[type="submit"]'));

    await expect(page.locator('.flash-success')).toContainText('Saved Classification.');
    await expect(section.locator('.er-status')).toHaveText('disabled');
    await expect(section.locator('.badge-default')).toBeVisible();
    await expect(page.locator('.er-rail-item[data-spykey="classification"] .er-rail-dot')).toHaveClass(/off/);
    await expect(section.locator('#rules-classification')).not.toHaveValue(CUSTOM_RULE);

    await goto(page, '/extraction-rules');
    await expect(page.locator('#classification .er-status')).toHaveText('disabled');

    await click(page, page.locator('#classification .er-switch'));
    await click(page, page.locator('#classification #er-save-classification button[type="submit"]'));
    await expect(page.locator('#classification .er-status')).toHaveText('enabled');
    await expect(page.locator('#classification #rules-classification')).toHaveValue(defaultRules);
  });

  test('an enabled field cannot be saved with blank rules', async ({ page }) => {
    await goto(page, '/extraction-rules');

    const section = page.locator('#classification');
    await fill(page, section.locator('#rules-classification'), '');
    await click(page, section.locator('#er-save-classification button[type="submit"]'));

    await expect(page.locator('.flash-error')).toContainText(
      'Rules cannot be blank while the field is enabled.');
    await expect(page.locator('#classification .badge-default')).toBeVisible();
    await expect(page.locator('#classification .er-status')).toHaveText('enabled');
    await expect(page.locator('#rules-classification')).not.toHaveValue('');
  });

  test('reset restores the built-in default and clears the custom rule', async ({ page }) => {
    await goto(page, '/extraction-rules');

    const section = page.locator('#classification');
    await fill(page, section.locator('#rules-classification'), CUSTOM_RULE);
    await click(page, section.locator('#er-save-classification button[type="submit"]'));
    await expect(section.locator('#rules-classification')).toHaveValue(CUSTOM_RULE);

    await click(page, section.locator('.er-reset-btn'));
    await expect(page.locator('.flash-success')).toContainText('Reset Classification to default.');
    await expect(page.locator('#classification .badge-default')).toBeVisible();
    await expect(page.locator('#rules-classification')).not.toHaveValue(CUSTOM_RULE);

    await goto(page, '/extraction-rules');
    await expect(page.locator('#classification .badge-default')).toBeVisible();
    await expect(page.locator('#rules-classification')).not.toHaveValue(CUSTOM_RULE);
  });
});
