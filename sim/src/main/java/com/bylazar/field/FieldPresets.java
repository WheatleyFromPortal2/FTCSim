package com.bylazar.field;

import java.util.Arrays;
import java.util.List;

public final class FieldPresets {
    public static final FieldPresets INSTANCE = new FieldPresets();
    private FieldPresets() {}
    public static final FieldPresetParams PANELS = new FieldPresetParams("Panels", 0, 0, CanvasRotation.DEG_0, false, false, true);
    public static final FieldPresetParams DEFAULT_FTC = new FieldPresetParams("Default FTC", 0, 0, CanvasRotation.DEG_0, false, false, true);
    public static final FieldPresetParams PEDRO_PATHING = new FieldPresetParams("Pedro Pathing", 24.0 * -3, 24.0 * -3, CanvasRotation.DEG_90, false, true, false);
    public static final FieldPresetParams ROAD_RUNNER = new FieldPresetParams("Road Runner", 0, 0, CanvasRotation.DEG_0, false, false, true);
    public FieldPresetParams getPANELS() { return PANELS; }
    public FieldPresetParams getDEFAULT_FTC() { return DEFAULT_FTC; }
    public FieldPresetParams getPEDRO_PATHING() { return PEDRO_PATHING; }
    public FieldPresetParams getROAD_RUNNER() { return ROAD_RUNNER; }
    public List<FieldPresetParams> getAllPresets() { return Arrays.asList(PANELS, DEFAULT_FTC, PEDRO_PATHING, ROAD_RUNNER); }
}
