import { test, expect } from '@playwright/test';
import { goto, click, fill, select, press } from './helpers/pacing';
import { seed } from './helpers/seed';

const ONBOARDING_TEXT = 'Welcome! What is the name of the school you represent?';

test.describe('Feed — filters, search and navigation', () => {

  test.use({ viewport: { width: 1440, height: 900 } });

  test.beforeEach(async ({ request }) => {
    await seed(request);
  });

  test('notices strip lists upcoming events and marks only actionable events', async ({ page }) => {
    await goto(page, '/feed');

    const notices = page.locator('.feed-notices-list .feed-notice');
    await expect(notices).toHaveCount(3);

    const parentMeeting = page.locator('.feed-notice', { hasText: 'Parent-Teacher Meeting' });
    await expect(parentMeeting).toContainText('PARENT_TEACHER_MEETING');
    await expect(parentMeeting).toContainText('Action needed');
    await expect(parentMeeting).toHaveClass(/action-needed/);

    const holiday = page.locator('.feed-notice', { hasText: 'Diwali Break' });
    await expect(holiday).toContainText('HOLIDAY');
    await expect(holiday).not.toContainText('Action needed');
    await expect(holiday).not.toHaveClass(/action-needed/);

    const exams = page.locator('.feed-notice', { hasText: 'Mid-term Examinations' });
    await expect(exams).toContainText('EXAM');
    await expect(exams).toContainText('Action needed');

    await click(page, page.locator('.feed-notices-link'));
    await expect(page).toHaveURL(/\/calendar/);
  });

  test('type filter narrows the feed and keeps the expanded type panel open', async ({ page }) => {
    await goto(page, '/feed');

    const checks = page.locator('#typeChecks');
    const toggle = page.locator('#typeChecksToggle');
    await expect(checks).toHaveClass(/collapsed/);
    await expect(toggle).toHaveText('More types');
    await expect(toggle).toHaveAttribute('aria-expanded', 'false');

    await click(page, toggle);
    await expect(checks).not.toHaveClass(/collapsed/);
    await expect(toggle).toHaveText('Less');
    await expect(toggle).toHaveAttribute('aria-expanded', 'true');

    await click(page, page.locator('#typeChecks input[value="REMINDER"]'));
    await expect(page.locator('#typeChecks input[value="REMINDER"]')).toBeChecked();
    await click(page, page.locator('.feed-filters button[type="submit"]'));

    await expect(page).toHaveURL(/types=REMINDER/);
    await expect(page.locator('.feed-card')).toHaveCount(1);
    await expect(page.locator('.feed-card')).toContainText('Parent-teacher meeting scheduled');
    await expect(checks).not.toHaveClass(/collapsed/);
    await expect(page.locator('#typeChecks input[value="REMINDER"]')).toBeChecked();

    await click(page, page.locator('.feed-filters a.clear-link'));
    await expect(page).toHaveURL(/\/feed$/);
    await expect(page.locator('.feed-card')).toHaveCount(4);
  });

  test('chat ID filter shows only one school chat', async ({ page }) => {
    await goto(page, '/feed');

    await fill(page, page.locator('.feed-filters input[name="chatId"]'), '67890');
    await click(page, page.locator('.feed-filters button[type="submit"]'));

    await expect(page).toHaveURL(/chatId=67890/);
    await expect(page.locator('.feed-card')).toHaveCount(2);
    await expect(page.locator('.feed-card', { hasText: 'closed for Diwali' })).toBeVisible();
    await expect(page.locator('.feed-card', { hasText: 'Annual Science Exhibition' })).toBeVisible();
    await expect(page.locator('.feed-card', { hasText: 'algebra' })).toHaveCount(0);
  });

  test('search shows the result count, highlights the match and clears', async ({ page }) => {
    await goto(page, '/feed');

    const search = page.locator('#feedSearchInput');
    await fill(page, search, 'algebra');
    await page.waitForURL((url) => url.searchParams.get('q') === 'algebra');

    await expect(page.locator('.feed-search-note')).toContainText('1 result for "algebra"');
    await expect(page.locator('.feed-card')).toHaveCount(1);
    await expect(page.locator('.feed-card mark')).toHaveText(/algebra/i);

    await expect(page.locator('#feedSearchClear')).toBeVisible();
    await click(page, page.locator('#feedSearchClear'));
    await expect(page).toHaveURL(/\/feed\?q=$/);
    await expect(page.locator('.feed-card')).toHaveCount(4);
  });

  test('tag chips filter the feed and can be removed', async ({ page }) => {
    await goto(page, '/feed');

    const hindiTag = page.locator('.feed-card-tag', { hasText: 'hindi' }).first();
    await expect(hindiTag).toBeVisible();
    await click(page, hindiTag);

    await expect(page).toHaveURL(/tag=hindi/);
    await expect(page.locator('.feed-card')).toHaveCount(1);
    await expect(page.locator('.feed-card')).toContainText('Parent-teacher meeting scheduled');
    await expect(page.locator('.active-tag .tag-chip')).toHaveText('hindi');

    await click(page, page.locator('.tag-chip-remove'));
    await expect(page).not.toHaveURL(/tag=/);
    await expect(page.locator('.feed-card')).toHaveCount(4);
  });

  test('tag links preserve the current search and type filters', async ({ page }) => {
    await goto(page, '/feed?q=parent-teacher&types=REMINDER');

    const hindiTag = page.locator('.feed-card-tag', { hasText: 'hindi' }).first();
    const href = await hindiTag.getAttribute('href');
    expect(href).toContain('q=parent-teacher');
    expect(href).toContain('types=REMINDER');

    await click(page, hindiTag);
    const url = new URL(page.url());
    expect(url.searchParams.get('tag')).toBe('hindi');
    expect(url.searchParams.get('q')).toBe('parent-teacher');
    expect(url.searchParams.get('types')).toBe('REMINDER');
    await expect(page.locator('.feed-card')).toHaveCount(1);
  });

  test('search with no matches shows an empty state with a way back', async ({ page }) => {
    await goto(page, '/feed?q=zzz-no-match');

    const empty = page.locator('.empty-state');
    await expect(empty).toContainText('No results for "zzz-no-match".');
    await click(page, empty.locator('a.clear-link'));

    await expect(page).toHaveURL(/\/feed$/);
    await expect(page.locator('.feed-card')).toHaveCount(4);
  });

  test('school filter scopes both the feed cards and the notices strip', async ({ page }) => {
    await goto(page, '/feed');

    const schoolId = await page.locator('#schoolSelect option', { hasText: 'Sunrise International' })
      .getAttribute('value');
    await select(page, page.locator('#schoolSelect'), schoolId!);

    await page.waitForURL((url) => url.searchParams.get('schoolId') === schoolId);
    await expect(page.locator('.feed-card')).toHaveCount(2);
    await expect(page.locator('.feed-card', { hasText: 'algebra' })).toHaveCount(0);
    await expect(page.locator('.feed-card', { hasText: ONBOARDING_TEXT })).toHaveCount(0);
    await expect(page.locator('.feed-notice')).toHaveCount(1);
    await expect(page.locator('.feed-notice')).toContainText('Parent-Teacher Meeting');
  });

  test('an unknown school id is ignored instead of breaking the feed', async ({ page }) => {
    const response = await page.goto('/feed?schoolId=00000000-0000-0000-0000-000000000000');
    expect(response?.status()).toBe(200);

    await expect(page.locator('#schoolSelect')).toHaveValue('');
    await expect(page.locator('.feed-card')).toHaveCount(4);
  });

  test('academic year meter jumps to a month and reports scroll progress', async ({ page }) => {
    await goto(page, '/feed');

    const months = page.locator('.meter-month');
    const count = await months.count();
    expect(count).toBeGreaterThanOrEqual(8);
    await expect(months.first()).toHaveClass(/current/);
    await expect(months.first()).toHaveClass(/active/);

    await page.evaluate(() => window.scrollTo(0, document.documentElement.scrollHeight));
    await expect
      .poll(async () => page.locator('#feedMeterProgress')
        .evaluate((el) => parseFloat(getComputedStyle(el).width)))
      .toBeGreaterThan(0);

    await click(page, months.first(), 800);
    await expect(months.first()).toHaveClass(/active/);
    await expect(page.locator('.feed-month-section').first()).toBeInViewport();
  });

  test('feed image opens a lightbox with its date and closes on Escape', async ({ page }) => {
    await goto(page, '/feed');

    const thumb = page.locator('.feed-card-media img.js-lightbox').first();
    await expect(thumb).toBeVisible();
    const date = await thumb.getAttribute('data-date');
    expect(date).toBeTruthy();

    await click(page, thumb);
    const lightbox = page.locator('#lightbox');
    await expect(lightbox).toBeVisible();
    await expect(lightbox.locator('.lightbox-image')).toHaveAttribute(
      'src', /\/api\/classifications\/[^/]+\/image/);
    await expect(lightbox.locator('.lightbox-meta')).toContainText(date!);

    await press(page, 'Escape');
    await expect(lightbox).toBeHidden();
  });
});
