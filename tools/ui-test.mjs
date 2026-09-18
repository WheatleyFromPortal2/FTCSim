// Playwright smoke test of the FTCSim browser UI.
// usage: NODE_PATH=/opt/node22/lib/node_modules node tools/ui-test.mjs <port> <opmode> <screenshotDir>
import { chromium } from 'playwright';
const port = process.argv[2] || '8000';
const opName = process.argv[3] || 'BlueTeleOp';
const shots = process.argv[4] || 'build/ui-shots';
import fs from 'fs';
fs.mkdirSync(shots, { recursive: true });
const browser = await chromium.launch({ executablePath: process.env.CHROMIUM || undefined });
const page = await browser.newPage({ viewport: { width: 1600, height: 1000 } });
const errors = [];
page.on('pageerror', (e) => errors.push('pageerror: ' + e.message));
page.on('console', (m) => { if (m.type() === 'error') errors.push('console: ' + m.text()); });
await page.goto(`http://localhost:${port}/`);
await page.waitForFunction(() => document.getElementById('connPill').textContent === 'connected', null, { timeout: 15000 });
await page.waitForFunction(() => document.querySelectorAll('#opmodeSelect option').length > 1, null, { timeout: 120000 });
await page.waitForTimeout(500);
await page.screenshot({ path: `${shots}/01-idle.png` });
const names = await page.$$eval('#opmodeSelect option', (os) => os.map(o => o.textContent));
console.log('opmodes in select:', names.length, names.slice(0, 6).join(', '));
const entry = await page.$$eval('#opmodeSelect option', (os, n) => { const o = os.find(o => o.value.endsWith('|' + n)); return o ? o.value : null; }, opName);
if (!entry) { console.log('OpMode not in select:', opName); process.exit(1); }
await page.selectOption('#opmodeSelect', entry);
await page.click('#initBtn');
await page.waitForFunction(() => document.getElementById('statePill').textContent === 'INIT' || document.getElementById('statePill').textContent === 'ESTOP', null, { timeout: 10000 });
await page.waitForTimeout(800);
console.log('state:', await page.textContent('#statePill'), 'error:', (await page.textContent('#errorBox')).slice(0, 200));
await page.screenshot({ path: `${shots}/02-init.png` });
await page.click('#startBtn');
await page.waitForTimeout(500);
console.log('state:', await page.textContent('#statePill'));
const pose0 = await page.textContent('#poseFtc');
// drive with the keyboard (W = left stick forward) for 2 seconds
await page.click('#field', { position: { x: 5, y: 5 } }); // focus page (not the robot)
await page.keyboard.down('KeyW');
await page.waitForTimeout(2000);
await page.keyboard.up('KeyW');
await page.waitForTimeout(700);
const pose1 = await page.textContent('#poseFtc');
console.log('pose before:', pose0, '\npose after :', pose1);
await page.screenshot({ path: `${shots}/03-running.png` });
console.log('telemetry:', (await page.textContent('#telemetry')).split('\n').slice(0, 6).join(' | '));
// hardware tab
await page.click('#rightTabs button[data-tab="hardware"]');
await page.waitForTimeout(300);
console.log('device cards:', await page.$$eval('.dev', (d) => d.length));
await page.click('#rightTabs button[data-tab="config"]');
await page.waitForTimeout(1200);
console.log('configurable classes:', await page.$$eval('#configurables .cls', (d) => d.length), 'rows:', await page.$$eval('#configurables .row', (d) => d.length));
await page.screenshot({ path: `${shots}/04-configurables.png` });
await page.click('#rightTabs button[data-tab="log"]');
await page.waitForTimeout(300);
console.log('log lines:', (await page.textContent('#log')).split('\n').length);
await page.click('#stopBtn');
await page.waitForFunction(() => ['IDLE', 'ESTOP'].includes(document.getElementById('statePill').textContent), null, { timeout: 10000 });
console.log('state after stop:', await page.textContent('#statePill'));
await page.screenshot({ path: `${shots}/05-stopped.png` });
console.log('page errors:', errors.length ? errors : 'none');
await browser.close();
