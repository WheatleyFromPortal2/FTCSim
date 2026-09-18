package com.acmerobotics.dashboard.telemetry;

import com.acmerobotics.dashboard.canvas.Canvas;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** FTCSim stand-in for FTC Dashboard's TelemetryPacket. */
public class TelemetryPacket {
    private final Map<String, String> data = new LinkedHashMap<>();
    private final List<String> log = new ArrayList<>();
    private final Canvas fieldOverlay = new Canvas();
    private long timestamp;
    public TelemetryPacket() { this(true); }
    public TelemetryPacket(boolean drawDefaultField) {}
    public void put(String key, Object value) { data.put(key, value == null ? "null" : value.toString()); }
    public void putAll(Map<String, Object> values) { for (Map.Entry<String, Object> e : values.entrySet()) put(e.getKey(), e.getValue()); }
    public void addLine(String line) { log.add(line); }
    public void clearLines() { log.clear(); }
    public long addTimestamp() { timestamp = System.currentTimeMillis(); return timestamp; }
    public Canvas fieldOverlay() { return fieldOverlay; }
    public Map<String, String> getData() { return data; }
    public List<String> getLog() { return log; }
    public Canvas field() { return fieldOverlay; }
}
