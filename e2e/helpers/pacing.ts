import { expect, type Locator, type Page } from '@playwright/test';

export const CURSOR_CSS = `
  #demoCursor {
    position: fixed; top: 0; left: 0; width: 30px; height: 30px;
    margin: -15px 0 0 -15px; border-radius: 50%;
    border: 2px solid rgba(124, 58, 237, 0.9);
    background: rgba(255, 255, 255, 0.92);
    box-shadow: 0 0 0 5px rgba(124, 58, 237, 0.14), 0 1px 5px rgba(0, 0, 0, 0.28);
    pointer-events: none; z-index: 999999;
    will-change: transform;
  }
  #demoCursor::after {
    content: ''; position: absolute; top: 50%; left: 50%;
    width: 6px; height: 6px; margin: -3px 0 0 -3px; border-radius: 50%;
    background: #7c3aed;
  }
`;

export async function injectCursor(page: Page): Promise<void> {
  await page.addStyleTag({ content: CURSOR_CSS });
  await page.evaluate(() => {
    const el = document.createElement('div');
    el.id = 'demoCursor';
    document.body.appendChild(el);
    document.addEventListener('mousemove', (e) => {
      el.style.transform = `translate(${e.clientX}px, ${e.clientY}px)`;
    });
  });
}

export async function pause(page: Page, ms = 450): Promise<void> {
  await page.waitForTimeout(ms);
}

export async function glide(page: Page, target: Locator, steps = 20, settleMs = 160): Promise<void> {
  await expect(target).toBeVisible();
  const box = await target.boundingBox();
  if (!box) throw new Error(`glide: no bounding box for ${target}`);
  await page.mouse.move(box.x + box.width / 2, box.y + box.height / 2, { steps });
  await pause(page, settleMs);
}

export async function goto(page: Page, path: string, settleMs = 600): Promise<void> {
  await page.goto(path);
  await pause(page, settleMs);
}

export interface ClickOptions {
  position?: { x: number; y: number };
  settleMs?: number;
}

export async function click(page: Page, target: Locator, options: ClickOptions | number = {}): Promise<void> {
  const opts: ClickOptions = typeof options === 'number' ? { settleMs: options } : options;
  const settleMs = opts.settleMs ?? 450;
  if (opts.position) {
    await glide(page, target);
    await target.click({ position: opts.position });
  } else {
    await glide(page, target);
    await target.click();
  }
  await pause(page, settleMs);
}

export async function fill(page: Page, target: Locator, value: string, settleMs = 400): Promise<void> {
  await glide(page, target);
  await target.fill(value);
  await pause(page, settleMs);
}

export async function select(page: Page, target: Locator, value: string, settleMs = 450): Promise<void> {
  await glide(page, target);
  await target.selectOption(value);
  await pause(page, settleMs);
}

export async function press(page: Page, key: string, settleMs = 350): Promise<void> {
  await page.keyboard.press(key);
  await pause(page, settleMs);
}
