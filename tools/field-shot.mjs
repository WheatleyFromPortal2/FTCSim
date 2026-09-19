// Screenshots the field view of a running simulator and prints what the field contains.
// usage: node tools/field-shot.mjs <port> <out.png>
import { chromium } from 'playwright';
const port = process.argv[2] || '8000', out = process.argv[3] || 'build/field.png';

const ws = new WebSocket(`ws://localhost:${port}/ws`);
let hello = null;
ws.onmessage = e => { const m = JSON.parse(e.data); if (m.type === 'hello') hello = m; };
await new Promise((res, rej) => { ws.onopen = res; ws.onerror = rej; });
ws.send(JSON.stringify({ type: 'hello' }));
for (let i = 0; i < 40 && !hello; i++) await new Promise(r => setTimeout(r, 250));
const f = hello && hello.field;
console.log('field:', f ? `${f.name} · ${f.tags.length} tags · ${(f.clusters || []).length} clusters · ${(f.zones || []).length} zones · ${(f.shapes || []).length} shapes · options ${JSON.stringify(f.state || {})}` : 'none');
if (f && f.clusters) for (const c of f.clusters) console.log(`  cluster ${c.name} at (${c.x.toFixed(1)}, ${c.y.toFixed(1)}, ${c.z.toFixed(1)}) yaw ${c.yawDeg.toFixed(0)}° pitch ${c.pitchDeg.toFixed(0)}° tags ${c.ids.join(',')}`);
ws.close();

const browser = await chromium.launch();
const page = await browser.newPage({ viewport: { width: 1500, height: 950 } });
const errors = []; page.on('pageerror', e => errors.push(String(e)));
await page.goto(`http://localhost:${port}/`);
await page.waitForTimeout(2500);
const opts = await page.$$eval('#fieldOptions select', els => els.map(e => e.dataset.key + '=' + e.value));
console.log('field option controls:', opts.join(' ') || 'none');
await page.screenshot({ path: out });
console.log('screenshot', out, '· page errors:', errors.length ? errors : 'none');
await browser.close();
process.exit(0);
