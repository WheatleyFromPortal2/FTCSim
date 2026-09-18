package com.bylazar.field;

import java.util.UUID;

public final class Image extends Drawable {
    private final double x, y, w, h; private final UUID id;
    public Image(double x, double y, double w, double h, UUID id) { super(DrawablesTypes.IMAGE); this.x = x; this.y = y; this.w = w; this.h = h; this.id = id; }
    public double getX() { return x; } public double getY() { return y; } public double getW() { return w; } public double getH() { return h; } public UUID getId() { return id; }
}
