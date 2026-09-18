package com.bylazar.field;

public final class FieldImages {
    public static final FieldImages INSTANCE = new FieldImages();
    private FieldImages() {}
    public static final FieldImage DECODE = new FieldImage(() -> "decode-light.png", () -> "decode-dark.png");
    public static final FieldImage BIOBUZZ = new FieldImage(() -> "biobuzz-light.png", () -> "biobuzz-dark.png");
    public FieldImage getDECODE() { return DECODE; }
    public FieldImage getBIOBUZZ() { return BIOBUZZ; }
    public byte[] loadResourceAsBytes(String resourceName) { return new byte[0]; }
    public String loadResourceAsBase64(String resourceName) { return resourceName; }
}
