package com.bylazar.field;

/** FTCSim stand-in for Panels' PanelsField object. Drawings appear on the simulator's field view. */
public final class PanelsField {
    public static final PanelsField INSTANCE = new PanelsField();
    private static final FieldManager manager = new FieldManager();
    public static final String TRANSPARENT = "transparent", WHITE = "white", BLACK = "black", RED = "red", BLUE = "blue";
    private PanelsField() {}
    public FieldManager getField() { return manager; }
    public FieldPresets getPresets() { return FieldPresets.INSTANCE; }
    public String getTRANSPARENT() { return TRANSPARENT; }
    public String getWHITE() { return WHITE; }
    public String getBLACK() { return BLACK; }
    public String getRED() { return RED; }
    public String getBLUE() { return BLUE; }
    public FieldImages getImages() { return FieldImages.INSTANCE; }
}
