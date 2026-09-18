package com.bylazar.telemetry;

import ftcsim.bridge.DashboardBridge;
import org.firstinspires.ftc.robotcore.external.Func;
import org.firstinspires.ftc.robotcore.external.Telemetry;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** FTCSim stand-in for Panels' TelemetryManager: lines are shown in the simulator's Panels telemetry view. */
public class TelemetryManager {
    private List<String> lines = new ArrayList<>();
    private List<String> lastLines = new ArrayList<>();
    private long updateInterval = 50;
    private long lastUpdate = 0;
    private final TelemetryWrapper wrapper = new TelemetryWrapper();

    public List<String> getLines() { return lines; }
    public void setLines(List<String> l) { lines = l; }
    public long getUpdateInterval() { return updateInterval; }
    public void setUpdateInterval(long ms) { updateInterval = ms; }
    public long getLastUpdate() { return lastUpdate; }
    public long getTimeSinceLastUpdate() { return System.currentTimeMillis() - lastUpdate; }
    public boolean getShouldUpdateLines() { return getTimeSinceLastUpdate() >= updateInterval; }
    public TelemetryWrapper getWrapper() { return wrapper; }

    public synchronized void addData(String key, Object value) { lines.add(key + ": " + value); }
    public synchronized void addData(String key, String value) { lines.add(key + ": " + value); }
    public synchronized void addLine(String line) { lines.add(line); }
    public synchronized void debug(String... data) { for (String s : data) lines.add(s); }
    public synchronized void debug(Object... data) { for (Object o : data) lines.add(String.valueOf(o)); }

    public synchronized void update() {
        if (lines.isEmpty()) return;
        if (getShouldUpdateLines()) {
            DashboardBridge.panelsTelemetry(lines);
            lastLines = lines;
            lastUpdate = System.currentTimeMillis();
        }
        lines = new ArrayList<>();
    }

    public synchronized void update(Telemetry telemetry) {
        for (String l : lines) telemetry.addLine(l);
        telemetry.update();
        update();
    }

    /** Telemetry-interface adapter (Panels' "ftcTelemetry"). */
    public class TelemetryWrapper implements Telemetry {
        private String itemSeparator = ", ";
        private String captionValueSeparator = ": ";
        @Override public Item addData(String caption, String format, Object... args) {
            String formatted;
            try { formatted = String.format(Locale.getDefault(), format == null ? "%s" : format, args); } catch (RuntimeException e) { formatted = format + " " + java.util.Arrays.toString(args); }
            TelemetryManager.this.addData(caption == null ? "" : caption, formatted);
            return null;
        }
        @Override public Item addData(String caption, Object value) { TelemetryManager.this.addData(caption == null ? "" : caption, String.valueOf(value)); return null; }
        @Override public <T> Item addData(String caption, Func<T> valueProducer) { TelemetryManager.this.addData(caption == null ? "" : caption, String.valueOf(valueProducer == null ? null : valueProducer.value())); return null; }
        @Override public <T> Item addData(String caption, String format, Func<T> valueProducer) {
            Object v = valueProducer == null ? null : valueProducer.value();
            String formatted;
            try { formatted = String.format(Locale.getDefault(), format == null ? "%s" : format, v); } catch (RuntimeException e) { formatted = format + " " + v; }
            TelemetryManager.this.addData(caption == null ? "" : caption, formatted);
            return null;
        }
        @Override public boolean removeItem(Item item) { return false; }
        @Override public void clear() { synchronized (TelemetryManager.this) { lines.clear(); } }
        @Override public void clearAll() { clear(); }
        @Override public Object addAction(Runnable action) { return null; }
        @Override public boolean removeAction(Object token) { return false; }
        @Override public void speak(String text) {}
        @Override public void speak(String text, String languageCode, String countryCode) {}
        @Override public boolean update() { TelemetryManager.this.update(); return true; }
        @Override public Line addLine() { TelemetryManager.this.addLine(""); return null; }
        @Override public Line addLine(String lineCaption) { TelemetryManager.this.addLine(lineCaption == null ? "" : lineCaption); return null; }
        @Override public boolean removeLine(Line line) { return false; }
        @Override public boolean isAutoClear() { return true; }
        @Override public void setAutoClear(boolean autoClear) {}
        @Override public int getMsTransmissionInterval() { return (int) updateInterval; }
        @Override public void setMsTransmissionInterval(int ms) { setUpdateInterval(ms); }
        @Override public String getItemSeparator() { return itemSeparator; }
        @Override public void setItemSeparator(String s) { itemSeparator = s == null ? ", " : s; }
        @Override public String getCaptionValueSeparator() { return captionValueSeparator; }
        @Override public void setCaptionValueSeparator(String s) { captionValueSeparator = s == null ? ": " : s; }
        @Override public void setDisplayFormat(DisplayFormat displayFormat) {}
        @Override public Log log() { return null; }
    }
}
