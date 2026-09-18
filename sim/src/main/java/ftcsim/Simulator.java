package ftcsim;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.reflect.TypeToken;
import com.qualcomm.robotcore.eventloop.opmode.SimOpModeRunner;
import com.qualcomm.robotcore.util.RobotLog;
import ftcsim.bridge.Configurables;
import ftcsim.bridge.DashboardBridge;
import ftcsim.config.RobotConfig;
import ftcsim.hardware.SimHardware;
import ftcsim.log.SimLog;
import ftcsim.log.StdoutCapture;
import ftcsim.physics.*;
import ftcsim.runner.OpModeEntry;
import ftcsim.runner.SimGamepad;
import ftcsim.teamcode.TeamCode;
import ftcsim.teamcode.TeamCompiler;
import ftcsim.teamcode.TeamRepo;
import ftcsim.web.WebServer;

import java.io.File;
import java.io.IOException;
import java.util.*;

/** Wires the world, hardware, OpMode runner, team code and web UI together. */
public final class Simulator {
    public static final String TAG = "FTCSim";

    public static final class Options {
        public File repo;
        public int port = 8000;
        public File config;
        public boolean includeSamples;
        public boolean prebuilt;
        public boolean openBrowser = true;
        public File home = new File(System.getProperty("ftcsim.home", "."));
        public File dataDir;
        public File webOverrideDir;
    }

    private final Options opts;
    private final Gson gson = new GsonBuilder().disableHtmlEscaping().serializeSpecialFloatingPointValues().create();
    private RobotConfig config;
    private File configFile;
    private World world;
    private SimHardware hardware;
    private SimOpModeRunner runner;
    private TeamCode teamCode;
    private WebServer web;
    private final Configurables configurables = new Configurables();
    private volatile int obeliskTag = 21;
    private volatile boolean matchTimerEnabled;
    private volatile long matchDeadline;
    private volatile int poseEpoch;
    private volatile long lastLogSeq;
    private volatile List<OpModeEntry> opModes = new ArrayList<>();
    private volatile long startedAt;

    public Simulator(Options opts) { this.opts = opts; }

    public World world() { return world; }
    public SimHardware hardware() { return hardware; }
    public SimOpModeRunner runner() { return runner; }
    public TeamCode teamCode() { return teamCode; }
    public RobotConfig config() { return config; }
    public WebServer web() { return web; }

    // ------------------------------------------------------------------ startup
    public void start() throws IOException {
        startedAt = System.currentTimeMillis();
        File data = opts.dataDir != null ? opts.dataDir : new File(opts.home, "build/ftcsim-data");
        data.mkdirs();
        System.setProperty("ftcsim.data", data.getAbsolutePath());
        android.content.Context.setFilesDir(new File(data, "files"));
        StdoutCapture.install();
        android.util.Log.setSink((priority, tag, msg, tr) -> SimLog.log(priority <= 2 ? SimLog.Level.VERBOSE : priority == 3 ? SimLog.Level.DEBUG : priority == 4 ? SimLog.Level.INFO : priority == 5 ? SimLog.Level.WARN : SimLog.Level.ERROR, tag, msg, tr));

        TeamRepo repo = opts.repo == null ? null : new TeamRepo(opts.repo);
        if (repo != null) System.setProperty("ftcsim.sdkVersion", repo.sdkVersion);
        config = loadConfig(repo);
        RobotLog.ii(TAG, "Robot configuration: %s (%s)", config.name, configFile);

        world = new World();
        world.field = "empty".equalsIgnoreCase(config.field) ? Field.empty() : Field.decode();
        hardware = new SimHardware(world, config, () -> world.field, () -> obeliskTag);
        runner = new SimOpModeRunner(hardware.hardwareMap, this::beforeInit, this::afterStop);
        hardware.snapAllowed = () -> runner.state() == SimOpModeRunner.State.INIT || runner.state() == SimOpModeRunner.State.IDLE;
        runner.addStateListener(s -> { if (s == SimOpModeRunner.State.RUNNING && matchTimerEnabled) armMatchTimer(); });
        world.start();

        if (repo != null) {
            String classpath = System.getProperty("java.class.path");
            teamCode = new TeamCode(repo, new File(data, "teamcode"), classpath, opts.includeSamples, opts.prebuilt);
            teamCode.addListener(b -> {
                opModes = b.opModes;
                configurables.setClasses(b.allClasses);
                broadcastOpModes();
            });
            Thread builder = new Thread(() -> { teamCode.build(); teamCode.startWatching(); }, "ftcsim-initial-build");
            builder.setDaemon(true);
            builder.start();
        } else {
            RobotLog.ww(TAG, "No team repository configured: start FTCSim with --repo <path to your FtcRobotController project>");
        }

        web = new WebServer(opts.port, "/web", opts.webOverrideDir, this::onMessage);
        web.addApi("/api/", this::api);
        web.start();
        String url = "http://localhost:" + web.port() + "/";
        RobotLog.ii(TAG, "FTCSim UI: %s", url);
        StdoutCapture.originalOut().println("\n  FTCSim is running: open " + url + " in your browser\n");
        Thread bc = new Thread(this::broadcastLoop, "ftcsim-broadcast");
        bc.setDaemon(true);
        bc.start();
        if (opts.openBrowser) openBrowser(url);
    }

    private RobotConfig loadConfig(TeamRepo repo) throws IOException {
        File f = opts.config;
        if (f == null) {
            String name = repo == null ? "default" : repo.dir.getName();
            File configs = new File(opts.home, "configs");
            f = new File(configs, name + ".json");
            // prefer an existing file that matches the repository name ignoring case (Decode.json for .../decode)
            File[] existing = configs.listFiles();
            if (existing != null) {
                for (File e : existing) {
                    if (e.getName().equalsIgnoreCase(name + ".local.json")) { f = e; break; }
                }
                if (!f.exists()) for (File e : existing) {
                    if (e.getName().equalsIgnoreCase(name + ".json")) { f = e; break; }
                }
            }
        }
        configFile = f;
        if (f.exists()) return RobotConfig.load(f);
        RobotConfig c = RobotConfig.defaults();
        c.name = f.getName().replace(".json", "");
        c.notes = "Auto-generated by FTCSim. Devices requested by the team code are added as they are used; edit this file (or use the Config tab) to describe your robot.";
        try { c.save(f); RobotLog.ii(TAG, "Created robot configuration %s", f); } catch (IOException e) { RobotLog.ww(TAG, "Could not save configuration: %s", e); }
        return c;
    }

    private static void openBrowser(String url) {
        try {
            if (java.awt.Desktop.isDesktopSupported() && java.awt.Desktop.getDesktop().isSupported(java.awt.Desktop.Action.BROWSE)) {
                java.awt.Desktop.getDesktop().browse(java.net.URI.create(url));
            }
        } catch (Throwable ignored) {}
    }

    private void beforeInit() {
        hardware.resetForOpMode();
        DashboardBridge.reset();
    }

    private void afterStop() {
        ftcsim.vision.SimVision.closeAll(); // the Robot Controller closes VisionPortals when an OpMode stops
        hardware.stopAllMotors();
    }

    private void armMatchTimer() {
        OpModeEntry e = runner.entry();
        long seconds = e != null && e.flavor == OpModeEntry.Flavor.AUTONOMOUS ? 30 : 120;
        matchDeadline = System.currentTimeMillis() + seconds * 1000;
    }

    // ------------------------------------------------------------------ messages
    @SuppressWarnings("unchecked")
    private void onMessage(WebServer.Client client, String text) {
        Map<String, Object> msg = gson.fromJson(text, new TypeToken<Map<String, Object>>() {}.getType());
        if (msg == null) return;
        String type = String.valueOf(msg.get("type"));
        try {
            switch (type) {
                case "gamepad": {
                    int id = (int) num(msg.get("id"), 1);
                    SimGamepad gp = id == 2 ? runner.gamepad2() : runner.gamepad1();
                    Object state = msg.get("state");
                    if (state instanceof Map) gp.apply((Map<String, Object>) state);
                    break;
                }
                case "opmode.init": {
                    String name = String.valueOf(msg.get("name"));
                    OpModeEntry entry = findOpMode(name, String.valueOf(msg.get("flavor")));
                    if (entry == null) { toast(client, "error", "Unknown OpMode " + name); break; }
                    runner.init(entry);
                    break;
                }
                case "opmode.start": runner.start(); break;
                case "opmode.stop": new Thread(runner::stop, "ftcsim-ui-stop").start(); break;
                case "robot.restart": new Thread(() -> { runner.restartRobot(); hardware.resetForOpMode(); }, "ftcsim-ui-restart").start(); break;
                case "sim.setPose": {
                    double x = num(msg.get("x"), 0), y = num(msg.get("y"), 0), h = num(msg.get("headingDeg"), 90);
                    world.chassis.setPose(new Pose2(Units.inToM(x), Units.inToM(y), Math.toRadians(h)));
                    poseEpoch++;
                    break;
                }
                case "sim.reset": {
                    world.chassis.setPose(new Pose2(Units.inToM(config.startPose.x), Units.inToM(config.startPose.y), Math.toRadians(config.startPose.headingDeg)));
                    poseEpoch++;
                    break;
                }
                case "sim.setStartPose": {
                    config.startPose = new RobotConfig.StartPose(num(msg.get("x"), 0), num(msg.get("y"), 0), num(msg.get("headingDeg"), 90));
                    saveConfigQuiet();
                    break;
                }
                case "sim.pause": world.setPaused(Boolean.TRUE.equals(msg.get("paused"))); break;
                case "sim.obelisk": obeliskTag = (int) num(msg.get("id"), 21); break;
                case "sim.timer": matchTimerEnabled = Boolean.TRUE.equals(msg.get("enabled")); if (matchTimerEnabled && runner.state() == SimOpModeRunner.State.RUNNING) armMatchTimer(); break;
                case "device.set": hardware.applyInput(String.valueOf(msg.get("name")), String.valueOf(msg.get("key")), msg.get("value")); break;
                case "code.reload": if (teamCode != null) new Thread(teamCode::build, "ftcsim-rebuild").start(); break;
                case "configurable.set": {
                    boolean ok = configurables.set(String.valueOf(msg.get("className")), String.valueOf(msg.get("path")), msg.get("value"));
                    if (!ok) toast(client, "error", "Could not set " + msg.get("className") + "." + msg.get("path"));
                    break;
                }
                case "config.get": client.send(gson.toJson(Map.of("type", "config", "json", config.toJson(), "file", configFile.getAbsolutePath()))); break;
                case "config.save": {
                    RobotConfig c = RobotConfig.fromJson(String.valueOf(msg.get("json")));
                    c.save(configFile);
                    toast(client, "info", "Saved " + configFile.getName() + " (restart FTCSim to apply hardware changes)");
                    break;
                }
                case "config.saveCurrent": saveConfigQuiet(); toast(client, "info", "Saved " + configFile.getName()); break;
                case "log.clear": SimLog.clear(); break;
                case "hello": sendHello(client); break;
                default: toast(client, "warn", "Unknown message " + type);
            }
        } catch (RuntimeException | IOException e) {
            RobotLog.ee(TAG, e, "UI message %s failed", type);
            toast(client, "error", e.toString());
        }
    }

    private void saveConfigQuiet() {
        try { config.save(configFile); } catch (IOException e) { RobotLog.ww(TAG, "save failed: %s", e); }
    }

    private OpModeEntry findOpMode(String name, String flavor) {
        for (OpModeEntry e : opModes) if (e.name.equals(name) && (flavor == null || "null".equals(flavor) || e.flavor.name().equals(flavor))) return e;
        for (OpModeEntry e : opModes) if (e.name.equals(name)) return e;
        return null;
    }

    private static double num(Object o, double def) { return o instanceof Number ? ((Number) o).doubleValue() : def; }

    private void toast(WebServer.Client c, String level, String text) { c.send(gson.toJson(Map.of("type", "toast", "level", level, "text", text))); }

    private WebServer.Response api(String method, String path, Map<String, String> query, byte[] body) {
        if (path.equals("/api/state")) return WebServer.Response.json(gson.toJson(stateMessage()));
        if (path.equals("/api/opmodes")) return WebServer.Response.json(gson.toJson(opModesMessage()));
        if (path.equals("/api/config")) return WebServer.Response.json(config.toJson());
        if (path.equals("/api/log")) return WebServer.Response.json(gson.toJson(SimLog.since(0, 4000)));
        return null;
    }

    // ------------------------------------------------------------------ broadcasting
    private void sendHello(WebServer.Client client) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("type", "hello");
        m.put("repo", opts.repo == null ? null : opts.repo.getAbsolutePath());
        m.put("repoName", opts.repo == null ? null : opts.repo.getName());
        m.put("sdkVersion", System.getProperty("ftcsim.sdkVersion", "11.0.0"));
        m.put("configName", config.name);
        m.put("configFile", configFile.getAbsolutePath());
        m.put("field", fieldMessage());
        m.put("robot", Map.of("lengthIn", config.lengthIn, "widthIn", config.widthIn, "drivetrain", hardware.drivetrainAssignment()));
        m.put("startPose", config.startPose);
        client.send(gson.toJson(m));
        client.send(gson.toJson(opModesMessage()));
        lastLogSeq = 0;
    }

    private Map<String, Object> fieldMessage() {
        Map<String, Object> f = new LinkedHashMap<>();
        f.put("name", world.field.name);
        f.put("sizeIn", Field.HALF_SIZE_IN * 2);
        List<Map<String, Object>> tags = new ArrayList<>();
        for (Field.AprilTag t : world.field.tags) tags.add(Map.of("id", t.id, "name", t.name, "x", t.x, "y", t.y, "z", t.z, "yawDeg", t.yawDeg, "size", t.sizeIn));
        f.put("tags", tags);
        List<Map<String, Object>> obs = new ArrayList<>();
        for (Field.Obstacle o : world.field.obstacles) obs.add(Map.of("name", o.name, "minX", o.minX, "minY", o.minY, "maxX", o.maxX, "maxY", o.maxY, "color", o.color));
        f.put("obstacles", obs);
        return f;
    }

    private Map<String, Object> opModesMessage() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("type", "opmodes");
        List<Map<String, Object>> list = new ArrayList<>();
        for (OpModeEntry e : opModes) {
            Map<String, Object> o = new LinkedHashMap<>();
            o.put("name", e.name); o.put("group", e.group); o.put("flavor", e.flavor.name()); o.put("disabled", e.disabled);
            o.put("className", e.clazz.getName()); o.put("linear", e.linear); o.put("preselectTeleOp", e.preselectTeleOp);
            list.add(o);
        }
        m.put("list", list);
        TeamCode.Build b = teamCode == null ? null : teamCode.current();
        Map<String, Object> build = new LinkedHashMap<>();
        build.put("building", teamCode != null && teamCode.isBuilding());
        build.put("sourcesChanged", teamCode != null && teamCode.sourcesChanged());
        if (b != null) {
            TeamCompiler.Result r = b.compileResult;
            build.put("success", r.success); build.put("summary", r.summary); build.put("builtAt", b.builtAt);
            List<Map<String, Object>> diags = new ArrayList<>();
            for (TeamCompiler.Diagnostic d : r.diagnostics) diags.add(Map.of("kind", d.kind, "file", d.file, "line", d.line, "message", d.message));
            build.put("diagnostics", diags);
            build.put("loadErrors", b.loadErrors);
        } else if (teamCode == null) {
            build.put("success", false); build.put("summary", "No team repository configured (use --repo)");
        }
        m.put("build", build);
        if (teamCode != null) {
            TeamRepo r = teamCode.repo();
            List<String> roots = new ArrayList<>();
            for (File f : r.sourceRoots) roots.add(f.getAbsolutePath());
            m.put("sourceRoots", roots);
            m.put("hasKotlin", r.hasKotlin);
        }
        return m;
    }

    private void broadcastOpModes() { if (web != null) web.broadcast(gson.toJson(opModesMessage())); }

    private Map<String, Object> stateMessage() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("type", "state");
        m.put("t", System.currentTimeMillis());
        Chassis c = world.chassis;
        Pose2 p = c.pose();
        Vec2 v = c.velocityRobot();
        Map<String, Object> robot = new LinkedHashMap<>();
        robot.put("x", Units.mToIn(p.x)); robot.put("y", Units.mToIn(p.y)); robot.put("headingDeg", Math.toDegrees(p.heading));
        robot.put("vx", Units.mToIn(v.x)); robot.put("vy", Units.mToIn(v.y)); robot.put("omegaDeg", Math.toDegrees(c.omega()));
        robot.put("epoch", poseEpoch);
        List<Double> wheels = new ArrayList<>();
        for (MotorState ms : c.driveMotors()) wheels.add(ms.duty() * ms.physicalSign);
        robot.put("wheels", wheels);
        m.put("robot", robot);
        SimOpModeRunner.Snapshot s = runner.snapshot();
        Map<String, Object> op = new LinkedHashMap<>();
        op.put("state", s.state.name()); op.put("name", s.opModeName); op.put("flavor", s.flavor); op.put("runtime", s.runtime);
        op.put("error", s.error); op.put("warning", s.warning); op.put("globalError", s.globalError);
        op.put("timerEnabled", matchTimerEnabled);
        op.put("timerRemaining", s.state == SimOpModeRunner.State.RUNNING && matchTimerEnabled ? Math.max(0, (matchDeadline - System.currentTimeMillis()) / 1000.0) : null);
        m.put("opmode", op);
        m.put("telemetry", Map.of("lines", s.telemetry, "time", s.telemetryTime));
        m.put("panels", Map.of("lines", DashboardBridge.panelsTelemetry(), "time", DashboardBridge.panelsTelemetryTime()));
        m.put("dashboard", Map.of("lines", DashboardBridge.dashboardTelemetry(), "time", DashboardBridge.dashboardTelemetryTime()));
        List<Map<String, Object>> drawings = new ArrayList<>();
        DashboardBridge.Drawing pd = DashboardBridge.panelsDrawing();
        if (pd != null && System.currentTimeMillis() - pd.time < 3000) drawings.add(Map.of("source", pd.source, "frame", pd.frame, "ops", pd.ops));
        DashboardBridge.Drawing dd = DashboardBridge.dashboardDrawing();
        if (dd != null && System.currentTimeMillis() - dd.time < 3000) drawings.add(Map.of("source", dd.source, "frame", dd.frame, "ops", dd.ops));
        m.put("drawings", drawings);
        m.put("devices", hardware.deviceViews());
        m.put("battery", Map.of("volts", world.battery.volts(), "current", world.battery.current()));
        m.put("sim", Map.of("paused", world.isPaused(), "stepRate", world.stepRate(), "simTime", world.simTime(), "obelisk", obeliskTag, "uptime", (System.currentTimeMillis() - startedAt) / 1000));
        m.put("drivetrain", hardware.drivetrainAssignment());
        m.put("codeFrame", hardware.codeFrame());
        return m;
    }

    private void broadcastLoop() {
        long next = System.nanoTime();
        int tick = 0;
        while (true) {
            try {
                if (!web.clients().isEmpty()) {
                    web.broadcast(gson.toJson(stateMessage()));
                    List<SimLog.Entry> entries = SimLog.since(lastLogSeq, 200);
                    if (!entries.isEmpty()) {
                        lastLogSeq = entries.get(entries.size() - 1).seq;
                        web.broadcast(gson.toJson(Map.of("type", "log", "entries", entries)));
                    }
                    if (tick % 10 == 0) {
                        web.broadcast(gson.toJson(Map.of("type", "configurables", "classes", configurables.snapshot())));
                        web.broadcast(gson.toJson(Map.of("type", "graphs", "series", DashboardBridge.graphs())));
                        Map<String, Object> om = opModesMessage();
                        om.put("listOnlyIfChanged", true);
                        web.broadcast(gson.toJson(Map.of("type", "buildStatus", "build", om.get("build"))));
                    }
                }
                if (matchTimerEnabled && runner.state() == SimOpModeRunner.State.RUNNING && System.currentTimeMillis() > matchDeadline) {
                    RobotLog.ii(TAG, "Match timer expired; stopping OpMode");
                    new Thread(runner::stop, "ftcsim-timer-stop").start();
                }
                tick++;
            } catch (Throwable e) {
                RobotLog.ee(TAG, e, "broadcast failed");
            }
            next += 50_000_000L;
            long sleep = next - System.nanoTime();
            if (sleep > 0) { try { Thread.sleep(sleep / 1_000_000L); } catch (InterruptedException e) { return; } }
            else next = System.nanoTime();
        }
    }

    public String toJson(Object o) { return gson.toJson(o); }
    public JsonElement toJsonTree(Object o) { return gson.toJsonTree(o); }
}
