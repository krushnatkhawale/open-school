import { test, expect } from '@playwright/test';
import { goto, click, injectCursor } from './helpers/pacing';

test.describe('Calendar page', () => {

  test.beforeAll(async ({ request }) => {
    const response = await request.post('/api/test/seed');
    expect(response.ok()).toBeTruthy();
    const body = await response.text();
    expect(body).toContain('Seeded');
  });

  test('shows month grid with seeded events highlighted', async ({ page }) => {
    await goto(page, '/calendar');
    await injectCursor(page);

    await expect(page.locator('h1')).toHaveText('Calendar');

    const cells = page.locator('a.calendar-day-link');
    const count = await cells.count();
    expect(count).toBeGreaterThan(20);

    const hasEvents = page.locator('a.calendar-day-link.has-classifications, a.calendar-day-link.has-scheduled, a.calendar-day-link.has-both');
    expect(await hasEvents.count()).toBeGreaterThanOrEqual(1);
  });

  test('clicking a day with classifications navigates to day detail', async ({ page }) => {
    await goto(page, '/calendar');
    await injectCursor(page);

    const dayLink = page.locator('a.calendar-day-link.has-classifications, a.calendar-day-link.has-both').first();
    await expect(dayLink).toBeVisible();

    const expectedDate = await dayLink.getAttribute('href');
    await click(page, dayLink);

    await expect(page).toHaveURL(/\/calendar\/day\?date=\d{4}-\d{2}-\d{2}/);
    await expect(page.locator('h1')).toHaveText('Activities');
  });

  test('day detail shows planned events section when events exist', async ({ page }) => {
    await goto(page, '/calendar');
    await injectCursor(page);

    const schedDay = page.locator(
      'a.calendar-day-link.has-scheduled, a.calendar-day-link.has-both'
    ).first();
    await expect(schedDay).toBeVisible();
    await click(page, schedDay);

    await expect(page).toHaveURL(/\/calendar\/day\?date=\d{4}-\d{2}-\d{2}/);
    const section = page.locator('.card-header:has-text("Planned Events")');
    await expect(section).toBeVisible();
  });

  test('calendar navigation works (previous / next month)', async ({ page }) => {
    await goto(page, '/calendar');
    await injectCursor(page);

    const currentText = await page.locator('.month-year').textContent();

    await click(page, page.locator('.calendar-nav a:has-text("Previous")'));
    const prevText = await page.locator('.month-year').textContent();
    expect(prevText).not.toBe(currentText);

    await click(page, page.locator('.calendar-nav a:has-text("Next")'));
    await click(page, page.locator('.calendar-nav a:has-text("Next")'));
    const nextText = await page.locator('.month-year').textContent();
    expect(nextText).not.toBe(currentText);
    expect(nextText).not.toBe(prevText);
  });

  test('legend is displayed', async ({ page }) => {
    await goto(page, '/calendar');
    await injectCursor(page);

    await expect(page.locator('.legend')).toBeVisible();
    await expect(page.locator('.legend')).toContainText('Classifications');
    await expect(page.locator('.legend')).toContainText('Planned events');
  });

  test('back link navigates to classifications page', async ({ page }) => {
    await goto(page, '/calendar');
    await injectCursor(page);

    await click(page, page.locator('a:has-text("Back to list")'));
    await expect(page).toHaveURL(/\/classifications/);
  });

});
