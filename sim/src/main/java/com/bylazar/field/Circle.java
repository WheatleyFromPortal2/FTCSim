package com.bylazar.field;

public final class Circle extends Drawable {
    private final double x, y, r; private final Style style;
    public Circle(double x, double y, double r, Style style) { super(DrawablesTypes.CIRCLE); this.x = x; this.y = y; this.r = r; this.style = style; }
    public double getX() { return x; } public double getY() { return y; } public double getR() { return r; } public Style getStyle() { return style; }
}
