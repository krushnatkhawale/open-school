import { chromium } from '@playwright/test';
const browser = await chromium.launch();
const page = await browser.newPage({ viewport: { width: 1440, height: 900 } });
await page.goto('http://localhost:8080/extraction-rules');
await page.waitForSelector('[data-er-modal-open]');
await page.addStyleTag({ content: `
  #probeCursor { position: fixed; top:0; left:0; width:30px; height:30px; margin:-15px 0 0 -15px;
    border-radius:50%; border:2px solid rgba(124,58,237,0.9); background: rgba(255,255,255,0.92);
    pointer-events:none; z-index:999999; }
  #probeCursor::after { content:''; position:absolute; top:50%; left:50%; width:6px; height:6px;
    margin:-3px 0 0 -3px; border-radius:50%; background:#7c3aed; }` });
await page.evaluate(() => {
  const el = document.createElement('div'); el.id = 'probeCursor'; document.body.appendChild(el);
  document.addEventListener('mousemove', (e) => el.style.transform = `translate(${e.clientX}px, ${e.clientY}px)`);
});
const b = await page.locator('[data-er-modal-open]').evaluate(el => { const r = el.getBoundingClientRect(); return { x: r.x+r.width/2, y: r.y+r.height/2 }; });
console.log('button center', b);
await page.mouse.move(b.x, b.y, { steps: 20 });
await page.waitForTimeout(400);
const state = await page.evaluate(() => {
  const el = document.getElementById('probeCursor');
  return { exists: !!el, transform: el.style.transform, rect: (() => { const r = el.getBoundingClientRect(); return {x:r.x,y:r.y,w:r.width,h:r.height}; })() };
});
console.log('cursor state', JSON.stringify(state));
await page.screenshot({ path: '.scratch/er-demo-video/frames/probe.png' });
await browser.close();
