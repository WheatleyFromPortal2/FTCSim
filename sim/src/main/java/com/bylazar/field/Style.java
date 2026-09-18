package com.bylazar.field;

import java.util.Objects;

public final class Style {
    private final String fill, outlineFill; private final double outlineWidth;
    public Style(String fill, String outlineFill, double outlineWidth) { this.fill = fill; this.outlineFill = outlineFill; this.outlineWidth = outlineWidth; }
    public String getFill() { return fill; }
    public String getOutlineFill() { return outlineFill; }
    public double getOutlineWidth() { return outlineWidth; }
    public Style copy(String fill, String outlineFill, double outlineWidth) { return new Style(fill, outlineFill, outlineWidth); }
    @Override public boolean equals(Object o) { return o instanceof Style && Objects.equals(((Style) o).fill, fill) && Objects.equals(((Style) o).outlineFill, outlineFill) && ((Style) o).outlineWidth == outlineWidth; }
    @Override public int hashCode() { return Objects.hash(fill, outlineFill, outlineWidth); }
    @Override public String toString() { return "Style(fill=" + fill + ", outlineFill=" + outlineFill + ", outlineWidth=" + outlineWidth + ")"; }
}
