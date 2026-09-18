// Minimal WebSocket driver for FTCSim used by the integration tests (Node >= 22).
// usage: node tools/wsclient.mjs <port> <opmode name> [seconds] [gamepad json]
const port = process.argv[2] || '8000';
const opName = process.argv[3] || 'BlueTeleOp';
const seconds = parseFloat(process.argv[4] || '4');
const pad = process.argv[5] ? JSON.parse(process.argv[5]) : { lx: 0, ly: -1, rx: 0, ry: 0 };
const ws = new WebSocket(`ws://localhost:${port}/ws`);
let last = null, opmodes = null, lastTelemetry = [];
const log = [];
ws.onmessage = (ev) => {
  const m = JSON.parse(ev.data);
  if (m.type === 'state') { last = m; if (m.telemetry.lines.length) lastTelemetry = m.telemetry.lines; }
  else if (m.type === 'opmodes') opmodes = m;
  else if (m.type === 'log') for (const e of m.entries) log.push(e);
  else if (m.type === 'toast') console.log('TOAST', m.level, m.text);
};
const sleep = (ms) => new Promise(r => setTimeout(r, ms));
const send = (o) => ws.send(JSON.stringify(o));
const pose = () => last ? `x=${last.robot.x.toFixed(1)} y=${last.robot.y.toFixed(1)} h=${last.robot.headingDeg.toFixed(1)} v=(${last.robot.vx.toFixed(1)},${last.robot.vy.toFixed(1)})` : 'n/a';
await new Promise((res, rej) => { ws.onopen = res; ws.onerror = rej; });
send({ type: 'hello' });
for (let i = 0; i < 180; i++) { await sleep(500); if (opmodes && opmodes.build && !opmodes.build.building && opmodes.build.success !== undefined && opmodes.list.length) break; if (i % 10 === 9) send({ type: 'hello' }); }
console.log('opmodes:', opmodes ? opmodes.list.length : 'none', 'build:', opmodes && opmodes.build.summary);
const entry = opmodes.list.find(o => o.name === opName);
if (!entry) { console.log('OpMode not found:', opName); process.exit(1); }
console.log('INIT', opName, 'pose', pose());
send({ type: 'opmode.init', name: entry.name, flavor: entry.flavor });
await sleep(1500);
console.log('state after init:', last.opmode.state, 'error:', last.opmode.error);
console.log('telemetry:', lastTelemetry.slice(0, 12));
if (last.opmode.state === 'INIT') {
  send({ type: 'opmode.start' });
  await sleep(300);
  console.log('state after start:', last.opmode.state);
  const t0 = Date.now(); let nextPrint = 0;
  while (Date.now() - t0 < seconds * 1000) {
    send({ type: 'gamepad', id: 1, state: pad });
    await sleep(50);
    if (Date.now() - t0 > nextPrint) { nextPrint += 2000; console.log(`  t=${((Date.now() - t0) / 1000).toFixed(1)}s ${pose()} state=${last.opmode.state} tel=${JSON.stringify(lastTelemetry.slice(0, 3))}`);
      const dm = last.devices.filter(d => d.type === 'DcMotorEx' && d.values.drive).map(d => `${d.name}=${d.values.power.toFixed(2)}`); console.log('    drive powers:', dm.join(' '), 'assignment:', JSON.stringify(last.drivetrain)); }
    if (last.opmode.state !== 'RUNNING') break;
  }
  console.log('after driving:', pose());
  send({ type: 'gamepad', id: 1, state: { lx: 0, ly: 0, rx: 0, ry: 0 } });
  await sleep(1000);
  console.log('after release:', pose());
  console.log('telemetry:', lastTelemetry.slice(0, 25));
  const devs = last.devices.filter(d => ['DcMotorEx', 'GoBildaPinpointDriver', 'Limelight3A', 'LynxModule'].includes(d.type)).map(d => `${d.name}: ${JSON.stringify(d.values)}`);
  console.log('devices:\n  ' + devs.join('\n  '));
  send({ type: 'opmode.stop' });
  await sleep(1500);
  console.log('state after stop:', last.opmode.state, 'error:', last.opmode.error);
}
console.log('--- log (warn+error) ---');
for (const e of log) if (e.level === 'WARN' || e.level === 'ERROR') console.log(e.level[0], e.tag, e.message.split('\n').slice(0, 6).join('\n   '));
ws.close();
process.exit(0);
