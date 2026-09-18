// Places the robot where it can see a DECODE goal tag, runs an AprilTag OpMode and prints its telemetry.
// usage: node tools/apriltag-check.mjs <port> [opmode name] [x] [y] [headingDeg]
const port = process.argv[2] || '8000';
const opName = process.argv[3] || 'Concept: AprilTag';
const x = parseFloat(process.argv[4] || '0'), y = parseFloat(process.argv[5] || '0'), h = parseFloat(process.argv[6] || '136');
const ws = new WebSocket(`ws://localhost:${port}/ws`);
let last = null, opmodes = null;
ws.onmessage = (ev) => { const m = JSON.parse(ev.data); if (m.type === 'state') last = m; else if (m.type === 'opmodes') opmodes = m; };
const sleep = (ms) => new Promise(r => setTimeout(r, ms));
const send = (o) => ws.send(JSON.stringify(o));
await new Promise((res, rej) => { ws.onopen = res; ws.onerror = rej; });
send({ type: 'hello' });
for (let i = 0; i < 120; i++) { await sleep(500); if (opmodes && opmodes.build && !opmodes.build.building && opmodes.list.length) break; }
const entry = opmodes.list.find(o => o.name === opName);
if (!entry) { console.log('OpMode not found:', opName); process.exit(1); }
send({ type: 'sim.reset' }); await sleep(300);
send({ type: 'sim.setPose', x, y, headingDeg: h }); await sleep(300);
send({ type: 'opmode.init', name: entry.name, flavor: entry.flavor }); await sleep(2000);
send({ type: 'opmode.start' }); await sleep(2500);
console.log('pose', last.robot.x.toFixed(1), last.robot.y.toFixed(1), last.robot.headingDeg.toFixed(0), 'state', last.opmode.state, last.opmode.error || '');
for (const l of last.telemetry.lines.slice(0, 14)) console.log('  ' + l);
for (const d of last.devices.filter(d => d.type === 'VisionPortal' || d.type === 'WebcamName')) console.log('  ' + d.name + ': ' + JSON.stringify(d.values));
send({ type: 'opmode.stop' }); await sleep(800);
ws.close(); process.exit(0);
