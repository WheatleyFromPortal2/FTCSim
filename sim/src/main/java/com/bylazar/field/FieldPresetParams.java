package com.bylazar.field;

public final class FieldPresetParams {
    private String name; private double offsetX, offsetY; private CanvasRotation rotation; private boolean flipX, flipY, reverseXY;
    public FieldPresetParams() { this("", 0, 0, CanvasRotation.DEG_0, false, false, false); }
    public FieldPresetParams(String name, double offsetX, double offsetY, CanvasRotation rotation, boolean flipX, boolean flipY, boolean reverseXY) {
        this.name = name; this.offsetX = offsetX; this.offsetY = offsetY; this.rotation = rotation; this.flipX = flipX; this.flipY = flipY; this.reverseXY = reverseXY;
    }
    public String getName() { return name; } public void setName(String v) { name = v; }
    public double getOffsetX() { return offsetX; } public void setOffsetX(double v) { offsetX = v; }
    public double getOffsetY() { return offsetY; } public void setOffsetY(double v) { offsetY = v; }
    public CanvasRotation getRotation() { return rotation; } public void setRotation(CanvasRotation v) { rotation = v; }
    public boolean getFlipX() { return flipX; } public void setFlipX(boolean v) { flipX = v; }
    public boolean getFlipY() { return flipY; } public void setFlipY(boolean v) { flipY = v; }
    public boolean getReverseXY() { return reverseXY; } public void setReverseXY(boolean v) { reverseXY = v; }
}
