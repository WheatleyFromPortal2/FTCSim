package com.bylazar.field;

public final class Line extends Drawable {
    private final double x1, y1, x2, y2; private final Style style;
    public Line(double x1, double y1, double x2, double y2, Style style) { super(DrawablesTypes.LINE); this.x1 = x1; this.y1 = y1; this.x2 = x2; this.y2 = y2; this.style = style; }
    public double getX1() { return x1; } public double getY1() { return y1; } public double getX2() { return x2; } public double getY2() { return y2; } public Style getStyle() { return style; }
}
