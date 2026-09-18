package com.bylazar.field;

public final class FieldImage {
    private final ImagePreset LIGHT, DARK;
    public FieldImage(ImagePreset LIGHT, ImagePreset DARK) { this.LIGHT = LIGHT; this.DARK = DARK; }
    public ImagePreset getLIGHT() { return LIGHT; }
    public ImagePreset getDARK() { return DARK; }
}
