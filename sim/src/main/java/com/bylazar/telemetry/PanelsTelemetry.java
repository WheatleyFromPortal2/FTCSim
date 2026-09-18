package com.bylazar.telemetry;

/** FTCSim stand-in for Panels' PanelsTelemetry object (Kotlin object -> INSTANCE). */
public final class PanelsTelemetry {
    public static final PanelsTelemetry INSTANCE = new PanelsTelemetry();
    private static final TelemetryManager manager = new TelemetryManager();
    private PanelsTelemetry() {}
    public TelemetryManager getTelemetry() { return manager; }
    public TelemetryManager.TelemetryWrapper getFtcTelemetry() { return manager.getWrapper(); }
    /** Kotlin property accessors for team code written in Kotlin. */
    public static TelemetryManager getTelemetryStatic() { return manager; }
}
