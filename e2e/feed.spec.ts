import { test, expect } from '@playwright/test';
import { goto, click, select, injectCursor } from './helpers/pacing';

test.describe('Feed page', () => {

  test.use({ viewport: { width: 1440, height: 900 } });

  test.beforeAll(async ({ request }) => {
    const response = await request.post('/api/test/seed');
    expect(response.ok()).toBeTruthy();
  });

  test('long image-card text clamps and expands on "more…"', async ({ page }) => {
    await goto(page, '/feed');
    await injectCursor(page);

    const card = page.locator('.feed-card', { hasText: 'Parent-teacher meeting scheduled for tomorrow at 5 PM' }).first();
    const text = card.locator('.feed-card-media-text');
    const btn = card.locator('.feed-card-more');

    await expect(text).toHaveClass(/clamped/);
    await expect(btn).toBeVisible();

    const collapsedHeight = (await text.boundingBox()).height;
    await click(page, btn);

    await expect(text).not.toHaveClass(/clamped/);
    await expect(btn).toHaveText('less');
    const expandedHeight = (await text.boundingBox()).height;
    expect(expandedHeight).toBeGreaterThan(collapsedHeight);

    await click(page, btn);
    await expect(text).toHaveClass(/clamped/);
    await expect(btn).toHaveText('more…');
  });

  test('long text-only card clamps and expands', async ({ page }) => {
    await goto(page, '/feed');
    await injectCursor(page);

    const card = page.locator('.feed-card', { hasText: 'Annual Science Exhibition will be held' }).first();
    const text = card.locator('.feed-card-text');
    const btn = card.locator('.feed-card-more');

    await expect(text).toHaveClass(/clamped/);
    await expect(btn).toBeVisible();

    await click(page, btn);
    await expect(text).not.toHaveClass(/clamped/);
    await expect(btn).toHaveText('less');
  });

  test('short text card shows no "more…" and is not clamped', async ({ page }) => {
    await goto(page, '/feed');
    await injectCursor(page);

    const card = page.locator('.feed-card', { hasText: 'Today we learned algebra' }).first();
    const text = card.locator('.feed-card-text');
    const btn = card.locator('.feed-card-more');

    await expect(text).not.toHaveClass(/clamped/);
    await expect(btn).toBeHidden();
  });

  test('school dropdown filters feed to that school', async ({ page }) => {
    await goto(page, '/feed');
    await injectCursor(page);

    await expect(page.locator('#schoolSelect')).toBeVisible();
    await expect(page.locator('#schoolSelect option')).toHaveText([
      'All schools',
      'Green Valley School',
      'Sunrise International',
    ]);

    const selectBox = page.locator('#schoolSelect');
    const schoolId = await selectBox.locator('option', { hasText: 'Sunrise International' }).getAttribute('value');
    await select(page, selectBox, schoolId!);
    await page.waitForURL((url) => url.searchParams.has('schoolId'));

    await expect(page.locator('.feed-card')).toHaveCount(2);
    await expect(page.locator('.feed-card', { hasText: 'closed for Diwali' })).toBeVisible();
    await expect(page.locator('.feed-card', { hasText: 'algebra' })).toHaveCount(0);
  });

  test('onboarding message is hidden from feed, classifications and calendar', async ({ page }) => {
    const onboardingText = 'Welcome! What is the name of the school you represent?';

    await goto(page, '/feed');
    await injectCursor(page);
    await expect(page.locator('.feed-card', { hasText: onboardingText })).toHaveCount(0);

    await goto(page, '/classifications');
    await expect(page.locator('tbody tr', { hasText: onboardingText })).toHaveCount(0);
    await expect(page.locator('body')).not.toContainText(onboardingText);

    await goto(page, '/calendar');
    await expect(page.locator('body')).not.toContainText(onboardingText);

    await goto(page, '/api/classifications');
    await expect(page.locator('body')).not.toContainText(onboardingText);
  });

});
