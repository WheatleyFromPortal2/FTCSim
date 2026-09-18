package com.bylazar.field;

public final class Rectangle extends Drawable {
    private final double x, y, w, h; private final Style style;
    public Rectangle(double x, double y, double w, double h, Style style) { super(DrawablesTypes.RECTANGLE); this.x = x; this.y = y; this.w = w; this.h = h; this.style = style; }
    public double getX() { return x; } public double getY() { return y; } public double getW() { return w; } public double getH() { return h; } public Style getStyle() { return style; }
}
