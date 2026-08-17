import { test, expect } from '@playwright/test';
import { goto, click, fill, select, press, injectCursor } from './helpers/pacing';

test.describe('Classifications edit drawer', () => {

  test.use({ viewport: { width: 1440, height: 900 } });

  test.beforeAll(async ({ request }) => {
    const response = await request.post('/api/test/seed');
    expect(response.ok()).toBeTruthy();
  });

  test('opens a ~60% right-side drawer and saves edits', async ({ page }) => {
    await goto(page, '/classifications');
    await injectCursor(page);

    const firstRow = page.locator('table tbody tr').first();
    await click(page, firstRow.locator('.js-edit'));

    const drawer = page.locator('#editDrawer');
    await expect(drawer).toBeVisible();

    const panel = drawer.locator('.drawer-panel');
    await expect(panel).toBeVisible();
    await page.waitForTimeout(400);

    const viewportWidth = page.viewportSize().width;
    const panelBox = await panel.boundingBox();
    expect(panelBox.x + panelBox.width).toBeCloseTo(viewportWidth, 0);
    expect(panelBox.width / viewportWidth).toBeGreaterThan(0.5);
    expect(panelBox.width / viewportWidth).toBeLessThanOrEqual(0.65);

    await select(page, drawer.locator('#editType'), 'CIRCULAR_NOTICE');
    await fill(page, drawer.locator('#editText'), 'Edited via e2e');
    await fill(page, drawer.locator('#editTags'), 'e2e, test, e2e');
    await click(page, drawer.locator('button[type="submit"]'));

    await expect(drawer).toBeHidden();
    await expect(firstRow).toContainText('Edited via e2e');
  });

  test('closes on backdrop click', async ({ page }) => {
    await goto(page, '/classifications');
    await injectCursor(page);

    await click(page, page.locator('table tbody tr').first().locator('.js-edit'));

    const drawer = page.locator('#editDrawer');
    await expect(drawer).toBeVisible();

    await click(page, page.locator('#editDrawer .drawer-backdrop'), { position: { x: 5, y: 5 } });
    await expect(drawer).toBeHidden();
  });

  test('closes on Escape key', async ({ page }) => {
    await goto(page, '/classifications');
    await injectCursor(page);

    await click(page, page.locator('table tbody tr').first().locator('.js-edit'));

    const drawer = page.locator('#editDrawer');
    await expect(drawer).toBeVisible();

    await press(page, 'Escape');
    await expect(drawer).toBeHidden();
  });

});
