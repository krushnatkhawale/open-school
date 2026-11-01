import { test, expect } from '@playwright/test';
import { goto, click, select } from './helpers/pacing';
import { seed } from './helpers/seed';

test.describe('Navigation — shared chrome across pages', () => {

  test.beforeEach(async ({ request }) => {
    await seed(request);
  });

  test('the root URL redirects to the classifications list', async ({ page }) => {
    await page.goto('/');

    await expect(page).toHaveURL(/\/classifications$/);
    await expect(page.locator('h1')).toHaveText('School Communications');
    await expect(page.locator('.nav-links a.active')).toHaveText('Classifications');
  });

  test('each page marks its own navigation link as current', async ({ page }) => {
    const pages = [
      { path: '/feed', link: 'Feed' },
      { path: '/classifications', link: 'Classifications' },
      { path: '/calendar', link: 'Calendar' },
      { path: '/extraction-rules', link: 'Extraction Rules' },
    ];

    for (const entry of pages) {
      await goto(page, entry.path);
      const active = page.locator('.nav-links a.active');
      await expect(active).toHaveCount(1);
      await expect(active).toHaveText(entry.link);
      await expect(active).toHaveAttribute('aria-current', 'page');
    }
  });

  test('every page shows the school picker with all schools plus the seeded schools', async ({ page }) => {
    for (const path of ['/feed', '/classifications', '/calendar', '/extraction-rules']) {
      await goto(page, path);
      const options = page.locator('#schoolSelect option');
      await expect(options).toHaveCount(3);
      await expect(options).toHaveText(['All schools', 'Green Valley School', 'Sunrise International']);
    }
  });

  test('the school picker filters classifications and keeps the page', async ({ page }) => {
    await goto(page, '/classifications');

    const schoolId = await page.locator('#schoolSelect option', { hasText: 'Sunrise International' })
      .getAttribute('value');
    await select(page, page.locator('#schoolSelect'), schoolId!);

    await page.waitForURL((url) => url.searchParams.get('schoolId') === schoolId);
    await expect(page).toHaveURL(/\/classifications\?schoolId=/);
    await expect(page.locator('#schoolSelect')).toHaveValue(schoolId);
    await expect(page.locator('table tbody tr')).toHaveCount(2);
    await expect(page.locator('table tbody')).not.toContainText('algebra');
  });

  test('mobile navigation opens, closes on outside click and navigates', async ({ page }) => {
    await page.setViewportSize({ width: 390, height: 844 });
    await goto(page, '/feed');

    const toggle = page.locator('#navToggle');
    const links = page.locator('#navLinks');
    await expect(toggle).toBeVisible();
    await expect(links).toBeHidden();
    await expect(toggle).toHaveAttribute('aria-expanded', 'false');

    await click(page, toggle);
    await expect(links).toBeVisible();
    await expect(toggle).toHaveAttribute('aria-expanded', 'true');

    await click(page, page.locator('.feed-stream-head h1'));
    await expect(links).toBeHidden();
    await expect(toggle).toHaveAttribute('aria-expanded', 'false');

    await click(page, toggle);
    await click(page, page.locator('#navLinks a', { hasText: 'Calendar' }));
    await expect(page).toHaveURL(/\/calendar$/);
    await expect(page.locator('h1')).toHaveText('Calendar');
  });
});
