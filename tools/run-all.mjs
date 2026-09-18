// Runs every OpMode the simulator found: INIT, START, drive a bit, STOP; reports exceptions and warnings.
// usage: node tools/run-all.mjs <port> [seconds per opmode] [name filter regex]
const port = process.argv[2] || '8000';
const seconds = parseFloat(process.argv[3] || '4');
const filter = process.argv[4] ? new RegExp(process.argv[4]) : null;
const ws = new WebSocket(`ws://localhost:${port}/ws`);
let last = null, opmodes = null, log = [];
ws.onmessage = (ev) => {
  const m = JSON.parse(ev.data);
  if (m.type === 'state') last = m;
  else if (m.type === 'opmodes') opmodes = m;
  else if (m.type === 'log') for (const e of m.entries) log.push(e);
};
const sleep = (ms) => new Promise(r => setTimeout(r, ms));
const send = (o) => ws.send(JSON.stringify(o));
const pose = () => last ? `(${last.robot.x.toFixed(1)}, ${last.robot.y.toFixed(1)}, ${last.robot.headingDeg.toFixed(0)}°)` : 'n/a';
await new Promise((res, rej) => { ws.onopen = res; ws.onerror = rej; });
send({ type: 'hello' });
for (let i = 0; i < 180; i++) { await sleep(500); if (opmodes && opmodes.build && !opmodes.build.building && opmodes.build.success !== undefined && opmodes.list.length) break; if (i % 10 === 9) send({ type: 'hello' }); }
console.log('opmodes:', opmodes.list.length, 'build:', opmodes.build.summary);
const results = [];
for (const entry of opmodes.list) {
  if (filter && !filter.test(entry.name)) continue;
  if (entry.disabled && !process.env.RUN_DISABLED) { console.log(`-- ${entry.name}: disabled, skipped`); continue; }
  log = [];
  const r = { name: entry.name, flavor: entry.flavor, ok: true, notes: [] };
  send({ type: 'sim.reset' });
  await sleep(300);
  send({ type: 'opmode.init', name: entry.name, flavor: entry.flavor });
  const t0 = Date.now();
  while (Date.now() - t0 < 2500) { await sleep(100); if (last.opmode.state === 'INIT' || last.opmode.state === 'ERROR' || last.opmode.state === 'IDLE') break; }
  await sleep(700);
  r.initState = last.opmode.state; r.initPose = pose();
  if (last.opmode.error) { r.ok = false; r.notes.push('init error: ' + last.opmode.error.split('\n')[0]); }
  if (last.opmode.state === 'INIT') {
    const initLines = (last.telemetry.lines || []).join(' ');
    if (/right bumper to select/i.test(initLines)) {
      // Pedro SelectableOpMode: pick the first entry (press and release the right bumper twice for nested menus)
      for (let j = 0; j < 2; j++) {
        send({ type: 'gamepad', id: 1, state: { rb: true } }); await sleep(150);
        send({ type: 'gamepad', id: 1, state: { rb: false } }); await sleep(400);
      }
      r.notes.push('selected: ' + (last.telemetry.lines || []).slice(0, 3).join(' | ').slice(0, 120));
    } else if (/\(A\):/i.test(initLines)) {
      // "press A/B/X to select a mode" style init menus: pick A
      send({ type: 'gamepad', id: 1, state: { a: true } }); await sleep(150);
      send({ type: 'gamepad', id: 1, state: { a: false } }); await sleep(300);
      r.notes.push('pressed A during init');
    }
    send({ type: 'opmode.start' });
    await sleep(300);
    const t1 = Date.now(); let k = 0;
    while (Date.now() - t1 < seconds * 1000) {
      // gentle driving + button presses so both teleop and tuning opmodes exercise something
      const phase = Math.floor((Date.now() - t1) / 1000) % 4;
      const pad = { lx: phase === 1 ? 0.6 : 0, ly: phase === 0 ? -0.6 : 0, rx: phase === 2 ? 0.5 : 0, ry: 0, a: k % 40 === 5, du: k % 40 === 15, dd: k % 40 === 25, rb: k % 40 === 35 };
      send({ type: 'gamepad', id: 1, state: pad });
      await sleep(50); k++;
      if (last.opmode.state !== 'RUNNING') break;
    }
    r.runState = last.opmode.state; r.runPose = pose();
    r.telemetry = (last.telemetry.lines || []).slice(0, 4).map(s => s.replace(/\s+/g, ' ').slice(0, 60));
    r.panels = (last.panels && last.panels.lines || []).slice(0, 4).map(s => s.replace(/\s+/g, ' ').slice(0, 60));
    if (last.opmode.error) { r.ok = false; r.notes.push('run error: ' + last.opmode.error.split('\n')[0]); }
    send({ type: 'gamepad', id: 1, state: { lx: 0, ly: 0, rx: 0, ry: 0 } });
    send({ type: 'opmode.stop' });
    const t2 = Date.now();
    while (Date.now() - t2 < 4000) { await sleep(100); if (last.opmode.state === 'IDLE' || last.opmode.state === 'ERROR') break; }
    r.stopState = last.opmode.state;
    if (last.opmode.state !== 'IDLE') { r.ok = false; r.notes.push('did not stop: ' + last.opmode.state + ' ' + (last.opmode.error || '')); }
  } else { r.ok = false; r.notes.push('init state ' + last.opmode.state); }
  const errs = log.filter(e => e.level === 'ERROR').map(e => `${e.tag}: ${e.message.split('\n')[0].slice(0, 160)}`);
  const warns = log.filter(e => e.level === 'WARN').map(e => `${e.tag}: ${e.message.split('\n')[0].slice(0, 160)}`);
  r.errors = [...new Set(errs)].slice(0, 6); r.warnings = [...new Set(warns)].slice(0, 6);
  results.push(r);
  console.log(`${r.ok ? 'OK ' : 'BAD'} ${entry.flavor.padEnd(10)} ${entry.name.padEnd(28)} init=${r.initState} run=${r.runState || '-'} stop=${r.stopState || '-'} pose ${r.initPose} -> ${r.runPose || '-'}`);
  for (const n of r.notes) console.log('      ! ' + n);
  for (const e of r.errors) console.log('      E ' + e);
  for (const w of r.warnings) console.log('      W ' + w);
  if (r.telemetry && r.telemetry.length) console.log('      telemetry: ' + JSON.stringify(r.telemetry));
  if (r.panels && r.panels.length) console.log('      panels: ' + JSON.stringify(r.panels));
}
console.log(`\n${results.filter(r => r.ok).length}/${results.length} OpModes ran without errors`);
ws.close();
process.exit(0);
