package com.bylazar.graph;

import ftcsim.bridge.DashboardBridge;

import java.util.LinkedHashMap;
import java.util.Map;

/** FTCSim stand-in for Panels' GraphManager: samples appear in the simulator's graph view. */
public class GraphManager {
    private final Map<String, Double> pending = new LinkedHashMap<>();
    public synchronized void addData(String key, Number value) { pending.put(key, value == null ? 0.0 : value.doubleValue()); }
    public synchronized void addData(String key, double value) { pending.put(key, value); }
    public synchronized void update() { for (Map.Entry<String, Double> e : pending.entrySet()) DashboardBridge.graphSample(e.getKey(), e.getValue()); pending.clear(); }
}
