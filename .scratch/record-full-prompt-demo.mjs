import { chromium } from '@playwright/test';
import fs from 'fs';

const outDir = '/Users/hulk/.buzz/REPOS/my-private-digital-school/.scratch/er-demo-video';
fs.mkdirSync(outDir, { recursive: true });

const browser = await chromium.launch();
const context = await browser.newContext({
  viewport: { width: 1440, height: 900 },
  recordVideo: { dir: outDir, size: { width: 1440, height: 900 } },
});

const page = await context.newPage();
await page.goto('http://localhost:8080/extraction-rules');
await page.waitForSelector('[data-er-modal-open]');
await page.waitForTimeout(1200);

await page.locator('[data-er-modal-open]').click();
await page.waitForSelector('#erPromptModal.open');
await page.waitForTimeout(800);

const field = page.locator('#erModalPrompt');
const headerCount = await field.evaluate((el) => {
  const text = el.value;
  return ['Classification', 'Image description', 'Lesson date', 'Scheduled events', 'Tags'].filter((l) =>
    text.includes(`=== ${l} ===`),
  ).length;
});
const isSingle = await page.locator('#erPromptModal textarea').count();
console.log(`[check] single textarea in modal: ${isSingle === 1}`);
console.log(`[check] field headers present: ${headerCount}/5`);
console.log(`[check] readonly: ${await field.getAttribute('readonly') === ''}`);
console.log(`[check] contract text present: ${(await field.inputValue()).includes('school communication classifier')}`);

await page.evaluate(() => {
  const el = document.getElementById('erModalPrompt');
  el.scrollTop = el.scrollHeight;
});
await page.waitForTimeout(900);

await page.keyboard.press('Escape');
await page.waitForFunction(() => !document.getElementById('erPromptModal').classList.contains('open'));
await page.waitForTimeout(600);

await page.close();
await context.close();
await browser.close();

const video = fs.readdirSync(outDir).find((f) => f.endsWith('.webm'));
console.log(`video: ${outDir}/${video}`);
