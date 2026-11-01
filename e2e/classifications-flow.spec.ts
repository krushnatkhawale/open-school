import { test, expect } from '@playwright/test';
import { goto, click, fill, select, press } from './helpers/pacing';
import { seed } from './helpers/seed';

const ONBOARDING_TEXT = 'Welcome! What is the name of the school you represent?';

test.describe('Classifications — list, filters and editing', () => {

  test.use({ viewport: { width: 1440, height: 900 } });

  test.beforeEach(async ({ request }) => {
    await seed(request);
  });

  test('list shows all visible communications and hides onboarding prompts', async ({ page }) => {
    await goto(page, '/classifications');

    await expect(page.locator('table tbody tr')).toHaveCount(4);
    await expect(page.locator('.record-count')).toContainText('4 record(s)');
    await expect(page.locator('body')).not.toContainText(ONBOARDING_TEXT);
  });

  test('type filter keeps only that communication type', async ({ page }) => {
    await goto(page, '/classifications');

    const typeSelect = page.locator('.filters select[name="type"]');
    await select(page, typeSelect, 'REMINDER');
    await click(page, page.locator('.filters button[type="submit"]'));

    await expect(page).toHaveURL(/type=REMINDER/);
    await expect(typeSelect).toHaveValue('REMINDER');
    await expect(page.locator('table tbody tr')).toHaveCount(1);
    await expect(page.locator('table tbody tr .badge')).toHaveText('REMINDER');
    await expect(page.locator('table tbody tr .img-link')).toBeVisible();

    await click(page, page.locator('.filters a.clear-link'));
    await expect(page).toHaveURL(/\/classifications$/);
    await expect(page.locator('table tbody tr')).toHaveCount(4);
  });

  test('chat ID filter keeps only that school chat', async ({ page }) => {
    await goto(page, '/classifications');

    await fill(page, page.locator('.filters input[name="chatId"]'), '67890');
    await click(page, page.locator('.filters button[type="submit"]'));

    await expect(page).toHaveURL(/chatId=67890/);
    await expect(page.locator('table tbody tr')).toHaveCount(2);
    await expect(page.locator('table tbody tr', { hasText: 'closed for Diwali' })).toBeVisible();
    await expect(page.locator('table tbody tr', { hasText: 'Annual Science Exhibition' })).toBeVisible();
    await expect(page.locator('table tbody tr', { hasText: 'algebra' })).toHaveCount(0);
  });

  test('a filter combination with no matches shows the empty state', async ({ page }) => {
    await goto(page, '/classifications?chatId=99999');

    await expect(page.locator('table tbody .js-edit')).toHaveCount(0);
    await expect(page.locator('table tbody tr')).toHaveCount(1);
    await expect(page.locator('.empty-state')).toContainText('No classifications yet');
  });

  test('cancel closes the editor without changing the record', async ({ page }) => {
    await goto(page, '/classifications');

    const firstRow = page.locator('table tbody tr').first();
    const originalText = await firstRow.locator('.cell-text').textContent();
    await click(page, firstRow.locator('.js-edit'));

    const drawer = page.locator('#editDrawer');
    await expect(drawer).toBeVisible();
    await fill(page, drawer.locator('#editText'), 'This text must not be saved');
    await click(page, drawer.locator('#editCancel'));

    await expect(drawer).toBeHidden();
    await expect(firstRow).toContainText(originalText!);
    await expect(firstRow).not.toContainText('This text must not be saved');
  });

  test('invalid edits keep the editor open and explain the failure', async ({ page }) => {
    await goto(page, '/classifications');

    await click(page, page.locator('table tbody tr').first().locator('.js-edit'));
    const drawer = page.locator('#editDrawer');
    await expect(drawer).toBeVisible();

    await fill(page, drawer.locator('#editDate'), '');
    await click(page, drawer.locator('button[type="submit"]'));

    const error = drawer.locator('#editError');
    await expect(error).toBeVisible();
    await expect(error).toContainText('Save failed');
    await expect(drawer).toBeVisible();
  });

  test('saved edits normalize tags and persist after reload', async ({ page }) => {
    await goto(page, '/classifications');

    const editButton = page.locator('table tbody tr').first().locator('.js-edit');
    const id = await editButton.getAttribute('data-id');
    await click(page, editButton);

    const drawer = page.locator('#editDrawer');
    await fill(page, drawer.locator('#editText'), 'Edited tags via e2e');
    await fill(page, drawer.locator('#editTags'), '  test , e2e , test  ');
    await click(page, drawer.locator('button[type="submit"]'));

    await expect(drawer).toBeHidden();
    await expect(page.locator('table tbody tr').first()).toContainText('Edited tags via e2e');

    const response = await page.request.get(`/api/classifications/${id}`);
    expect(response.status()).toBe(200);
    expect((await response.json()).tags).toBe('test,e2e');

    await click(page, page.locator(`.js-edit[data-id="${id}"]`));
    await expect(drawer.locator('#editTags')).toHaveValue('test,e2e');
    await press(page, 'Escape');
    await expect(drawer).toBeHidden();
  });

  test('image thumbnail opens a lightbox and the close button dismisses it', async ({ page }) => {
    await goto(page, '/classifications');

    const thumb = page.locator('table img.js-lightbox').first();
    await expect(thumb).toBeVisible();
    await click(page, thumb);

    const lightbox = page.locator('#lightbox');
    await expect(lightbox).toBeVisible();
    await expect(lightbox.locator('.lightbox-image')).toHaveAttribute(
      'src', /\/api\/classifications\/[^/]+\/image/);

    await click(page, lightbox.locator('.lightbox-close'));
    await expect(lightbox).toBeHidden();
  });
});
