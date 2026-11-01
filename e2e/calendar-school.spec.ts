import { test, expect } from '@playwright/test';
import { goto, click, select } from './helpers/pacing';
import { seed } from './helpers/seed';

const CLASS_DAYS = 'a.calendar-day-link.has-classifications, a.calendar-day-link.has-both';
const SCHEDULED_DAYS = 'a.calendar-day-link.has-scheduled, a.calendar-day-link.has-both';

test.describe('Calendar — school scoping and day detail', () => {

  test.beforeEach(async ({ request }) => {
    await seed(request);
  });

  test('school filter scopes highlighted days and stays in calendar navigation', async ({ page }) => {
    await goto(page, '/calendar');
    await expect(page.locator(CLASS_DAYS)).toHaveCount(3);

    const schoolId = await page.locator('#schoolSelect option', { hasText: 'Sunrise International' })
      .getAttribute('value');
    await select(page, page.locator('#schoolSelect'), schoolId!);
    await page.waitForURL((url) => url.searchParams.get('schoolId') === schoolId);

    await expect(page.locator('#schoolSelect')).toHaveValue(schoolId);
    await expect(page.locator(CLASS_DAYS)).toHaveCount(2);
    await expect(page.locator(SCHEDULED_DAYS)).toHaveCount(1);

    const previous = page.locator('.calendar-nav a', { hasText: 'Previous' });
    const next = page.locator('.calendar-nav a', { hasText: 'Next' });
    await expect(previous).toHaveAttribute('href', new RegExp(`schoolId=${schoolId}`));
    await expect(next).toHaveAttribute('href', new RegExp(`schoolId=${schoolId}`));
  });

  test('day detail shows only the selected school and keeps the school on the way back', async ({ page }) => {
    await goto(page, '/calendar');
    const schoolId = await page.locator('#schoolSelect option', { hasText: 'Sunrise International' })
      .getAttribute('value');
    await select(page, page.locator('#schoolSelect'), schoolId!);
    await page.waitForURL((url) => url.searchParams.get('schoolId') === schoolId);

    await click(page, page.locator(CLASS_DAYS).first());
    await expect(page).toHaveURL(/\/calendar\/day\?date=\d{4}-\d{2}-\d{2}/);
    await expect(page).toHaveURL(new RegExp(`schoolId=${schoolId}`));
    await expect(page.locator('h1')).toHaveText('Activities');

    const table = page.locator('table tbody');
    await expect(table).not.toContainText('algebra');
    await expect(table).not.toContainText('Welcome! What is the name of the school');
    await expect(table).toContainText(/Annual Science Exhibition|closed for Diwali/);

    const back = page.locator('.links a', { hasText: 'Back to calendar' });
    await expect(back).toHaveAttribute('href', new RegExp(`schoolId=${schoolId}`));
    await click(page, back);
    await expect(page).toHaveURL(new RegExp(`/calendar\\?schoolId=${schoolId}`));
    await expect(page.locator('#schoolSelect')).toHaveValue(schoolId);
  });

  test('planned events for the selected school appear on the day detail', async ({ page }) => {
    await goto(page, '/calendar');
    const schoolId = await page.locator('#schoolSelect option', { hasText: 'Sunrise International' })
      .getAttribute('value');
    await select(page, page.locator('#schoolSelect'), schoolId!);
    await page.waitForURL((url) => url.searchParams.get('schoolId') === schoolId);

    await click(page, page.locator(SCHEDULED_DAYS).first());
    await expect(page).toHaveURL(/\/calendar\/day\?date=/);

    const planned = page.locator('.card', { hasText: 'Planned Events' });
    await expect(planned).toBeVisible();
    await expect(planned).toContainText('Parent-Teacher Meeting');
    await expect(planned).toContainText('PARENT_TEACHER_MEETING');
    await expect(planned).not.toContainText('Diwali Break');
    await expect(planned).not.toContainText('Mid-term Examinations');
  });

  test('a day with no activity shows the empty state', async ({ page }) => {
    await goto(page, '/calendar/day?date=2030-01-01');

    await expect(page.locator('h1')).toHaveText('Activities');
    await expect(page.locator('.empty-state')).toContainText('No activities on this day.');
    await click(page, page.locator('.links a', { hasText: 'Back to calendar' }));
    await expect(page).toHaveURL(/\/calendar(\?|$)/);
  });

  test('an unknown school id is ignored and the calendar still renders', async ({ page }) => {
    const response = await page.goto('/calendar?schoolId=00000000-0000-0000-0000-000000000000');
    expect(response?.status()).toBe(200);

    await expect(page.locator('#schoolSelect')).toHaveValue('');
    expect(await page.locator('a.calendar-day-link').count()).toBeGreaterThan(20);
  });
});
