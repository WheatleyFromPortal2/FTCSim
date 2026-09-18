package ftcsim.bridge;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Collects what dashboard libraries (Panels, FTC Dashboard) would send to their
 * own web UIs so the simulator UI can show it: telemetry lines, field drawings
 * and graph samples.
 */
public final class DashboardBridge {
    public static final class Drawing {
        public final String source;          // "panels" | "dashboard"
        public final String frame;           // "ftc" | "pedro" | "panels"
        public final List<Map<String, Object>> ops;
        public final long time;
        public Drawing(String source, String frame, List<Map<String, Object>> ops) { this.source = source; this.frame = frame; this.ops = ops; this.time = System.currentTimeMillis(); }
    }

    private static volatile List<String> panelsTelemetry = new ArrayList<>();
    private static volatile long panelsTelemetryTime;
    private static volatile List<String> dashboardTelemetry = new ArrayList<>();
    private static volatile long dashboardTelemetryTime;
    private static volatile Drawing panelsDrawing;
    private static volatile Drawing dashboardDrawing;
    private static final Map<String, Deque<double[]>> graphs = new LinkedHashMap<>();
    private static final List<Runnable> resetHooks = new CopyOnWriteArrayList<>();

    private DashboardBridge() {}

    public static void panelsTelemetry(List<String> lines) { panelsTelemetry = new ArrayList<>(lines); panelsTelemetryTime = System.currentTimeMillis(); }
    public static void dashboardTelemetry(List<String> lines) { dashboardTelemetry = new ArrayList<>(lines); dashboardTelemetryTime = System.currentTimeMillis(); }
    public static void panelsDrawing(String frame, List<Map<String, Object>> ops) { panelsDrawing = new Drawing("panels", frame, ops); }
    public static void dashboardDrawing(List<Map<String, Object>> ops) { dashboardDrawing = new Drawing("dashboard", "ftc", ops); }
    public static void graphSample(String key, double value) {
        synchronized (graphs) {
            Deque<double[]> q = graphs.computeIfAbsent(key, k -> new ArrayDeque<>());
            q.addLast(new double[] { System.currentTimeMillis(), value });
            while (q.size() > 600) q.pollFirst();
        }
    }

    public static List<String> panelsTelemetry() { return panelsTelemetry; }
    public static long panelsTelemetryTime() { return panelsTelemetryTime; }
    public static List<String> dashboardTelemetry() { return dashboardTelemetry; }
    public static long dashboardTelemetryTime() { return dashboardTelemetryTime; }
    public static Drawing panelsDrawing() { return panelsDrawing; }
    public static Drawing dashboardDrawing() { return dashboardDrawing; }
    public static Map<String, List<double[]>> graphs() {
        Map<String, List<double[]>> out = new LinkedHashMap<>();
        synchronized (graphs) { for (Map.Entry<String, Deque<double[]>> e : graphs.entrySet()) out.put(e.getKey(), new ArrayList<>(e.getValue())); }
        return out;
    }
    public static void clearGraphs() { synchronized (graphs) { graphs.clear(); } }

    /** Called when an OpMode starts so stale drawings disappear. */
    public static void reset() {
        panelsTelemetry = new ArrayList<>(); dashboardTelemetry = new ArrayList<>();
        panelsDrawing = null; dashboardDrawing = null;
        clearGraphs();
        for (Runnable r : resetHooks) r.run();
    }
    public static void addResetHook(Runnable r) { resetHooks.add(r); }
}
