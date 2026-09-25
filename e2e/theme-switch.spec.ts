import { test, expect } from '@playwright/test';
import { click, goto, injectCursor, pause } from './helpers/pacing';

test('background theme switcher toggles static and animated themes', async ({ page }) => {
  const errors: string[] = [];
  page.on('pageerror', (e) => errors.push(String(e)));
  page.on('console', (m) => { if (m.type() === 'error') errors.push(m.text()); });

  await injectCursor(page);
  await goto(page, '/feed', 700);

  await expect(page.locator('.theme-btn')).toHaveCount(2);
  await expect(page.locator('.theme-btn[data-theme="static"]')).toHaveClass(/active/);

  // default is the static SVG background
  await expect(page.locator('body')).not.toHaveClass(/theme-animated/);
  await expect(page.locator('#three-bg')).toBeHidden();

  // switch to the animated Three.js theme
  await click(page, page.locator('.theme-btn[data-theme="animated"]'), 500);
  await expect(page.locator('body')).toHaveClass(/theme-animated/);
  await expect(page.locator('#three-bg')).toBeVisible();

  // give Three.js time to initialise a sized WebGL canvas
  await pause(page, 4000);
  const info = await page.evaluate(() => {
    const c = document.getElementById('three-bg') as HTMLCanvasElement;
    const gl = c.getContext('webgl2') || c.getContext('webgl');
    const vw = window.innerWidth;
    const vh = window.innerHeight;
    return {
      w: c.width,
      h: c.height,
      hasGl: !!gl,
      coversViewport: c.clientWidth === vw && c.clientHeight === vh,
      viewport: { vw, vh },
      theme: (window as any).__themeAnimated ?? null
    };
  });
  expect(info.w).toBeGreaterThan(0);
  expect(info.h).toBeGreaterThan(0);
  expect(info.hasGl).toBeTruthy();
  // fix: the canvas must span the full viewport and tiles must cover it all
  expect(info.coversViewport).toBeTruthy();
  expect(info.viewport.vw).toBeGreaterThan(0);
  expect(info.viewport.vh).toBeGreaterThan(0);
  expect(info.theme).not.toBeNull();
  expect(info.theme.tiles).toBeGreaterThan(0);
  expect(info.theme.meshes).toBeGreaterThan(0);

  // fix: glow layer is alive, not zeroed (opacity oscillates well above 0)
  const glowStats = await page.evaluate(() => {
    const w = (window as any).__themeAnimated;
    return w && w._lastGlow != null ? w._lastGlow : -1;
  });
  expect(glowStats).toBeGreaterThan(0.05);

  // preference persists across navigation
  await goto(page, '/calendar', 800);
  await expect(page.locator('body')).toHaveClass(/theme-animated/);

  // switch back to static
  await click(page, page.locator('.theme-btn[data-theme="static"]'), 500);
  await expect(page.locator('body')).not.toHaveClass(/theme-animated/);

  expect(errors).toEqual([]);
});