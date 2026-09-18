package com.acmerobotics.dashboard;

import com.acmerobotics.dashboard.canvas.Canvas;
import com.acmerobotics.dashboard.config.ValueProvider;
import com.acmerobotics.dashboard.telemetry.TelemetryPacket;
import ftcsim.bridge.DashboardBridge;
import org.firstinspires.ftc.robotcore.external.Func;
import org.firstinspires.ftc.robotcore.external.Telemetry;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * FTCSim stand-in for FTC Dashboard's FtcDashboard singleton. Telemetry packets
 * and field overlays are shown in the simulator UI; @Config classes are
 * exposed in its Configurables panel.
 */
public class FtcDashboard {
    private static final FtcDashboard instance = new FtcDashboard();
    private int telemetryTransmissionInterval = 100;
    private final DashboardTelemetry telemetry = new DashboardTelemetry();
    private volatile boolean enabled = true;

    public static FtcDashboard getInstance() { return instance; }
    public static void start(Object context) {}
    public static void stop(Object context) {}
    public static void attachWebServer(Object... args) {}
    public static void suppressOpMode() {}
    public static void populateMenu(Object... args) {}

    public void sendTelemetryPacket(TelemetryPacket packet) {
        if (packet == null) return;
        List<String> lines = new ArrayList<>();
        for (Map.Entry<String, String> e : packet.getData().entrySet()) lines.add(e.getKey() + ": " + e.getValue());
        lines.addAll(packet.getLog());
        DashboardBridge.dashboardTelemetry(lines);
        for (Map.Entry<String, String> e : packet.getData().entrySet()) {
            try { DashboardBridge.graphSample(e.getKey(), Double.parseDouble(e.getValue())); } catch (NumberFormatException ignored) {}
        }
        Canvas c = packet.fieldOverlay();
        if (c != null && !c.getOperations().isEmpty()) DashboardBridge.dashboardDrawing(new ArrayList<>(c.getOperations()));
    }
    public void clearTelemetry() { DashboardBridge.dashboardTelemetry(new ArrayList<>()); }
    public Telemetry getTelemetry() { return telemetry; }
    public int getTelemetryTransmissionInterval() { return telemetryTransmissionInterval; }
    public void setTelemetryTransmissionInterval(int ms) { telemetryTransmissionInterval = ms; }
    public void startCameraStream(Object source, double maxFps) {}
    public void stopCameraStream() {}
    public void updateConfig() {}
    public void withConfigRoot(Object consumer) {}
    public <T> void addConfigVariable(String category, String name, ValueProvider<T> provider) {}
    public <T> void addConfigVariable(String category, String name, ValueProvider<T> provider, boolean autoRemove) {}
    public void removeConfigVariable(String category, String name) {}
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean e) { enabled = e; }
    public void registerOpMode(Object... args) {}
    public void setImageQuality(int quality) {}
    public void sendImage(Object bitmap) {}

    /** Telemetry implementation that batches items into a packet on update(). */
    private class DashboardTelemetry implements Telemetry {
        private TelemetryPacket packet = new TelemetryPacket();
        private final List<Runnable> actions = new ArrayList<>();
        private boolean autoClear = true;
        private String itemSep = " | ", captionSep = " : ";
        private final DashLog log = new DashLog();

        @Override public Item addData(String caption, String format, Object... args) { packet.put(caption, String.format(format, args)); return new DashItem(caption); }
        @Override public Item addData(String caption, Object value) { packet.put(caption, value); return new DashItem(caption); }
        @Override public <T> Item addData(String caption, Func<T> valueProducer) { packet.put(caption, valueProducer.value()); return new DashItem(caption); }
        @Override public <T> Item addData(String caption, String format, Func<T> valueProducer) { packet.put(caption, String.format(format, valueProducer.value())); return new DashItem(caption); }
        @Override public boolean removeItem(Item item) { return false; }
        @Override public void clear() { packet = new TelemetryPacket(); }
        @Override public void clearAll() { clear(); actions.clear(); }
        @Override public Object addAction(Runnable action) { actions.add(action); return action; }
        @Override public boolean removeAction(Object token) { return actions.remove(token); }
        @Override public void speak(String text) {}
        @Override public void speak(String text, String languageCode, String countryCode) {}
        @Override public boolean update() {
            for (Runnable r : actions) r.run();
            for (String l : log.entries) packet.addLine(l);
            sendTelemetryPacket(packet);
            if (autoClear) packet = new TelemetryPacket();
            return true;
        }
        @Override public Line addLine() { return addLine(""); }
        @Override public Line addLine(String lineCaption) { packet.addLine(lineCaption); return new DashLine(); }
        @Override public boolean removeLine(Line line) { return false; }
        @Override public boolean isAutoClear() { return autoClear; }
        @Override public void setAutoClear(boolean a) { autoClear = a; }
        @Override public int getMsTransmissionInterval() { return telemetryTransmissionInterval; }
        @Override public void setMsTransmissionInterval(int ms) { telemetryTransmissionInterval = ms; }
        @Override public String getItemSeparator() { return itemSep; }
        @Override public void setItemSeparator(String s) { itemSep = s; }
        @Override public String getCaptionValueSeparator() { return captionSep; }
        @Override public void setCaptionValueSeparator(String s) { captionSep = s; }
        @Override public void setDisplayFormat(DisplayFormat displayFormat) {}
        @Override public Log log() { return log; }

        private class DashItem implements Item {
            private String caption; DashItem(String c) { caption = c; }
            @Override public String getCaption() { return caption; }
            @Override public Item setCaption(String c) { caption = c; return this; }
            @Override public Item setValue(String format, Object... args) { packet.put(caption, String.format(format, args)); return this; }
            @Override public Item setValue(Object value) { packet.put(caption, value); return this; }
            @Override public <T> Item setValue(Func<T> valueProducer) { packet.put(caption, valueProducer.value()); return this; }
            @Override public <T> Item setValue(String format, Func<T> valueProducer) { packet.put(caption, String.format(format, valueProducer.value())); return this; }
            @Override public Item setRetained(Boolean retained) { return this; }
            @Override public boolean isRetained() { return false; }
            @Override public Item addData(String c, String format, Object... args) { return DashboardTelemetry.this.addData(c, format, args); }
            @Override public Item addData(String c, Object value) { return DashboardTelemetry.this.addData(c, value); }
            @Override public <T> Item addData(String c, Func<T> valueProducer) { return DashboardTelemetry.this.addData(c, valueProducer); }
            @Override public <T> Item addData(String c, String format, Func<T> valueProducer) { return DashboardTelemetry.this.addData(c, format, valueProducer); }
        }
        private class DashLine implements Line {
            @Override public Item addData(String caption, String format, Object... args) { return DashboardTelemetry.this.addData(caption, format, args); }
            @Override public Item addData(String caption, Object value) { return DashboardTelemetry.this.addData(caption, value); }
            @Override public <T> Item addData(String caption, Func<T> valueProducer) { return DashboardTelemetry.this.addData(caption, valueProducer); }
            @Override public <T> Item addData(String caption, String format, Func<T> valueProducer) { return DashboardTelemetry.this.addData(caption, format, valueProducer); }
        }
        private class DashLog implements Log {
            final List<String> entries = new ArrayList<>(); int capacity = 9; DisplayOrder order = DisplayOrder.OLDEST_FIRST;
            @Override public int getCapacity() { return capacity; }
            @Override public void setCapacity(int c) { capacity = c; }
            @Override public DisplayOrder getDisplayOrder() { return order; }
            @Override public void setDisplayOrder(DisplayOrder o) { order = o; }
            @Override public void add(String entry) { entries.add(entry); while (entries.size() > capacity) entries.remove(0); }
            @Override public void add(String format, Object... args) { add(String.format(format, args)); }
            @Override public void clear() { entries.clear(); }
        }
    }
}
